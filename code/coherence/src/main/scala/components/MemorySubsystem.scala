
package components


import chisel3._
import chisel3.internal.naming.chiselName
import chisel3.util._
import coherences.{PMESI, PMESILLC}
import components.arbiters.{HasChoice, WTDMArbiter}
import param.CoreParam
import params.{MemorySystemParams, SimpleCacheParams}


@chiselName
class MemorySubsystem(coreParam: CoreParam, memorySystemParams: MemorySystemParams, genErrorMessage: ErrorMessage
                     ) extends Module {
  val dataWidth: Int = memorySystemParams.dataWidth
  val masterCount: Int = memorySystemParams.masterCount
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

    val ordered_point = Output(
      new Bundle {
        val req = Output(genBusReq)
        val valid = Output(Bool())
        val ready = Output(Bool())
      }
    )
  })


  val private_caches = for { i <- 0 until masterCount } yield {
    val cache = Module(new CacheController(coreParam, memorySystemParams, i))
    cache
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

  val response_concats = for { i <- 0 until masterCount } yield {
    val beats_per_line = memorySystemParams.cacheParams.lineWidth / memorySystemParams.busDataWidth
    val response_concat = Module(new CommandDataConcat(genBusRespCommand, UInt(busDataWidth.W),
      genBusResp, beats_per_line) {
      override def getFlitsToReceive(): UInt = {
        (memorySystemParams.cacheParams.lineWidth / memorySystemParams.busDataWidth).U
      }

      override def concat(cmd: MemRespCommand, data: Vec[UInt]): MemResp = {
        val q = Wire(genRes)
        q.ack := cmd.ack
        q.is_edata := cmd.is_edata
        q.data := Cat(for{i <- (0 until beats_per_line).reverse} yield { data(i) })
        q
      }
    })
    response_concat
  }

  val bus = Module(
    new SnoopyBus(masterCount,
      genBusReq,
      genBusRespCommand,
      genSnoopReq,
      genSnoopResp,
      false,
      memorySystemParams.getDataBusConf,
      memorySystemParams.translateGetS) {
    protected override lazy val arbiter: LockingArbiterLike[MemReqCommand] with HasChoice = Module(
      /*
      new RRArbiter(new MemReq(addrWidth,dataWidth, reqTypeWidth, masterCount), masterCount) with HasChoice {
        override def getChoice: UInt = choice
      }
       */
      new WTDMArbiter(genBusReq, masterCount, slotWidth,
        List.fill(masterCount)(1))
    )

    override def reqToSnoop(i: Int): Unit= {
      io.snoop.request_channel(i).bits.address := slot_owner_req_reg.address
      io.snoop.request_channel(i).bits.req_type:= slot_owner_req_reg.req_type
      io.snoop.request_channel(i).bits.requester_id := slot_owner_req_reg.requester_id
      io.snoop.request_channel(i).bits.criticality := slot_owner_req_reg.criticality
    }

    override def getReqWB(i: Int): UInt = io.controller.request_channel.in(i).bits.req_wb
  })

  val memory_controller = Module(new MemoryController(
    idWidth,
    memorySystemParams,
    new PMESILLC {},
    genErrorMessage))

  io.ordered_point.req := bus.io.controller.request_channel.out.bits
  io.ordered_point.valid := bus.io.controller.request_channel.out.valid
  io.ordered_point.ready := bus.io.controller.request_channel.out.ready


  for { i <- 0 until masterCount } {
    private_caches(i).io.core.request_channel <> io.core.request_channel(i)
    private_caches(i).io.core.response_channel <> io.core.response_channel(i)

    private_caches(i).io.bus.request_channel <> bus.io.controller.request_channel.in(i)
    response_concats(i).io.in.command <> bus.io.controller.response_channel.out(i)
    response_concats(i).io.in.data <> memory_controller.io.bus.response_data_channel(i)
    private_caches(i).io.bus.response_channel <> response_concats(i).io.out

    private_caches(i).io.snoop.request_channel <> bus.io.snoop.request_channel(i)
    private_caches(i).io.snoop.response_channel <> bus.io.snoop.response_channel(i)


    private_caches(i).io.id := i.U

  }

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


}
