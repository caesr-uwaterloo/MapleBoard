
package components

import chisel3._
import chisel3.internal.naming.chiselName
import chisel3.util._
import chisel3.util.experimental.BoringUtils
import coherences.PMESILLC
import components.arbiters.{AtomicArbiter, CARPArbiter, HasChoice, WTDMArbiter}
import param.CoreParam
import params.{CoherenceSpec, MemorySystemParams, SimpleCacheParams}

class DataBusIO(private val memorySystemParams: MemorySystemParams) extends Bundle {
  val data = Output(UInt(memorySystemParams.busDataWidth.W))
  val valid = Output(Bool())
  val ready = Output(Bool())
}

@chiselName
class PipelinedMemorySubsystem[S <: Data, M <: Data, B <: Data]
(
  coreParam: CoreParam, memorySystemParams: MemorySystemParams, genErrorMessage: ErrorMessage,
  coherenceSpec: CoherenceSpec[S, M, B],
  xtraCaches: Int = 0
) extends Module {

  val dataWidth: Int = memorySystemParams.dataWidth
  val masterCount: Int = memorySystemParams.masterCount + xtraCaches
  val slotWidth : Int = memorySystemParams.slotWidth
  val busDataWidth: Int = memorySystemParams.busDataWidth
  val cacheParams: SimpleCacheParams = memorySystemParams.cacheParams
  val genCacheReq: CacheReq = memorySystemParams.getGenCacheReq
  val genCacheResp: CacheResp = memorySystemParams.getGenCacheResp
  val genDebugCacheLine: DebugCacheline = memorySystemParams.getGenDebugCacheline
  val genDramReq: DRAMReq = memorySystemParams.getGenDramReq
  val genDramResp: DRAMResp = memorySystemParams.getGenDramResp

  val idWidth = log2Ceil(masterCount) + 1 // plus 1 for potential extensibility for LLC as a command bus master

  val genBusReq = new MemReqCommand(cacheParams.addrWidth, RequestType.getWidth, masterCount)
  val genBusResp = new MemResp(cacheParams.lineWidth)
  val genBusRespCommand = new MemRespCommand()
  val genSnoopReq = new SnoopReq(cacheParams.addrWidth, RequestType.getWidth, masterCount)
  val genSnoopResp = new SnoopResp(cacheParams.lineWidth)

  val io = IO(new Bundle {
    val core = new Bundle {
      val request_channel = Vec(masterCount, Flipped(Decoupled(genCacheReq)))
      val response_channel = Vec(masterCount, Decoupled(genCacheResp))
    }
    val dram = new Bundle {
      val request_channel = Decoupled(genDramReq)
      val response_channel = Flipped(Decoupled(genDramResp))
    }


    val err = Output(genErrorMessage)

    val bus_req_addr = Output(Vec(masterCount, UInt(32.W)))

    val bus_llc_req_addr = Output(UInt(32.W))

    val latency = Output(Vec(masterCount, UInt(64.W)))

    val ordered_point = new Bundle {
      val req = Output(genBusReq)
      val valid = Output(Bool())
      val ready = Output(Bool())
    }

    val query_coverage = Vec(masterCount, Valid(UInt(coherenceSpec.getGenCohQuery.getWidth.W)))

    val data_core_to_mem = Vec(masterCount,
      new DataBusIO(memorySystemParams)
    )
    val data_mem_to_core = Vec(masterCount,
      new DataBusIO(memorySystemParams)
    )
    val rb_fire = Output(Vec(masterCount, Bool()))
  })


  val (counter, _) = Counter(true.B, 128)
  // printf("==== Clock Cycle (%d) Started ====\n", counter)

  val private_caches = for { i <- 0 until masterCount } yield {
    val cache = Module(new PipelinedCache(coreParam, memorySystemParams, i, coherenceSpec))
    cache.io.time := counter
    io.query_coverage(i).bits := cache.io.query_coverage.bits.asUInt
    io.query_coverage(i).valid := cache.io.query_coverage.valid
    cache
  }
  for { i <- 0 until masterCount } {
    io.rb_fire(i) := private_caches(i).io.rb_fire
  }

  val global_counter = RegInit(0.U(64.W))
  global_counter := global_counter + 1.U

  val lantency = for { i <- 0 until masterCount } yield {
    val lat = RegInit(0.U(64.W))
    val start = RegInit(0.U(64.W))
    val current_latency = global_counter - start
    when(io.core.request_channel(i).fire()) {
      start := global_counter
    }
    when(io.core.response_channel(i).fire()) {
      when(current_latency > lat) {
        lat := current_latency
      }
    }
    io.latency(i) := lat
  }

  val memory_controller = Module(new PipelinedMemoryController(
    idWidth,
    memorySystemParams.copy(masterCount = masterCount),
    coherenceSpec,
    genErrorMessage))

  val bus = Module(
    new SnoopyBus(masterCount,
      genBusReq,
      genBusRespCommand,
      genSnoopReq,
      genSnoopResp,
      memorySystemParams.outOfSlotResponse,
      memorySystemParams.getDataBusConf,
      memorySystemParams.translateGetS,
      memorySystemParams.useAtomicBus,
      memorySystemParams.isConventionalProtocol
    ) {

      lazy val slotTimerCounterInst = Counter(true.B, slotWidth)
      lazy val slotCounterInst = slotTimerCounterInst._1
      lazy val slotCounterWrapInst = slotTimerCounterInst._2
      lazy val slotCounterWrapReg = Reg(Bool())
      slotCounterWrapReg := slotCounterWrapInst

      override def switchingSame: Bool = slotCounterWrapReg
    protected override lazy val arbiter: LockingArbiterLike[MemReqCommand] with HasChoice = Module(
      /*
      new RRArbiter(new MemReq(addrWidth,dataWidth, reqTypeWidth, masterCount), masterCount) with HasChoice {
        override def getChoice: UInt = choice
      }
       */
      if(!atomicBus) {
        if(memorySystemParams.useCARPArbiter) {
          // currently only support non crit core to be high ids
          // like this
          // [0, 1, 2, 3, 4] [5, 6, 7]
          val RRCores = (0 until masterCount).filter(i => memorySystemParams.getCritFromID(i).litValue() == 5).toList
          val CritCores = RRCores.min
          val res = new CARPArbiter(genBusReq, masterCount, slotWidth, List.fill(CritCores)(1), RRWidth = slotWidth + 20,
            RRCores = RRCores) {
            val state = WireInit(phase)
            val lastRRC = WireInit(lastRRCounter)
            dontTouch(lastRRC)
            dontTouch(state)
          }
          res
        } else if(memorySystemParams.isConventionalProtocol) {
          for { i <- 0 until masterCount } {
            BoringUtils.addSource(memory_controller.io.bus.response_channel(i).valid, s"ResponseValid${i}", disableDedup = true)
            BoringUtils.addSource(memory_controller.io.bus.response_channel(i).ready, s"ResponseReady${i}", disableDedup = true)
          }
          val res = new components.arbiters.FCFSArbiterSharedBus(genBusReq, masterCount, slotWidth)
          res
        } else {
          new WTDMArbiter(genBusReq, masterCount, slotWidth,
            List.fill(masterCount)(1))
        }
      } else {
        // augment the arbiter
        val res = new AtomicArbiter(genBusReq, masterCount, slotWidth,
          List.fill(masterCount)(1)) {
          lazy val phase1IsMatchedWBInst = Wire(Vec(masterCount, Bool()))
          val phase0AddressInst: UInt = RegInit(0.U(memorySystemParams.addrWidth.W))
          lazy val phase0NeedsWBInst: Bool = Reg(Bool())
          lazy val grantMaskInst = Wire(Vec(masterCount, Bool()))
          lazy val fire = Wire(Vec(masterCount, Bool()))
          lazy val slotBeginningInst = Wire(Bool())
          for { i <- 0 until masterCount } {
            grantMaskInst(i) := grantMask(i)
            if(memorySystemParams.useAtomicBusModified) {
              BoringUtils.addSource(phase, s"CC${i}Phase", true)
            }
          }
          slotBeginningInst := slotBeginning
          /*
          for { i <- 0 until masterCount } {
            grantMaskInst(i) := slotOwner === i.U && slotBeginning && phase === 0.U ||
              phase0NeedsWB && phase1IsMatchedWB(i) && slotBeginning && phase === 1.U
          } */
          dontTouch(grantMaskInst)
          dontTouch(fire)
          // val phase1MatchedInst: UInt = Wire(UInt(log2Ceil(masterCount).W))
          override def phase0NeedsWB: Bool = phase0NeedsWBInst
          override def phase1IsMatchedWB: Vec[Bool] = phase1IsMatchedWBInst
          // override def phase1Matched: UInt = phase1MatchedInst
          for { i <- 0 until masterCount } {
            fire(i) := false.B
            when(io.in(i).valid && io.in(i).ready && !phase0NeedsWBInst && slotBeginningInst) {
              fire(i) := true.B
              phase0AddressInst := io.out.bits.address
            }
            when(!io.in(i).fire() && fire(i)) {
              assert(false.B)
            }
          }
          for {i <- 0 until masterCount} {

            when(io.in(i).ready || (phase === 1.U && !slotBeginning)) {
              phase0NeedsWBInst := false.B
            }
            when(io.in(i).fire()) {
              phase0NeedsWBInst := phase === 0.U && (io.in(i).bits.req_wb === 0.U)
            }
            // note: the io(i) must be valid, however, if it is not valid, even if it is granted, nothing will happen
            phase1IsMatchedWBInst(i) := phase === 1.U && io.in(i).valid && io.in(i).bits.req_wb === 1.U && io.in(i).bits.address === phase0AddressInst
            when(phase1IsMatchedWBInst(i)) {
              phase1Matched := i.U
            }
          }
          assert(PopCount(VecInit(for { i <- 0 until masterCount } yield io.in(i).fire())) <= 1.U)
          when(phase === 1.U && !phase0NeedsWB) {
          }
        }
        res
      }
    )

    override def reqToSnoop(i: Int): Unit= {
      io.snoop.request_channel(i).bits.address := slot_owner_req_reg.address
      io.snoop.request_channel(i).bits.req_type:= slot_owner_req_reg.req_type
      io.snoop.request_channel(i).bits.requester_id := slot_owner_req_reg.requester_id
      io.snoop.request_channel(i).bits.criticality := slot_owner_req_reg.criticality
    }

    override def getReqWB(i: Int): UInt = io.controller.request_channel.in(i).bits.req_wb
  })


  io.ordered_point.req := bus.io.controller.request_channel.out.bits
  io.ordered_point.valid := bus.io.controller.request_channel.out.valid
  io.ordered_point.ready := bus.io.controller.request_channel.out.ready


  for { i <- 0 until masterCount } {
    private_caches(i).io.criticality := memorySystemParams.getCritFromID(i)
    private_caches(i).io.core.request_channel <> io.core.request_channel(i)
    private_caches(i).io.core.response_channel <> io.core.response_channel(i)

    private_caches(i).io.bus.request_channel <> bus.io.controller.request_channel.in(i)
    private_caches(i).io.bus.response_channel <> bus.io.controller.response_channel.out(i)
    private_caches(i).io.bus.dataq_in <> memory_controller.io.bus.response_data_channel(i)

    private_caches(i).io.snoop.request_channel <> bus.io.snoop.request_channel(i)
    private_caches(i).io.snoop.response_channel <> bus.io.snoop.response_channel(i)

    private_caches(i).io.id := i.U

    io.data_mem_to_core(i).valid := private_caches(i).io.bus.dataq_in.valid
    io.data_mem_to_core(i).ready := private_caches(i).io.bus.dataq_in.ready
    io.data_mem_to_core(i).data := private_caches(i).io.bus.dataq_in.bits

    io.data_core_to_mem(i).valid := private_caches(i).io.bus.dataq.valid
    io.data_core_to_mem(i).ready := private_caches(i).io.bus.dataq.ready
    io.data_core_to_mem(i).data := private_caches(i).io.bus.dataq.bits

    memory_controller.io.bus.dedicated_request_channel(i) <> private_caches(i).io.bus.dedicated_bus_request_channel
  }

  memory_controller.io.bus.slotOwner := bus.io.slotOwner
  memory_controller.io.bus.slotStart := bus.io.slotStart

  bus.io.controller.request_channel.out <> memory_controller.io.bus.request_channel
  for { i <- 0 until masterCount } {
    bus.io.controller.response_channel.in(i) <> memory_controller.io.bus.response_channel(i)
  }

  for { i <- 0 until masterCount } {
    private_caches(i).io.bus.dataq <> memory_controller.io.bus.data_channel(i)
    // private_caches(i).io.bus.response_data_channel <> memory_controller.io.bus.response_data_channel(i)

  }

  memory_controller.io.dram.request_channel <> io.dram.request_channel
  memory_controller.io.dram.response_channel <> io.dram.response_channel

  io.err <> memory_controller.io.err

  val bus_req_valid = WireInit(Cat(for { i <- (0 until masterCount).reverse } yield {
    private_caches(i).io.bus.request_channel.valid
  }))
  // exposeTop(bus_req_valid)
  val bus_req_ready = WireInit(Cat(for { i <- (0 until masterCount).reverse } yield {
    bus.io.controller.request_channel.in(i).ready
  }))

  val broadcasted = Wire(UInt(masterCount.W))
  broadcasted := bus_req_valid & bus_req_ready

  // exposeTop(bus_req_ready)
  val bus_resp_valid = WireInit(Cat(for { i <- (0 until masterCount).reverse } yield {
    bus.io.controller.response_channel.out(i).valid
  }))
  // exposeTop(bus_resp_valid)
  val bus_resp_ready = WireInit(Cat(for { i <- (0 until masterCount).reverse } yield {
    private_caches(i).io.bus.response_channel.ready
  }))
  // exposeTop(bus_resp_valid)

  val bus_llc_req_valid = WireInit(bus.io.controller.request_channel.out.valid)
  // exposeTop(bus_llc_req_valid)
  val bus_llc_req_ready = WireInit(bus.io.controller.request_channel.out.ready)
  // exposeTop(bus_llc_req_ready)
  val bus_llc_resp_valid = WireInit(Cat(for { i <- (0 until masterCount).reverse } yield {
    memory_controller.io.bus.response_channel(i).valid
  }))
  // exposeTop(bus_llc_resp_valid)
  val bus_llc_resp_ready = WireInit(Cat(for { i <- (0 until masterCount).reverse } yield {
    bus.io.controller.response_channel.in(i).ready
  }))
  // exposeTop(bus_llc_resp_ready)

  for{ i <- 0 until masterCount } {
    // io.bus_req_valid(i) := private_caches(i).io.bus.request_channel.valid
    // io.bus_req_ready(i) := bus.io.controller.request_channel.in(i).ready
    io.bus_req_addr(i) := bus.io.controller.request_channel.in(i).bits.address

    //io.bus_resp_valid(i) := bus.io.controller.response_channel.out(i).valid
    //io.bus_resp_ready(i) := private_caches(i).io.bus.response_channel.ready

    //io.bus_llc_req_valid := bus.io.controller.request_channel.out.valid
    //io.bus_llc_req_ready := bus.io.controller.request_channel.out.ready
    io.bus_llc_req_addr := bus.io.controller.request_channel.out.bits.address
    //io.bus_llc_resp_valid(i) := memory_controller.io.bus.response_channel(i).valid // bus.io.controller.response_channel.in(2).valid
    //io.bus_llc_resp_ready(i) := bus.io.controller.response_channel.in(i).ready

  }

  //io.master_state := bus.io.master_state
  //io.snoop_state  := bus.io.snoop_state
  //io.slave_state  := bus.io.slave_state
  val req_addr = WireInit(VecInit.tabulate(masterCount)(i => io.core.request_channel(i).bits.address))
  val req_data = WireInit(VecInit.tabulate(masterCount)(i => io.core.request_channel(i).bits.data))
  val req_valid = WireInit(VecInit.tabulate(masterCount)(i => io.core.request_channel(i).valid))
  val req_ready = WireInit(VecInit.tabulate(masterCount)(i => io.core.request_channel(i).ready))
  val resp_data =  WireInit(VecInit.tabulate(masterCount)(i => io.core.response_channel(i).bits.data))
  val resp_valid = WireInit(VecInit.tabulate(masterCount)(i => io.core.response_channel(i).valid))
  val resp_ready = WireInit(VecInit.tabulate(masterCount)(i => io.core.response_channel(i).ready))
  // exposeTop(req_addr)
  // exposeTop(req_valid)
  // exposeTop(req_ready)
  // exposeTop(resp_data)
  // exposeTop(resp_valid)
  // exposeTop(resp_ready)
  for { i <- 0 until masterCount } {
    memorySystemParams.getDataBusConf match {
      case DedicatedDataBusOneWay => {
        // prevent the data bus from being occupied by response
        // prioritize request
        when(private_caches(i).io.bus.dedicated_bus_request_channel.fire() || private_caches(i).io.bus.dataq.fire()) {
          memory_controller.io.bus.response_channel(i).ready := false.B
          private_caches(i).io.bus.response_channel.valid := false.B

          private_caches(i).io.bus.dataq_in.valid := false.B
          memory_controller.io.bus.response_data_channel(i).ready := false.B
        }
      }
      case _ => {}
    }
  }
}
