
package components

import params.{CoherenceSpec, MemorySystemParams, SimpleCacheParams}
import chisel3.{Data, _}
import chisel3.util._
import chisel3.experimental._
import coherences.RelativeCriticality
import components.PendingRequestLookupTable.{PRLUTEntry, PRLUTResponseChannel}

object PipelinedMemoryController {
  class BusSideIO(
                   private val memorySystemParams: MemorySystemParams,
                 ) extends Bundle {
    private val masterCount: Int = memorySystemParams.masterCount
    private val genMemReqCommand: MemReqCommand = memorySystemParams.getGenMemReqCommand
    private val genMemRespCommand: MemRespCommand = memorySystemParams.getGenMemRespCommand

    private val busDataWidth: Int = memorySystemParams.busDataWidth
    val request_channel = Flipped(Decoupled(genMemReqCommand))
    // for dedicated writebacks
    // if PutM is requried to broadcasted, this should not be used

    // goining in
    // per core dedicated PutM channel
    val dedicated_request_channel = Flipped(Vec(masterCount, Decoupled(genMemReqCommand)))
    val data_channel = Flipped(Vec(masterCount, Decoupled(UInt(busDataWidth.W))))

    // going out
    val response_channel = Vec(masterCount, Decoupled(genMemRespCommand))
    val response_data_channel = Vec(masterCount, Decoupled(UInt(busDataWidth.W)))

    val slotOwner = Input(UIntHolding(memorySystemParams.masterCount))
    val slotStart = Input(Bool())
  }
  // Front End takes as input the writeback data and requests and manage them such that they are in some buffer
  // And so that they are ready to be output
  class FrontEnd[S <: Data, M <: Data, B <: Data](private val memorySystemParams: MemorySystemParams,
                 private val coherenceSpec: CoherenceSpec[S, M, B]
                ) extends Module {
    val m = memorySystemParams
    private val busDataWidth: Int = m.busDataWidth
    private val masterCount = m.masterCount
    private val genMemRespCommand: MemRespCommand = m.getGenMemRespCommand
    val io = IO(new Bundle {
      val bus = new BusSideIO(m)
      // facing backend of MC
      val response_channel = Flipped(Vec(masterCount, Decoupled(genMemRespCommand)))
      val response_data_channel = Flipped(Vec(masterCount, Decoupled(UInt(busDataWidth.W))))
      val backendWBEntry = Decoupled(new MemoryControllerWBBufferEntry(m))
      val prlutInsert = Decoupled(new PendingRequestLookupTable.PRLUTRequestChannel(m, m.genCrit, coherenceSpec))
      val slotOwner = Output(UIntHolding(memorySystemParams.masterCount))
      val slotStart = Output(Bool())
    })
    // val prlut = Module(new PendingRequestLookupTable(memorySystemParams, coherenceSpec))
    val prlutQ = Module(new Queue(new PendingRequestLookupTable.PRLUTRequestChannel(m, m.genCrit, coherenceSpec),
      if(!memorySystemParams.useAtomicBusModified) { // if not using modified, the PRLUT should be all
        m.masterCount
      } else { // only one entry if using atmoic modified, no request accumulated
        1
      }))
    prlutQ.io.enq.valid := false.B
    prlutQ.io.enq.bits := 0.U.asTypeOf(prlutQ.io.enq.bits)

    io.prlutInsert <> prlutQ.io.deq
    val writebackBuffer = Module(new Queue(
      new MemoryControllerWBBufferEntry(memorySystemParams),
      memorySystemParams.masterCount * 2,
      pipe = true))
    dontTouch(writebackBuffer.io.count)
    // writeback data buffer
    val dq = for { i <- 0 until memorySystemParams.masterCount } yield {
      val beatCount = memorySystemParams.cacheParams.lineWidth / memorySystemParams.busDataWidth
      val (dataCounter, dataWrap) = Counter(io.bus.data_channel(i).fire(), beatCount)
      val dataReg = Reg(Vec(beatCount, UInt(memorySystemParams.busDataWidth.W)))
      val dataLineQueue = Module(new Queue(memorySystemParams.cacheParams.genCacheLine, 2, true))
      val lastWrap = RegInit(false.B)
      lastWrap := dataWrap
      io.bus.data_channel(i).ready := dataLineQueue.io.enq.ready

      when(io.bus.data_channel(i).fire()) {
        dataReg(dataCounter) := io.bus.data_channel(i).bits
      }
      dataLineQueue.io.enq.valid := lastWrap
      dataLineQueue.io.enq.bits := dataReg.asTypeOf(memorySystemParams.cacheParams.genCacheLine)
      //  default, dont dequeue
      dataLineQueue.io.deq
    }
    for { i <- 0 until masterCount } {
      dq(i).ready := false.B
    }
    val writebackDataBuffer = VecInit(for { i <- 0 until memorySystemParams.masterCount } yield {
      dq(i)
    })
    val requestBuffer = Module(new Queue(m.getGenMemReqCommand, 1, pipe=true))
    io.bus.request_channel <> requestBuffer.io.enq
    requestBuffer.io.deq.ready := false.B
    val req_wire: B = coherenceSpec.requestTypeToB(requestBuffer.io.deq.bits.req_type)

    writebackBuffer.io.enq.bits.address := 0.U
    writebackBuffer.io.enq.bits.dirty := 0.U
    writebackBuffer.io.enq.bits.line := 0.U
    writebackBuffer.io.enq.bits.requestor := 0.U
    writebackBuffer.io.enq.valid := false.B

    prlutQ.io.enq.bits.requestTypePRLUT := PendingRequestLookupTable.PendingReqeustLookupTableRequestType.Insert
    prlutQ.io.enq.bits.requestType := req_wire
    prlutQ.io.enq.bits.tag := memorySystemParams.cacheParams.getTagAddress(requestBuffer.io.deq.bits.address)
    prlutQ.io.enq.bits.requestor :=  requestBuffer.io.deq.bits.requester_id
    prlutQ.io.enq.bits.queryAndRemove := false.B
    prlutQ.io.enq.bits.data := requestBuffer.io.deq.bits.criticality

    val requestor = requestBuffer.io.deq.bits.requester_id
    writebackBuffer.io.enq.bits.address := requestBuffer.io.deq.bits.address
    writebackBuffer.io.enq.bits.dirty := requestBuffer.io.deq.bits.dirty.asBool
    writebackBuffer.io.enq.bits.line := writebackDataBuffer(requestor).bits
    // The following logic controls whether enqueue into the prlut or enqueue into the writeback Buffer
    // For Shared, only the first when takes effect
    // For dedicated, the writeback is received from the dedicated path
    for { i <- 0 until masterCount } {
      writebackDataBuffer(i).ready := false.B
    }
    when(requestBuffer.io.deq.valid) {
      memorySystemParams.getDataBusConf match {
        // Process both GetS/GetM/PutM
        case SharedEverything => {
          val getse = memorySystemParams.translateGetS match {
            case true => req_wire.asUInt === coherenceSpec.GetSE.asUInt
            case false => false.B
          }
          when(req_wire.asUInt === coherenceSpec.GetS.asUInt ||
            req_wire.asUInt === coherenceSpec.GetM.asUInt ||
            req_wire.asUInt === coherenceSpec.Upg.asUInt ||
            getse
          ) {
            printf("Initializing at the start trying to enqueue\n")
            // GetS/GetM/Upg
            assert(prlutQ.io.enq.ready, "PRLUT should not block")
            /* Set up the entry into the prlut */
            prlutQ.io.enq.valid := true.B
            requestBuffer.io.deq.ready := true.B
          }.otherwise {
            // PutM/PutS
            assert(req_wire.asUInt === coherenceSpec.PutM.asUInt || req_wire.asUInt === coherenceSpec.PutS.asUInt)
            for { i <- 0 until masterCount } {
              writebackDataBuffer(i).ready := false.B
            }
            when(requestBuffer.io.deq.bits.dirty === 1.U) {
              // we need to get data
              when(writebackDataBuffer(requestor).valid) {
                // now we are ready to send to the writeback buffer
                requestBuffer.io.deq.ready := true.B
                writebackBuffer.io.enq.valid := true.B
                writebackBuffer.io.enq.bits.requestor := requestor
                assert(writebackBuffer.io.enq.ready)
                writebackDataBuffer(requestor).ready := true.B
              }
            }.otherwise {
              requestBuffer.io.deq.ready := true.B
              writebackBuffer.io.enq.valid := true.B
              assert(writebackBuffer.io.enq.ready)
            }
          }
        }
        case DedicatedDataBusTwoWay | DedicatedDataBusOneWay => {
          // must be one of GetS/GetM/Upg
          // elided
          val getse: Bool = memorySystemParams.translateGetS match {
            case true => req_wire.asUInt === coherenceSpec.GetSE.asUInt
            case false => false.B
          }
          assert(req_wire.asUInt === coherenceSpec.GetS.asUInt ||
            req_wire.asUInt === coherenceSpec.GetM.asUInt ||
            req_wire.asUInt === coherenceSpec.Upg.asUInt ||
            getse
          )
          assert(prlutQ.io.enq.ready, "PRLUT should not block")
          prlutQ.io.enq.valid := true.B
          requestBuffer.io.deq.ready := true.B
        }
        case _ => {
          println("Configuration Not Supported Yet")
          assert(false, "Configuration Not Supported Yet")
        }
      }
    }.otherwise {
      // Do nothing, the dequeue of prlut is done in the back end
    }
    io.backendWBEntry <> writebackBuffer.io.deq
    io.slotOwner := io.bus.slotOwner
    io.slotStart := io.bus.slotStart

    val (wbBufferRRCounter, _) = Counter(true.B, memorySystemParams.masterCount)
    lazy val q = for { i <- 0 until memorySystemParams.masterCount } yield Module(new Queue(memorySystemParams.getGenMemReqCommand, 8, pipe = true))
    memorySystemParams.getDataBusConf match {
      case DedicatedDataBusTwoWay | DedicatedDataBusOneWay=> {
        // buffer writeback requests
        for { i <- 0 until memorySystemParams.masterCount } {
          q(i).io.enq <> io.bus.dedicated_request_channel(i)
          q(i).io.deq.ready := false.B
          writebackDataBuffer(i).ready := false.B
          val isDirty = q(i).io.deq.bits.dirty === 1.U
          when(i.U === wbBufferRRCounter && q(i).io.deq.valid) {
            q(i).io.deq.ready :=  isDirty && writebackDataBuffer(i).valid || !isDirty
            writebackDataBuffer(i).ready := isDirty
            writebackBuffer.io.enq.valid := isDirty && writebackDataBuffer(i).valid || !isDirty
            assert(writebackBuffer.io.enq.ready)
            writebackBuffer.io.enq.bits.address := q(i).io.deq.bits.address
            writebackBuffer.io.enq.bits.dirty := q(i).io.deq.bits.dirty.asBool
            writebackBuffer.io.enq.bits.line := writebackDataBuffer(i).bits
            writebackBuffer.io.enq.bits.requestor := i.U
          }
        }
      }
      case _ => {
        // does nothing
        for { i <- 0 until memorySystemParams.masterCount } {
          io.bus.dedicated_request_channel(i).ready := false.B
          q(i).io.enq.bits := 0.U.asTypeOf(q(i).io.enq.bits)
          q(i).io.enq.valid := false.B
          q(i).io.deq.ready := false.B
        }
      }
    }

    io.bus.response_channel <> io.response_channel
    io.bus.response_data_channel <> io.response_data_channel
  }
  object BackEndState extends ChiselEnum {
    val idle, request, waitInsert, waitOther = Value
    //                                              TagCheck
    // dram has to be separated
  }
  // A wrapper for the PRLUT
  class BackEnd[S <: Data, M <: Data, B <: Data](private val memorySystemParams: MemorySystemParams,
                                    private val coherenceSpec: CoherenceSpec[S, M, B]
                                   ) extends Module {
    private val m = memorySystemParams
    private val busDataWidth: Int = m.busDataWidth
    private val masterCount = m.masterCount
    private val genMemRespCommand: MemRespCommand = m.getGenMemRespCommand
    val io = IO(new Bundle {
      // facing backend of MC
      // val backendWBEntry = Flipped(Decoupled(new MemoryControllerWBBufferEntry(m)))
      val prlutInsert = Flipped(Decoupled(new PendingRequestLookupTable.PRLUTRequestChannel(m, m.genCrit, coherenceSpec)))


      val prlutreq = Flipped(Decoupled(new PendingRequestLookupTable.PRLUTRequestChannel(m, m.genCrit, coherenceSpec)))
      val prlutresp = Decoupled(new PendingRequestLookupTable.PRLUTResponseChannel(m, m.genCrit, coherenceSpec))

      val slotOwner = Input(UIntHolding(memorySystemParams.masterCount))
      val slotStart = Input(Bool())
      val version = Output(UInt(32.W))
    })
    val prlut = Module(new PendingRequestLookupTable(memorySystemParams, coherenceSpec,
      memorySystemParams.withCriticality
    ))

    val state = RegInit(BackEndState.idle)
    val slotDone = RegInit(false.B)
    val slotHasRequest = RegInit(false.B)
    prlut.io.requestChannel.valid := false.B
    // default value
    prlut.io.requestChannel.bits := io.prlutInsert.bits
    prlut.io.responseChannel.ready := false.B
    io.prlutInsert.ready := false.B
    io.version := prlut.io.version

    val prlutInternalReqBuffer = Module(new Queue( new PendingRequestLookupTable.PRLUTRequestChannel(m, m.genCrit, coherenceSpec), 2))
    val prlutInternalRespBuffer = Module(new Queue(new PendingRequestLookupTable.PRLUTResponseChannel(m, m.genCrit, coherenceSpec), 2))
    prlutInternalReqBuffer.io.deq.ready := false.B

    io.prlutreq <> prlutInternalReqBuffer.io.enq
    io.prlutresp <> prlutInternalRespBuffer.io.deq

    // This will take around 4 cycles, so the slot width will be 12 cycles + tag check in the best case
    // We still need this for dedicated
    when(io.slotStart) {
      slotDone := false.B
    }
    prlutInternalRespBuffer.io.enq.bits := prlut.io.responseChannel.bits
    prlutInternalRespBuffer.io.enq.valid := false.B
    switch(state) {
      // Prioritize the prlut
      // TODO: need to have status for slot
      is(BackEndState.idle) {
        when(io.prlutInsert.valid && !slotDone) {
          io.prlutInsert.ready := prlut.io.requestChannel.ready
          prlut.io.requestChannel.valid := io.prlutInsert.valid
          slotDone := true.B
          state := BackEndState.waitInsert
        }.otherwise {
          // for the request buffer
          when(prlutInternalReqBuffer.io.deq.valid) {
            prlut.io.requestChannel.valid := prlutInternalReqBuffer.io.deq.valid
            prlutInternalReqBuffer.io.deq.ready := prlut.io.requestChannel.ready
            prlut.io.requestChannel.bits := prlutInternalReqBuffer.io.deq.bits
            when(prlutInternalReqBuffer.io.deq.fire()) {
              state := BackEndState.waitOther
            }
          }
        }
      }
      is(BackEndState.waitInsert) {
        prlut.io.responseChannel.ready := true.B
        when(prlut.io.responseChannel.fire()) {
          assert(prlut.io.responseChannel.bits.responseType ===
            PendingRequestLookupTable.PendingReqeustLookupTableResponseType.Success)
          state := BackEndState.idle
        }
      }
      is(BackEndState.waitOther) {
        prlutInternalRespBuffer.io.enq.valid := prlut.io.responseChannel.valid
        prlut.io.responseChannel.ready := prlutInternalRespBuffer.io.enq.ready
        when(prlutInternalRespBuffer.io.enq.fire()) {
          state := BackEndState.idle
        }
      }
    }

    // The DRAM Engine

  }
  class DRAMBus(private val memorySystemParams: MemorySystemParams) extends Bundle {
    private val genDramReq: DRAMReq = memorySystemParams.getGenDramReq
    private val genDramResp: DRAMResp = memorySystemParams.getGenDramResp
    val request_channel = Decoupled(genDramReq)
    val response_channel = Flipped(Decoupled(genDramResp))
  }
  class PipelinedMemoryControllerIO[S <: Data, M <: Data, B <: Data](
                                     private val idWidth: Int,
                                     private val memorySystemParams: MemorySystemParams,
                                     private val coherenceSpec: CoherenceSpec[S, M, B],
                                     private val genErrorMessage: ErrorMessage
                                   ) extends Bundle {
    private val masterCount: Int = memorySystemParams.masterCount
    private val genMemReqCommand: MemReqCommand = memorySystemParams.getGenMemReqCommand
    private val genMemRespCommand: MemRespCommand = memorySystemParams.getGenMemRespCommand

    private val busDataWidth: Int = memorySystemParams.busDataWidth

    val bus = new BusSideIO(memorySystemParams)
    val dram = new DRAMBus(memorySystemParams)
    val err = Output(genErrorMessage)
  }
  class DRAMModuleIO[S <: Data, M <: Data, B <: Data](
                                                     private val memorySystemParams: MemorySystemParams,
                                                     private val coherenceSpec: CoherenceSpec[S, M, B]
                                                     ) extends Bundle {
    private val m = memorySystemParams
    private val masterCount = m.masterCount
    private val genMemRespCommand: MemRespCommand = memorySystemParams.getGenMemRespCommand
    private val busDataWidth: Int = memorySystemParams.busDataWidth
    val dram = new DRAMBus(memorySystemParams)
    val req = Decoupled(new PendingRequestLookupTable.PRLUTRequestChannel(m, m.genCrit, coherenceSpec))
    val resp = Flipped(Decoupled(new PendingRequestLookupTable.PRLUTResponseChannel(m, m.genCrit, coherenceSpec)))
    val wb = Flipped(Decoupled(new MemoryControllerWBBufferEntry(memorySystemParams)))

    val response_channel = Vec(masterCount, Decoupled(genMemRespCommand))
    val response_data_channel = Vec(masterCount, Decoupled(UInt(busDataWidth.W)))
    val slotOwner = Input(UIntHolding(m.masterCount))
    val version = Input(UInt(32.W))
  }
  object DRAMState extends ChiselEnum {
    val idle, wb, reqCheck, reqCheckResp, tagCheck, coherenceResponse, reqRemove, reqRemoveResp,
    dramRead, dramWrite, dramWait, tagWrite, response, preTagCheck = Value
  }
  class DRAMModule[S <: Data, M <: Data, B <: Data](memorySystemParams: MemorySystemParams,
                                                    coherenceSpec: CoherenceSpec[S, M, B]) extends Module {
    val io = IO(new DRAMModuleIO(memorySystemParams, coherenceSpec))
    val m = memorySystemParams
    val lastServiced = RegInit(0.U(2.W))
    val state = RegInit(DRAMState.idle)
    val coherenceTable = Module(CoherenceSpec.translateToSharedModule(coherenceSpec)())
    val respEntry = Reg(new PendingRequestLookupTable.PRLUTResponseChannel(m, m.genCrit, coherenceSpec))
    val address = Reg(m.cacheParams.genAddress)
    val tagArray = for { i <- 0 until m.masterCount * m.cacheParams.nWays } yield {
      val valid = RegInit(VecInit(Seq.fill(m.cacheParams.nSets) { false.B }))
      val tag = SyncReadMem(m.cacheParams.nSets, new TagEntry(m, coherenceSpec))
      (valid, tag)
    }
    dontTouch(address)
    val data = Reg(m.cacheParams.genCacheLine)
    val setAddress = Reg(m.cacheParams.genSet)
    val tagEntry = VecInit(for { i <- 0 until m.masterCount * m.cacheParams.nWays } yield {
      val te = Wire(new TagEntry(m, coherenceSpec))
      // te := tagArray(i)._2.read(io.resp.bits.tag)
      te := tagArray(i)._2.read(m.cacheParams.getTagAddress(address), state === DRAMState.preTagCheck)
      te
    })
    val vacantWay = RegInit(0.U(log2Ceil(tagEntry.length).W))
    val coherenceResponse = Wire(new CoherenceResponse[S, B](coherenceSpec.getGenStateF,
      coherenceSpec.getGenBusReqTypeF))
    coherenceResponse := coherenceTable.io.resp
    val hasDRAMResp = RegInit(false.B)
    val isWB = RegInit(false.B)
    val isDirty = RegInit(true.B)
    val hasVacant = RegInit(false.B)
    val matchedWayReg = Reg(UIntHolding(tagEntry.length))
    val matchedEntryReg = Reg(new TagEntry(m, coherenceSpec))
    // Basic RR
    io.dram.request_channel.bits := 0.U.asTypeOf(io.dram.request_channel.bits)
    io.dram.request_channel.bits.length := "b110".U // READ
    io.dram.request_channel.bits.data := data // READ
    io.dram.request_channel.bits.address := address // READ

    // overwrite if serviced
    io.wb.ready := false.B
    dontTouch(io.wb)

    val tagToWrite = Wire(new TagEntry(m, coherenceSpec))
    when(coherenceResponse.markDirty) {
      tagToWrite.dirty := true.B
    }.otherwise {
      tagToWrite.dirty := false.B
    }
    tagToWrite.tag := m.cacheParams.getTagAddress(address)
    tagToWrite.state := coherenceResponse.nextState

    val respChannel = for { i <- 0 until m.masterCount } yield {
      val respCmdQ = Module(new Queue(m.getGenMemRespCommand, 1))
      respCmdQ.io.enq.valid := false.B
      respCmdQ.io.enq.bits := 0.U.asTypeOf(respCmdQ.io.enq.bits)

      respCmdQ.io.deq <> io.response_channel(i)
      val respDataQ = Module(new Queue(m.cacheParams.genCacheLine, 1))
      val beat = m.cacheParams.lineWidth / m.busDataWidth
      val dataWord = Wire(Vec(beat, UInt(m.busDataWidth.W)))
      dataWord := respDataQ.io.deq.bits.asTypeOf(dataWord)
      val (dataCounter, dataCounterWrap) = Counter(io.response_data_channel(i).fire(), beat)
      io.response_data_channel(i).valid := respDataQ.io.deq.valid
      io.response_data_channel(i).bits  := dataWord(dataCounter)
      respDataQ.io.deq.ready := io.response_data_channel(i).ready && dataCounterWrap

      respCmdQ.io.enq.bits.address := address
      respCmdQ.io.enq.bits.is_edata := false.B
      respCmdQ.io.enq.bits.ack := true.B
      respDataQ.io.enq.bits := data

      respCmdQ.io.enq.valid := false.B
      respDataQ.io.enq.valid := false.B

      (respCmdQ, respDataQ)
    }

    io.req.bits := 0.U.asTypeOf(io.req.bits)
    io.req.bits.requestor := io.slotOwner
    io.req.valid := false.B
    coherenceTable.io.query.message := 0.U.asTypeOf(coherenceTable.io.query.message)
    coherenceTable.io.query.state := 0.U.asTypeOf(coherenceTable.io.query.state)

    coherenceTable.io.enable := false.B

    dontTouch(coherenceTable.io.query.message)
    dontTouch(coherenceTable.io.query.state)
    dontTouch(coherenceTable.io.resp.nextState)
    dontTouch(coherenceTable.io.resp.defined)
    dontTouch(coherenceTable.io.enable)

    io.resp.ready := false.B
    io.dram.response_channel.ready := false.B
    io.dram.request_channel.valid := false.B
    coherenceTable.io.query.relCrit := RelativeCriticality.SameCrit

    val tagMatch = VecInit(for { i <- 0 until tagEntry.length}
      yield {
        tagEntry(i).tag === m.cacheParams.getTagAddress(address) && tagArray(i)._1(m.cacheParams.getTagAddress(address))
      })
    val hasMatch = tagMatch.asUInt.orR()
    val isVacant = VecInit(for { i <- 0 until tagEntry.length } yield {
      !tagArray(i)._1(respEntry.tag)
    })

    val lastprlutVersion = RegInit(2147483647.U(32.W))
    val changed = io.version === lastprlutVersion

    switch(state) {
      // Note: the RR is separated into these three states
      is(DRAMState.idle) {
        isWB := false.B
        hasVacant := false.B
        when(lastServiced === 0.U) {
          when(io.wb.valid) {
            state := DRAMState.wb
            io.wb.ready := true.B
            address := io.wb.bits.address
            data := io.wb.bits.line
            lastServiced := 1.U
            isDirty := io.wb.bits.dirty
          }.otherwise {
            // no writeback
            state := DRAMState.reqCheck
          }
        }.otherwise {
          state := DRAMState.reqCheck
        }
      }
      is(DRAMState.reqCheck) {
        io.req.valid := true.B
        io.req.bits.queryAndRemove := false.B
        memorySystemParams.getDataBusConf match {
          case SharedEverything => {
            if (m.isConventionalProtocol) {
              // io.req.bits.requestTypePRLUT := PendingRequestLookupTable.PendingReqeustLookupTableRequestType.NextRequestInGlobalOrder
              io.req.bits.requestTypePRLUT := PendingRequestLookupTable.PendingReqeustLookupTableRequestType.NextRequestGivenSlot
            } else {
              io.req.bits.requestTypePRLUT := PendingRequestLookupTable.PendingReqeustLookupTableRequestType.NextRequestGivenSlot
            }
          }
          case DedicatedDataBusTwoWay | DedicatedDataBusOneWay => {
            io.req.bits.requestTypePRLUT := PendingRequestLookupTable.PendingReqeustLookupTableRequestType.NextRequestInGlobalOrder
          }
        }
        assert(io.req.ready)
        state := DRAMState.reqCheckResp
        lastprlutVersion := io.version
      }
      is(DRAMState.reqCheckResp) {
        io.resp.ready := true.B
        when(io.resp.fire()) {
          when(io.resp.bits.responseType ===
            PendingRequestLookupTable.PendingReqeustLookupTableResponseType.Found) {
            lastServiced := 0.U
            state := DRAMState.preTagCheck
            respEntry := io.resp.bits
            address := m.cacheParams.tagAddrToLineAddr(io.resp.bits.tag)
            /*
            for { i <- 0 until tagArray.length } {
              tagEntry(i) := tagArray(i)._2.read(io.resp.bits.tag)
              available in the next cycle
            } */
          }.elsewhen(io.resp.bits.responseType ===
            PendingRequestLookupTable.PendingReqeustLookupTableResponseType.NotFound){
            state := DRAMState.idle
            when(lastServiced === 0.U) {
              lastServiced := 1.U
            }.elsewhen(lastServiced === 1.U) {
              when(io.wb.valid) {
                state := DRAMState.wb
                io.wb.ready := true.B
                address := io.wb.bits.address
                data := io.wb.bits.line
                isDirty := io.wb.bits.dirty
                lastServiced := 1.U
              }.otherwise {
                lastServiced := 0.U
              }
            }
          }.otherwise {
            assert(false.B, "Invalid response")
          }
        }
      }
      is(DRAMState.wb) {
        isWB := true.B
        state := DRAMState.preTagCheck
      }

      is(DRAMState.preTagCheck) {
        // at this point the address should be ready
        state := DRAMState.tagCheck
        printf(p"Read Tag For Addr: ${address}\n")
        for { i <- 0 until m.masterCount } {
          printf(p" way ${i}: ")
          utils.printbundle(tagEntry(i))
          printf("\n")
        }
        printf("\n")
      }

      is(DRAMState.tagCheck) {

        hasVacant := isVacant.asUInt.orR
        val matchedWay = Wire(UIntHolding(tagEntry.length))
        matchedWay := 0.U
        assert(PopCount(tagMatch) <= 1.U, "At most one match")
        for { i <- 0 until tagMatch.length } {
          when(tagMatch(i)) {
            matchedWay := i.U
          }
          when(isVacant(i)) {
            vacantWay := i.U
          }
        }
        matchedWayReg := matchedWay
        val matchedEntry = tagEntry(matchedWay)
        matchedEntryReg := matchedEntry
        // TODO: we need to have this for criticality
        coherenceTable.io.query.relCrit := RelativeCriticality.SameCrit
        when(isWB) {
          coherenceTable.io.query.message := coherenceSpec.PutM
        }.otherwise {
          coherenceTable.io.query.message := respEntry.requestType
        }
        when(hasMatch) {
          coherenceTable.io.query.state := matchedEntry.state
        }.otherwise {
          coherenceTable.io.query.state := 0.U.asTypeOf(coherenceSpec.getGenState)
        }
        state := DRAMState.coherenceResponse
        hasDRAMResp := false.B
        coherenceTable.io.enable := true.B
        printf("Coherence Query Addr: \n")
        utils.printbundle(coherenceTable.io.query)
        printf(p"\n ${Hexadecimal(address)}\n")
      }
      is(DRAMState.coherenceResponse) {
        when(coherenceResponse.defined) {
          assert(!coherenceResponse.isErr, "Should not trigger invalid transition")
        }
        setAddress := m.cacheParams.getLineAddress(address)
        state := DRAMState.reqRemove
      }
      is(DRAMState.reqRemove) {
        when(coherenceResponse.defined && coherenceResponse.prlutRemove) {
          io.req.valid := true.B
          io.req.bits.queryAndRemove := true.B
          memorySystemParams.getDataBusConf match {
            case SharedEverything => {
              if (m.isConventionalProtocol) {
                // io.req.bits.requestTypePRLUT := PendingRequestLookupTable.PendingReqeustLookupTableRequestType.NextRequestInGlobalOrder
                io.req.bits.requestTypePRLUT := PendingRequestLookupTable.PendingReqeustLookupTableRequestType.NextRequestGivenSlot
              } else {
                io.req.bits.requestTypePRLUT := PendingRequestLookupTable.PendingReqeustLookupTableRequestType.NextRequestGivenSlot
              }
            }
            case DedicatedDataBusTwoWay | DedicatedDataBusOneWay => {
              io.req.bits.requestTypePRLUT := PendingRequestLookupTable.PendingReqeustLookupTableRequestType.NextRequestInGlobalOrder
            }
            case _ => {
              assert(false, "Not supported")
            }
          }
          state := DRAMState.reqRemoveResp
        }.otherwise {
          state := DRAMState.reqRemoveResp
        }
      }
      is(DRAMState.reqRemoveResp) {
        when(coherenceResponse.defined && coherenceResponse.prlutRemove) {
          io.resp.ready := true.B
          when(io.resp.fire()) {
            assert(io.resp.bits.responseType ===
              PendingRequestLookupTable.PendingReqeustLookupTableResponseType.FoundAndRemoved)
            state := DRAMState.dramRead
          }
        }.otherwise {
          state := DRAMState.dramRead
        }
      }
      is(DRAMState.dramRead) {
        when(coherenceResponse.defined && coherenceResponse.dramRead) {
          io.dram.request_channel.valid := true.B
          io.dram.request_channel.bits.mem_type := 1.U // READ
          when(io.dram.request_channel.fire()) {
            state := DRAMState.dramWrite
            hasDRAMResp := true.B
          }
        }.otherwise {
          state := DRAMState.dramWrite
        }
      }
      is(DRAMState.dramWrite) {
        when(coherenceResponse.defined && coherenceResponse.dramWrite && isDirty) {
          io.dram.request_channel.valid := true.B
          io.dram.request_channel.bits.mem_type := 0.U // Write
          when(io.dram.request_channel.fire()) {
            state := DRAMState.dramWait
            hasDRAMResp := true.B
          }
        }.otherwise {
          state := DRAMState.dramWait
        }
      }
      is(DRAMState.dramWait) {
        when(hasDRAMResp) {
          io.dram.response_channel.ready := true.B
          when(io.dram.response_channel.fire()) {
            data := io.dram.response_channel.bits.data
            state := DRAMState.tagWrite
          }
        }.otherwise {
          state := DRAMState.tagWrite
        }
      }
      is(DRAMState.tagWrite) {
        when(coherenceResponse.insertTag) {
          printf("Target To Write: \n")
          utils.printbundle(coherenceResponse)
          printf("\n")
          utils.printbundle(tagToWrite)
          printf("\n")
          printf(p"Before Write (vacant=${vacantWay}): \n")
          for { i <- tagArray.indices } {
            printf(p" way ${i.U}: ${tagArray(i)._1}\n")
          }
          // insert into vacant
          assert(hasVacant)
          for { i <- tagArray.indices } {
            when(i.U === vacantWay) {
              tagArray(i)._1(setAddress) := true.B
              tagArray(i)._2.write(setAddress, tagToWrite)
            }
          }
        }.elsewhen(coherenceResponse.updateTag) {
          val entry = WireInit(matchedEntryReg)
          entry.state := coherenceResponse.nextState
          when(coherenceResponse.markDirty) {
            entry.dirty := true.B
          }
          when(coherenceResponse.markClean) {
            entry.dirty := true.B
          }
          for { i <- tagArray.indices } {
            when(i.U === matchedWayReg) {
              tagArray(i)._2.write(setAddress, entry)
              assert(tagArray(i)._1(setAddress), "Should only update valid tag")
            }
          }
        }.elsewhen(coherenceResponse.removeTag) {
          for { i <- tagArray.indices } {
            when(i.U === matchedWayReg) {
              tagArray(i)._1(setAddress) := false.B
            }
          }
        }
        state := DRAMState.response
      }
      is(DRAMState.response) {
        printf("After Write: \n")
        for { i <- tagArray.indices } {
          printf(p" way ${i.U}: ${tagArray(i)._1}\n")
        }
        when(coherenceResponse.pushCacheResp) {
          for { i <- 0 until m.masterCount } {
            when(i.U === respEntry.requestor) {
              when(hasDRAMResp) {
                when(respChannel(i)._1.io.enq.ready && respChannel(i)._2.io.enq.ready) {
                  respChannel(i)._1.io.enq.valid := true.B
                  respChannel(i)._1.io.enq.bits.ack := 0.U
                  respChannel(i)._2.io.enq.valid := true.B
                  respChannel(i)._1.io.enq.bits.is_edata := coherenceResponse.isEData
                  state := DRAMState.idle
                }
              }.otherwise { // no data
                when(respChannel(i)._1.io.enq.ready) {
                  respChannel(i)._1.io.enq.valid := true.B
                  respChannel(i)._1.io.enq.bits.ack := 1.U
                  state := DRAMState.idle
                }
              }
            }
          }
        }.otherwise {
          state := DRAMState.idle
        }
      }
    }
  }
}

