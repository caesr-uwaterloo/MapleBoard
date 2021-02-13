
package components

import chisel3._
import chisel3.util._
import param.{CoreParam, RISCVParam}
import params.{CoherenceSpec, MemorySystemParams, PMSISpec, SimpleCacheParams}
import utils.CoreGen._
import utils.withCompilerOptionParser

// scalastyle:off
class PipelinedCache[S <: Data, M <: Data, B <: Data](coreParam: CoreParam, memorySystemParams: MemorySystemParams, id: Int,
                                           cohSpec: CoherenceSpec[S, M, B]) extends Module {
  val cacheParams: SimpleCacheParams = memorySystemParams.cacheParams
  val depth: Int = cacheParams.nSets
  val lineSize: Int = cacheParams.lineBytes
  val addrWidth : Int = memorySystemParams.addrWidth
  val masterCount: Int = memorySystemParams.masterCount
  val dataWidth: Int = memorySystemParams.dataWidth
  val busDataWidth: Int = memorySystemParams.busDataWidth
  val genCacheReq: CacheReq = memorySystemParams.getGenCacheReq
  val genCacheResp: CacheResp= memorySystemParams.getGenCacheResp
  val genControllerReq: MemReq = memorySystemParams.getGenMemReq
  val genControllerReqCommand: MemReqCommand = memorySystemParams.getGenMemReqCommand
  val genControllerResp: MemRespCommand = memorySystemParams.getGenMemRespCommand
  val genSnoopReq: SnoopReq = memorySystemParams.getGenSnoopReq
  val genSnoopResp: SnoopResp = memorySystemParams.getGenSnoopResp
  val genDebugCacheLine: DebugCacheline = memorySystemParams.getGenDebugCacheline
  val wordWidth = coreParam.isaParam.XLEN
  val m = memorySystemParams

  val tags = for { i <- 0 until cacheParams.nWays } yield {
    // easier to reset
    val tag_valid = RegInit(VecInit(Seq.fill(cacheParams.nSets) { false.B }))
    val tag = Module(new DualPortedRam(new TagEntry(m, cohSpec), cacheParams.nSets))
    (tag_valid, tag)
  }

  // maybe it's fine to not have byte enable signals in the data array
  val data = for { i <- 0 until cacheParams.nWays } yield {
    // val data = Module(new DualPortedRamBE(cacheParams.nSets, cacheParams.lineWidth / 8, UInt(8.W)))
    val data = Module(new DualPortedRam(cacheParams.genCacheLine, cacheParams.nSets))
    (data)
  }

  val nrtCores = 1
  val io = IO(new Bundle {
    // from core to cc
    val core = new Bundle {
      val request_channel = Flipped(Decoupled(genCacheReq))
      val response_channel = Decoupled(genCacheResp)
    }
    // from cc to bus
    val bus = new Bundle {
      val request_channel = Decoupled(genControllerReqCommand)
      val response_channel = Flipped(Decoupled(genControllerResp))
      val dataq = Decoupled(UInt(busDataWidth.W))
      val dataq_in = Flipped(Decoupled(UInt(busDataWidth.W)))

      val dedicated_bus_request_channel = Decoupled(m.getGenMemReqCommand)
    }

    val snoop = new Bundle {
      val request_channel = Flipped(Decoupled(genSnoopReq))
      val response_channel = Decoupled(genSnoopResp)
    }
    val id = Input(UInt((log2Ceil(masterCount) + 1).W))
    val criticality = Input(m.genCrit())
    val time = Input(UIntHolding(128))

    // This is for Coverage test
    val query_coverage = Output(ValidIO(cohSpec.getGenCohQuery))

    val rb_fire = Output(Bool())
    val latencyCounterReg = Output(UInt(64.W))
  })
  val busy = RegInit(false.B)

  val mshr = Reg(Vec(memorySystemParams.MSHRSize, genCacheReq))
  val mshrValid = RegInit(VecInit(Seq.fill(memorySystemParams.MSHRSize) { false.B }))
  val pendingMem = Reg(Vec(memorySystemParams.pendingMemSize,
    new PendingMemoryRequestEntry(memorySystemParams, cohSpec)))
  val pendingMemValid = RegInit(VecInit(Seq.fill(memorySystemParams.pendingMemSize) { false.B }))
  val hasMSHR = !mshrValid.reduceLeft { (u, v) => u && v }
  val hasPendingMem = !pendingMemValid.reduceLeft { (u, v) => u && v }
  val replayBuffer = Module(new Queue(genCacheReq, 1, pipe = true))
  val replayBufferBuffer = Module(new Queue(genCacheReq, 1, pipe = true))
  val rb_fire = WireInit(false.B)
  val latencyCounter = RegInit(0.U(64.W))
  val latencyCounterReg = RegInit(0.U(64.W))
  latencyCounterReg := latencyCounter
  when(io.core.request_channel.fire() || rb_fire || io.core.response_channel.fire() || hasMSHR) {
    latencyCounter := 0.U
  }.otherwise {
    latencyCounter := latencyCounter + 1.U
  }
  io.latencyCounterReg := latencyCounterReg
  dontTouch(io.latencyCounterReg)
  dontTouch(latencyCounterReg)



  // data buffer for storing the data
  // a trick here: when counter is in [0, 7], it is accepting data from bus
  // when counter is 8, the data is ready to use, so incrementing it will result in a counter wrap
  val dataBeats = m.cacheParams.lineWidth / m.busDataWidth
  val readDataBuffer = Wire(Bool())
  val dataBuffer = Reg(m.cacheParams.genCacheLineBytes)
  val (dataBufferCounter, dataBufferCounterWrap) = Counter(io.bus.dataq_in.fire() || readDataBuffer,
    dataBeats + 1)
  val dataBufferDone = dataBufferCounter === dataBeats.U
  io.bus.dataq_in.ready := !dataBufferDone
  assert(!(readDataBuffer && io.bus.dataq_in.fire()), "cannot read data buffer and write data buffer at the same time")
  for { i <- 0 until dataBeats } {
    when(i.U === dataBufferCounter && io.bus.dataq_in.fire()) {
      val start = i * m.busDataWidth / 8
      for { j <- 0 until m.busDataWidth / 8} {
        dataBuffer(start + j) := io.bus.dataq_in.bits((j + 1) * 8 - 1, j * 8)
      }
    }
  }
  // All other cores request a line + replacement
  val dedicatedReqChannel = Module(new Queue(m.getGenMemReqCommand, m.masterCount + 1))

  dedicatedReqChannel.io.enq.bits := io.bus.dedicated_bus_request_channel.bits
  when(io.bus.dedicated_bus_request_channel.fire()) {
    dedicatedReqChannel.io.enq.valid := true.B
    assert(dedicatedReqChannel.io.enq.ready)
  }.otherwise {
    dedicatedReqChannel.io.enq.valid := false.B
  }

  when(io.bus.response_channel.valid) {
    printf("[CC%d] DATABUFFER. Get Response, Resp Ready: %b, Data Valid: %b, Data Ready: %b, ReadBufferDone: %b, dbCounter: %x\n", io.id, io.bus.response_channel.ready, io.bus.dataq_in.valid, io.bus.dataq_in.ready, dataBufferDone, dataBufferCounter)
  }
  when(dataBufferCounter =/= 0.U && dataBufferCounter < 8.U && !io.bus.dataq_in.fire()) {
    printf("[CC%d] Data Bus should burst...", io.id)
    assert(false.B, "databus should burst")
  }

  val arbitrate = Module(new InputArbitrationPipe(memorySystemParams, cohSpec))
  val data_mod = Module(new DataModification(coreParam, m, cohSpec))
  // arbitrate.io.dedicated_wb_fire := io.bus.dedicated_bus_request_channel.fire()
  // arbitrate.io.dedicated_address := io.bus.dedicated_bus_request_channel.bits.address
  arbitrate.io.bus_dedicated_request_channel <> dedicatedReqChannel.io.deq
  arbitrate.io.core_request_channel <> io.core.request_channel
  // we need this to prevent a request from intercepting a read of the request...
  // we do not accept new request when the replay buffer is empty
  arbitrate.io.core_request_channel.valid := io.core.request_channel.valid && !replayBuffer.io.deq.valid
  io.core.request_channel.ready := arbitrate.io.core_request_channel.ready && !replayBuffer.io.deq.valid
  val respCommandQueue = Module(new Queue(genControllerResp, 1, pipe = true))
  arbitrate.io.bus_response_channel <> respCommandQueue.io.deq
  respCommandQueue.io.enq <> io.bus.response_channel

  when(dataBufferCounter =/= 0.U || dataBufferCounter === 0.U && io.bus.dataq_in.fire()) {
    printf("[CC%d] DATABUFFER received data, dataBufferCounter: %d, dataBufferCounterWrap: %b readDataBuffer: %b, dataBufferDone: %b\n", io.id, dataBufferCounter, dataBufferCounterWrap, readDataBuffer, dataBufferDone)
    printf("[CC%d] The Command Q Dequeue: V %b R %b V %b R %b\n", io.id,
      arbitrate.io.bus_response_channel.valid,
      arbitrate.io.bus_response_channel.ready,
      respCommandQueue.io.deq.valid,
      respCommandQueue.io.deq.ready
    )
  }
  arbitrate.io.snoop_request_channel <> io.snoop.request_channel
  // arbitrate.io.replay_request_channel <> replayBufferBuffer.io.deq
  // arbitrate.io.replay_request_channel.valid := replayBuffer.io.deq.valid && data_mod.io.releaseReplay
  // replayBuffer.io.deq.ready := arbitrate.io.replay_request_channel.ready && data_mod.io.releaseReplay
  arbitrate.io.replay_request_channel <> replayBufferBuffer.io.deq
  arbitrate.io.mshrVacant := hasMSHR
  arbitrate.io.pendingMemVacant := hasPendingMem
  arbitrate.io.busy := busy
  arbitrate.io.id := io.id
  arbitrate.io.time := io.time

  val pipe0 = Module(new Queue(new PipeData(m, cohSpec), 1, pipe = true))
  pipe0.io.enq <> arbitrate.io.pipe
  val pipe1 = Module(new Queue(new PipeData(m, cohSpec), 1, pipe = true))
  val tag_check = Module(new TagCheck(memorySystemParams, cohSpec))
  val tag_valid = Reg(Vec(cacheParams.nWays, Bool()))
  tag_check.io.pipe_in <> pipe0.io.deq
  tag_check.io.pipe_out <> pipe1.io.enq
  tag_check.io.id := io.id
  tag_check.io.time := io.time
  tag_check.io.criticality := io.criticality
  // val data_mod = Module(new DataModification(m, cohSpec))
  data_mod.io.pipe_in <> pipe1.io.deq
  // we need this as the coherence response is delayed by 1 cycle
  data_mod.io.pipe_in.bits.coh_resp := tag_check.io.coh_resp
  data_mod.io.time := io.time
  data_mod.io.id := io.id
  data_mod.io.mshr := mshr
  data_mod.io.mshrValid := mshrValid
  val pipe2 = Module(new Queue(new PipeData(m, cohSpec), 1, pipe = true))
  data_mod.io.pipe_out <> pipe2.io.enq
  val writeback = Module(new Writeback(m, cohSpec, id))

  arbitrate.io.readingPWBs := writeback.io.bus_request_channel.fire() || writeback.io.dedicated_bus_request_channel.fire()

  writeback.io.id := id.U
  writeback.io.pipe_in <> pipe2.io.deq
  writeback.io.time := io.time
  writeback.io.busy := busy
  writeback.io.criticality := io.criticality

  for { i <- 0 until cacheParams.nWays } { tag_check.io.tags(i) := tags(i)._2.io.dout }
  tag_check.io.tag_valid := tag_valid
  tag_check.io.pendingMemValid := pendingMemValid
  tag_check.io.pendingMem := pendingMem
  tag_check.io.mshrValid := mshrValid

  replayBufferBuffer.io.enq <> replayBuffer.io.deq
  replayBufferBuffer.io.enq.valid := replayBuffer.io.deq.valid && writeback.io.releaseReplay
  replayBuffer.io.deq.ready := replayBufferBuffer.io.enq.ready && writeback.io.releaseReplay
  rb_fire := replayBufferBuffer.io.deq.valid && replayBufferBuffer.io.deq.ready
  dontTouch(rb_fire)
  io.rb_fire := rb_fire
  /**
    * Set sink
    */
  def sink: Bool = writeback.io.sink
  when(io.core.request_channel.fire() || io.snoop.request_channel.fire() || /* io.bus.response_channel.fire() */
    respCommandQueue.io.deq.fire() || replayBufferBuffer.io.deq.fire() || dedicatedReqChannel.io.deq.fire()) {
    busy := true.B
    printf(p"[CC${io.id}] started working, busy\n")
    printf("[CC%d] hasPendingMem: %b, hasPendingMSHR: %b\n", io.id, hasPendingMem, hasMSHR)
    printf(p"[CC${io.id}] pendingMemValid: ${pendingMemValid}, MSHRs: ${mshrValid}\n")

  }.elsewhen(sink) {
    busy := false.B
    printf(p"[CC${io.id}] done\n")
  }

  val isPipeIdle = pipe0.io.count === 0.U && pipe1.io.count === 0.U && pipe2.io.count === 0.U
  when(!isPipeIdle) {
    printf(p"[CC${io.id}] PipeLineStatus: ${pipe0.io.count}, ${pipe1.io.count}, ${pipe2.io.count}\n")
  }
  when(dataBufferCounter =/= 0.U) {
    printf(p"[CC${io.id}] dataBufferCounter: ${dataBufferCounter}, ${dataBeats.U}\n")
  }

  for { i <- 0 until cacheParams.nWays } {
    // tag control from writing back
    tags(i)._2.io.din := writeback.io.tag_array_ctrl.wtag
    tags(i)._2.io.waddr := writeback.io.tag_array_ctrl.waddr
    tags(i)._2.io.wen := false.B
    when(writeback.io.tag_array_ctrl.wway === i.U) {
      when(writeback.io.tag_array_ctrl.insert) {
        tags(i)._2.io.wen := true.B
        tags(i)._1(writeback.io.tag_array_ctrl.waddr) := true.B
        printf(p">>> Inserting to Tag(way=${i.U}, addr=${writeback.io.tag_array_ctrl.waddr}): \n")
        utils.printbundle(tags(i)._2.io.din)
        printf("\n")
      }.elsewhen(writeback.io.tag_array_ctrl.remove) {
        tags(i)._2.io.wen := false.B
        tags(i)._1(writeback.io.tag_array_ctrl.waddr) := false.B
        printf(p">>> Removing to Tag(${i.U}): \n")
        utils.printbundle(tags(i)._2.io.din)
        printf("\n")
      }.elsewhen(writeback.io.tag_array_ctrl.wen/* update */) {
        tags(i)._2.io.wen := true.B
        assert(tags(i)._1(writeback.io.tag_array_ctrl.waddr), "update must assume presence of tag")
        printf(p">>> Updating to Tag(${i.U}): \n")
        utils.printbundle(tags(i)._2.io.din)
        printf("\n")
      }
    }
    // tags(i)._2.io.wen := writeback.io.tag_array_ctrl.wen && writeback.io.tag_array_ctrl.wway === i.U

    tags(i)._2.io.ren := arbitrate.io.readTag
    tags(i)._2.io.raddr := arbitrate.io.readSet

    when(pipe0.io.enq.fire()) {
      tag_valid(i) := tags(i)._1(arbitrate.io.readSet)
    }

    data(i).io.wen := writeback.io.data_array.wen && writeback.io.data_array.wway === i.U
    data(i).io.waddr := writeback.io.data_array.waddr
    data(i).io.din := writeback.io.data_array.wdata

    data(i).io.ren := arbitrate.io.readData
    data(i).io.raddr := arbitrate.io.readSet

    tag_check.io.data(i) := data(i).io.dout.asTypeOf(cacheParams.genCacheLineBytes)
    when(data(i).io.wen) {
      printf(p">>> DataArray Writing to ${data(i).io.waddr}, data:${Hexadecimal(data(i).io.din)}\n")
    }
  }
  when(sink) {
    printf(p"[CC${io.id}]>>> ReplayBuffer: ${replayBuffer.io.count}\n")
    printf(p"[CC${io.id}]>>> ReplayBufferBuffer: ${replayBufferBuffer.io.count} EnqV: ${replayBufferBuffer.io.enq.valid }EnqR: ${replayBufferBuffer.io.enq.ready}\n")
  }

  val core_response_channel = Module(new Queue(genCacheResp, 4, pipe=true))
  val snoop_response_channel  = Module(new Queue(m.getGenSnoopResp, entries = 1, pipe = true))
  io.snoop.response_channel <> snoop_response_channel.io.deq
  io.bus.request_channel <> writeback.io.bus_request_channel
  io.core.response_channel <> core_response_channel.io.deq
  io.core.response_channel.bits.latency := latencyCounterReg
  core_response_channel.io.enq <> data_mod.io.core_response
  replayBuffer.io.enq <> data_mod.io.replayBuffer
  writeback.io.snoop_response_channel <> snoop_response_channel.io.enq
  snoop_response_channel.io.deq <> io.snoop.response_channel

  arbitrate.io.readBufferDone := dataBufferDone
  readDataBuffer := arbitrate.io.readBufferRead
  arbitrate.io.readBuffer := dataBuffer.asTypeOf(cacheParams.genCacheLine)
  io.bus.dataq <> writeback.io.data_out

  // pendingMemWriteLogic
  when(writeback.io.pending_mem_ctrl.insert) {
    pendingMem(writeback.io.pending_mem_ctrl.waddr) := writeback.io.pending_mem_ctrl.wdata
    pendingMemValid(writeback.io.pending_mem_ctrl.waddr) := true.B
  }.elsewhen(writeback.io.pending_mem_ctrl.remove) {
    pendingMemValid(writeback.io.pending_mem_ctrl.waddr) := false.B
  }.elsewhen(writeback.io.pending_mem_ctrl.wen) {
    pendingMem(writeback.io.pending_mem_ctrl.waddr) := writeback.io.pending_mem_ctrl.wdata
  }
  // MSHR is checked at the data modification stage
  when(data_mod.io.mshr_ctrl.insert) {
    mshr(data_mod.io.mshr_ctrl.waddr) := data_mod.io.mshr_ctrl.wdata
    mshrValid(data_mod.io.mshr_ctrl.waddr) := true.B
  }.elsewhen(data_mod.io.mshr_ctrl.remove) {
    mshrValid(data_mod.io.mshr_ctrl.waddr) := false.B
  }.elsewhen(data_mod.io.mshr_ctrl.wen) {
    // this should not happen...we don't need to UPDATE (only insert/remove)
    assert(false.B)
    mshr(data_mod.io.mshr_ctrl.waddr) := data_mod.io.mshr_ctrl.wdata
  }
  io.query_coverage <> tag_check.io.query_coverage

  m.getDataBusConf match {
      // In this case it is forced to broadcast the PutM message
    case SharedEverything => {
      writeback.io.dedicated_bus_request_channel.ready := false.B
      io.bus.dedicated_bus_request_channel.valid := false.B
      io.bus.dedicated_bus_request_channel.bits := 0.U.asTypeOf(chiselTypeOf(io.bus.dedicated_bus_request_channel.bits))
    }

    case DedicatedDataBusTwoWay | DedicatedDataBusOneWay => {
      writeback.io.dedicated_bus_request_channel <> io.bus.dedicated_bus_request_channel
    }
  }
}

object PipelinedCache extends App with withCompilerOptionParser  {
  implicit override val margs: List[String] = args.toList
  println(getOptions)
  println(getRemainingOptions.mkString(" "))
  val nCore = getCoreCount
  val depth = getCacheDepth
  val lineSize = 64
  val addrWidth = 64
  val interfaceAddrWidth = 64
  val dataWidth = 64
  val slotWidth = 128
  val busDataWidth = 64
  val masterCount = getMasterCount
  val cacheParams = SimpleCacheParams(depth, 4, lineSize * 8, addrWidth)
  val memorySystemParams = MemorySystemParams(
    addrWidth = addrWidth,
    interfaceAddrWidth = interfaceAddrWidth,
    dataWidth = dataWidth,
    slotWidth = slotWidth,
    busDataWidth = busDataWidth,
    busRequestType = new RequestType {},
    masterCount = masterCount,
    CohS = getCohS /* new PMESI {} */,
    cacheParams = cacheParams,
    getL1CoherenceTable /* { new PMESICoherenceTable() } */,
    getLLCCoherenceTable /* { new PMESILLCCoherenceTable(masterCount) } */,
    withCriticality = false,
    outOfSlotResponse = false
  )
  val XLEN = 64
  val fetchWidth = 32
  val isaParam = new RISCVParam(XLEN = XLEN,
    Embedded = false,
    Atomic = true,
    Multiplication = false,
    Compressed = false,
    SingleFloatingPoint = false,
    DoubleFloatingPoint = false)

  val coreParam = CoreParam(fetchWidth = fetchWidth,
    isaParam = isaParam,
    iCacheReqDepth = 1,
    iCacheRespDepth = 1,
    resetRegisterAddress =  0x80000000L,
    initPCRegisterAddress = 0x80003000L,
    baseAddrAddress = 0x80001000L,
    coreID = 0, // Note this is just a placeholder, internally, coreID will be adjusted
    withAXIMemoryInterface = true,
    nCore = nCore)
  val coherenceProtocol = PMSISpec()
  coherenceProtocol.generatePrivateCacheTableFile(None)
  chisel3.Driver.execute(getRemainingOptions, () => new PipelinedCache(coreParam, memorySystemParams, 0, PMSISpec()))
}