class PipelinedMemoryController[S <: Data, M <: Data, B <: Data](val idWidth: Int,
                                val memorySystemParams: MemorySystemParams,
                                val coherenceSpec: CoherenceSpec[S, M, B],
                                val genErrorMessage: ErrorMessage) extends Module {
  val masterCount: Int = memorySystemParams.masterCount
  val cacheParams: SimpleCacheParams = memorySystemParams.cacheParams
  // val genMemReq: MemReq = memorySystemParams.getGenMemReq
  val genMemReqCommand: MemReqCommand = memorySystemParams.getGenMemReqCommand
  val genMemResp: MemResp = memorySystemParams.getGenMemResp
  val genMemRespCommand: MemRespCommand = memorySystemParams.getGenMemRespCommand
  val genDramReq: DRAMReq = memorySystemParams.getGenDramReq
  val genDramResp: DRAMResp = memorySystemParams.getGenDramResp
  val busDataWidth: Int = memorySystemParams.busDataWidth
  val beats_per_line: Int = memorySystemParams.cacheParams.lineWidth / memorySystemParams.busDataWidth

  val io = IO(new PipelinedMemoryController.PipelinedMemoryControllerIO(idWidth,
    memorySystemParams, coherenceSpec, genErrorMessage))
  val m = memorySystemParams
  val frontend = Module(new PipelinedMemoryController.FrontEnd(m, coherenceSpec))
  val backend = Module(new PipelinedMemoryController.BackEnd(m, coherenceSpec))
  val dramEngine = Module(new PipelinedMemoryController.DRAMModule(m, coherenceSpec))

  m.getDataBusConf match {
    case SharedEverything => {
      for { i <- 0 until masterCount} {
        assert(!io.bus.dedicated_request_channel(i).valid, "Should not receive dedicated request in Shared Everything")
      }
    }
    case _ => {}
  }

  /* module connection */
  frontend.io.bus <> io.bus
  backend.io.prlutreq <> dramEngine.io.req
  backend.io.prlutresp <> dramEngine.io.resp
  backend.io.slotStart := frontend.io.slotStart
  backend.io.slotOwner := frontend.io.slotOwner
  frontend.io.response_data_channel <> dramEngine.io.response_data_channel
  frontend.io.response_channel <> dramEngine.io.response_channel
  frontend.io.prlutInsert <> backend.io.prlutInsert

  dramEngine.io.dram <> io.dram
  dramEngine.io.wb <> frontend.io.backendWBEntry
  dramEngine.io.slotOwner := backend.io.slotOwner
  dramEngine.io.version := backend.io.version

  // Tie off error
  io.err.src := 0.U
  io.err.msg := 0.U
  io.err.valid := false.B
}
