
package coherences

import chisel3._
import chisel3.util.Decoupled
import chiseltest._
import org.scalatest._
import chiseltest.experimental.TestOptionBuilder._
import chiseltest.internal.VerilatorBackendAnnotation
import chiseltest.legacy.backends.verilator.VerilatorFlags
import components.{AMOALUOP, CacheReq, CacheResp, DRAMReq, DRAMResp, ErrorMessage, MemReq, MemReqCommand, MemResp, MemRespCommand, MemToAXI4, MemoryController, MemorySubsystem, RequestType}
import _root_.core.AXIMemory
import fixtures.TargetPlatformCARP
import param.CoreParam
import params.{MemorySystemParams, SimpleCacheParams}
import chisel3.experimental.BundleLiterals._
import _root_.core.{AMOOP, MemoryRequestType}
import chisel3.util.experimental.BoringUtils

import scala.collection.mutable.ArrayBuffer
class BareMemorySubsystemWithPrivateCaches(coreParam: CoreParam, memorySystemParams: MemorySystemParams)
  extends Module {
  val masterCount: Int = memorySystemParams.masterCount
  val genCacheReq: CacheReq = memorySystemParams.getGenCacheReq
  val genCacheResp: CacheResp = memorySystemParams.getGenCacheResp
  val genErrorMessage = new ErrorMessage()
  val io = IO(new Bundle {
    val core = new Bundle {
      val request_channel = Vec(masterCount, Flipped(Decoupled(genCacheReq)))
      val response_channel = Vec(masterCount, Decoupled(genCacheResp))
    }
    val err = Output(genErrorMessage)
    val latency = Output(Vec(masterCount, UInt(64.W)))
    val cancelledInLLC = Output(UInt(32.W))
    val cancelledInCache = Output(Vec(masterCount, UInt(32.W)))
    val slotOwner = Output(UInt(5.W))
    val broadcasted = Output(Vec(masterCount, Bool()))
  })
  // we need to compose the memory subsystem
  val memory = Module(new MemorySubsystem(coreParam, memorySystemParams, genErrorMessage))
  val cg = Module(new MemToAXI4(memorySystemParams))
  val mem = Module(new fixtures.AXIMemoryBlank(coreParam))
  io.latency := memory.io.latency
  io.cancelledInLLC := 0.U
  io.slotOwner := 0.U

  BoringUtils.bore(memory.memory_controller.pr_lut_new.cancelledCount, Seq(io.cancelledInLLC))
  for { i <- 0 until masterCount } {
    val cancelledInCache = Wire(UInt(32.W))
    io.cancelledInCache(i) := 0.U
    cancelledInCache := 0.U
    BoringUtils.bore(memory.private_caches(i).wb_fifo_inst_lo_crit.cancelledCount, Seq(cancelledInCache))
    io.cancelledInCache(i) := cancelledInCache
  }
  BoringUtils.bore(memory.bus.last_tick_owner, Seq(io.slotOwner))
  val broadcasted = Wire(UInt(masterCount.W))
  broadcasted := 0.U
  BoringUtils.bore(memory.broadcasted, Seq(broadcasted))
  for { i <- 0 until masterCount } {
    io.broadcasted(i) := broadcasted(i)
  }

  for { i <- 0 until masterCount } {
    io.core.request_channel(i) <> memory.io.core.request_channel(i)
    io.core.response_channel(i) <> memory.io.core.response_channel(i)
  }
  io.err <> memory.io.err
  memory.io.dram.request_channel <> cg.io.dramReq
  memory.io.dram.response_channel <> cg.io.dramResp
  connectAXI()
  // we also need to stick in the memory...
  private def connectAXI(): Unit = {
    // AR Channel
    mem.io.m_axi.araddr  := cg.m_axi.araddr
    mem.io.m_axi.arid    := cg.m_axi.arid
    mem.io.m_axi.arsize  := cg.m_axi.arsize
    mem.io.m_axi.arlen   := cg.m_axi.arlen
    mem.io.m_axi.arburst := cg.m_axi.arburst
    mem.io.m_axi.arlock  := cg.m_axi.arlock
    mem.io.m_axi.arcache := cg.m_axi.arcache
    mem.io.m_axi.arprot  := cg.m_axi.arprot
    mem.io.m_axi.arvalid := cg.m_axi.arvalid
    cg.m_axi.arready     :=  mem.io.m_axi.arready

    mem.io.m_axi.rready := cg.m_axi.rready
    cg.m_axi.rvalid     := mem.io.m_axi.rvalid
    cg.m_axi.rlast      := mem.io.m_axi.rlast
    cg.m_axi.rdata      := mem.io.m_axi.rdata
    cg.m_axi.rresp      := mem.io.m_axi.rresp
    cg.m_axi.rid        := mem.io.m_axi.rid

    // write channel
    mem.io.m_axi.awaddr  := cg.m_axi.awaddr
    mem.io.m_axi.awid    := cg.m_axi.awid
    mem.io.m_axi.awsize  := cg.m_axi.awsize
    mem.io.m_axi.awlen   := cg.m_axi.awlen
    mem.io.m_axi.awburst := cg.m_axi.awburst
    mem.io.m_axi.awlock  := cg.m_axi.awlock
    mem.io.m_axi.awcache := cg.m_axi.awcache
    mem.io.m_axi.awprot  := cg.m_axi.awprot
    mem.io.m_axi.awvalid := cg.m_axi.awvalid
    cg.m_axi.awready     := mem.io.m_axi.awready

    mem.io.m_axi.wdata  := cg.m_axi.wdata
    mem.io.m_axi.wlast  := cg.m_axi.wlast
    mem.io.m_axi.wstrb  := cg.m_axi.wstrb
    mem.io.m_axi.wvalid := cg.m_axi.wvalid
    cg.m_axi.wready     := mem.io.m_axi.wready

    cg.m_axi.bid        := mem.io.m_axi.bid
    cg.m_axi.bresp      := mem.io.m_axi.bresp
    cg.m_axi.bvalid     := mem.io.m_axi.bvalid
    mem.io.m_axi.bready := cg.m_axi.bready
  }
}

class BareMemorySubsystemWithPMSILLC(coreParam: CoreParam, memorySystemParams: MemorySystemParams) extends Module {
  val masterCount: Int = memorySystemParams.masterCount
  val cacheParams: SimpleCacheParams = memorySystemParams.cacheParams
  val genMemReq: MemReq = memorySystemParams.getGenMemReq
  val genMemReqCommand: MemReqCommand = memorySystemParams.getGenMemReqCommand
  val genMemResp: MemResp = memorySystemParams.getGenMemResp
  val genMemRespCommand: MemRespCommand = memorySystemParams.getGenMemRespCommand
  val genDramReq: DRAMReq = memorySystemParams.getGenDramReq
  val genDramResp: DRAMResp = memorySystemParams.getGenDramResp
  val busDataWidth: Int = memorySystemParams.busDataWidth
  val beats_per_line: Int = memorySystemParams.cacheParams.lineWidth / memorySystemParams.busDataWidth
  val io = IO(new Bundle {
    val bus = new Bundle {
      val request_channel = Flipped(Decoupled(genMemReqCommand))
      val response_channel = Vec(masterCount, Decoupled(genMemRespCommand))
      val data_channel = Flipped(Vec(masterCount, Decoupled(UInt(busDataWidth.W))))
      val response_data_channel = Vec(masterCount, Decoupled(UInt(busDataWidth.W)))
    }
    val cancelledInLLC = Output(UInt(32.W))
  })

  val genErrorMessage = new ErrorMessage()
  val llc = Module(new MemoryController(5, memorySystemParams, new PMESILLC{}, genErrorMessage))
  val cg = Module(new MemToAXI4(memorySystemParams))
  val mem = Module(new fixtures.AXIMemoryBlank(coreParam))

  io.cancelledInLLC := 0.U
  BoringUtils.bore(llc.pr_lut_new.cancelledCount, Seq(io.cancelledInLLC))
  llc.io.bus.request_channel <> io.bus.request_channel
  llc.io.bus.response_channel <> io.bus.response_channel
  llc.io.bus.data_channel <> io.bus.data_channel
  llc.io.bus.response_data_channel <> io.bus.response_data_channel

  llc.io.dram.request_channel <> cg.io.dramReq
  llc.io.dram.response_channel <> cg.io.dramResp
  connectAXI()
  // we also need to stick in the memory...
  private def connectAXI(): Unit = {
    // AR Channel
    mem.io.m_axi.araddr  := cg.m_axi.araddr
    mem.io.m_axi.arid    := cg.m_axi.arid
    mem.io.m_axi.arsize  := cg.m_axi.arsize
    mem.io.m_axi.arlen   := cg.m_axi.arlen
    mem.io.m_axi.arburst := cg.m_axi.arburst
    mem.io.m_axi.arlock  := cg.m_axi.arlock
    mem.io.m_axi.arcache := cg.m_axi.arcache
    mem.io.m_axi.arprot  := cg.m_axi.arprot
    mem.io.m_axi.arvalid := cg.m_axi.arvalid
    cg.m_axi.arready     :=  mem.io.m_axi.arready

    mem.io.m_axi.rready := cg.m_axi.rready
    cg.m_axi.rvalid     := mem.io.m_axi.rvalid
    cg.m_axi.rlast      := mem.io.m_axi.rlast
    cg.m_axi.rdata      := mem.io.m_axi.rdata
    cg.m_axi.rresp      := mem.io.m_axi.rresp
    cg.m_axi.rid        := mem.io.m_axi.rid

    // write channel
    mem.io.m_axi.awaddr  := cg.m_axi.awaddr
    mem.io.m_axi.awid    := cg.m_axi.awid
    mem.io.m_axi.awsize  := cg.m_axi.awsize
    mem.io.m_axi.awlen   := cg.m_axi.awlen
    mem.io.m_axi.awburst := cg.m_axi.awburst
    mem.io.m_axi.awlock  := cg.m_axi.awlock
    mem.io.m_axi.awcache := cg.m_axi.awcache
    mem.io.m_axi.awprot  := cg.m_axi.awprot
    mem.io.m_axi.awvalid := cg.m_axi.awvalid
    cg.m_axi.awready     := mem.io.m_axi.awready

    mem.io.m_axi.wdata  := cg.m_axi.wdata
    mem.io.m_axi.wlast  := cg.m_axi.wlast
    mem.io.m_axi.wstrb  := cg.m_axi.wstrb
    mem.io.m_axi.wvalid := cg.m_axi.wvalid
    cg.m_axi.wready     := mem.io.m_axi.wready

    cg.m_axi.bid        := mem.io.m_axi.bid
    cg.m_axi.bresp      := mem.io.m_axi.bresp
    cg.m_axi.bvalid     := mem.io.m_axi.bvalid
    mem.io.m_axi.bready := cg.m_axi.bready
  }
}
class CARPArchitecturalChangesSpec extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior of "CARP arch changes"
  it should "remove writebacks for the non critical cores" in {
    val memParam = TargetPlatformCARP.memorySystemParams.copy(masterCount = 4)
    test(new BareMemorySubsystemWithPrivateCaches(
      TargetPlatformCARP.coreParam,
      memParam)).withAnnotations(Seq(VerilatorBackendAnnotation, VerilatorFlags(Seq("-DPRINTF_COND=0")), chiseltest.internal.WriteVcdAnnotation)) { c =>
      val masterCount = memParam.masterCount
      // initialize the requests
      // (Core, Latency)

      for{ i <- 0 until masterCount } {
        c.io.core.request_channel(i).initSource().setSourceClock(c.clock)
        c.io.core.response_channel(i).initSink().setSinkClock(c.clock)
      }
      // initialize first for serveral cycles
      c.clock.step(129)
      // now construct the requests
      // we need fine grained control over the tests
      val reqToPokeOwner = chiselTypeOf(c.io.core.request_channel(0).bits).Lit(
        _.address -> 0.U,
        _.amo_alu_op -> AMOOP.none,
        _.data -> "hdeadbeefdeadbeef".U,
        _.length -> 3.U,
        _.mem_type -> MemoryRequestType.write,
        _.is_amo -> false.B,
        _.flush  -> false.B,
        _.llcc_flush -> false.B,
        _.aq -> 0.U,
        _.rl -> 0.U)
      val respOwner = chiselTypeOf(c.io.core.response_channel(0).bits).Lit(
        _.address -> 0.U,
        _.data -> 0.U,
        _.length -> 3.U,
        _.mem_type -> MemoryRequestType.write
      )
      // This request is also useful for those NRT cores
      val reqToPokeSharer = chiselTypeOf(c.io.core.request_channel(0).bits).Lit(
        _.address -> 0.U,
        _.amo_alu_op -> AMOOP.none,
        _.data -> "hdeadbeefdeadbeef".U,
        _.length -> 3.U,
        _.mem_type -> MemoryRequestType.read,
        _.is_amo -> false.B,
        _.flush -> false.B,
        _.llcc_flush -> false.B,
        _.aq -> 0.U,
        _.rl -> 0.U
      )
      val respSharer = chiselTypeOf(c.io.core.response_channel(0).bits).Lit(
        _.address -> 0.U,
        _.data -> "hdeadbeefdeadbeef".U,
        _.length -> 3.U,
        _.mem_type -> MemoryRequestType.write
      )
      val nReq = 20
      val timeout = 20000
      val slot = 128
      c.clock.setTimeout(timeout)
      c.clock.step()
      // wait until the slot for last core
      while(c.io.slotOwner.peek().litValue() != 3) c.clock.step()
      c.clock.step()
      // General Schedule
      // Core 2: I -> IM_AD -> IM_D -> M
      // Core 3: I -> IS_D, Core 2: M -> MI_A for lo crit
      // Core 0: I -> IM_AD -> IM_D and cancels the writeback for Core 3
      while(c.io.slotOwner.peek().litValue() != 1) c.clock.step()
      println("[TEST] At Slot Owner 1")
      c.io.core.request_channel(2).enqueue(reqToPokeOwner)
      // wait until it is broadcasted
      while(!c.io.broadcasted(2).peek().litToBoolean) c.clock.step()
      println("[TEST] Request of 2 broadcasted")
      // now we can enqueue requests for 3 and 0
      fork {
        c.io.core.request_channel(3).enqueueNow(reqToPokeSharer)
      }.fork {
        c.io.core.request_channel(0).enqueueNow(reqToPokeOwner)
      }.join
      // 3 should be broadcasted before 0
      var core3bd = 0
      var core0bd = 0
      fork {
        while(!c.io.broadcasted(3).peek().litToBoolean) {
          c.clock.step()
          core3bd += 1
        }
        println("[TEST] Request of 3 broadcasted")
      }.fork {
        while(!c.io.broadcasted(0).peek().litToBoolean) {
          c.clock.step()
          core0bd += 1
        }
        println("[TEST] Request of 0 broadcasted")
      }.join

      assert(core3bd < core0bd, "Core 3 should broadcast before core 0")
      fork {
        for {i <- 0 until (slot * masterCount * masterCount * masterCount)} {
          // core 3's request should be cancelled
          c.io.core.response_channel(3).valid.expect(false.B)
        }
      }.fork {
        // core 1's request should be satisfied
        c.io.core.response_channel(0).expectDequeue(respOwner)
      }.joinAndStep(c.clock)

      // now check the counters
      assert(c.io.cancelledInCache(2).peek().litValue() == 1)
      assert(c.io.cancelledInLLC.peek().litValue() == 1)
    }
  }
  it should "cancel requests in LLC for low critical cores" in {
    val memParam = TargetPlatformCARP.memorySystemParams.copy(masterCount = 4)
    test(new BareMemorySubsystemWithPMSILLC (
      TargetPlatformCARP.coreParam,
      memParam)).withAnnotations(Seq(VerilatorBackendAnnotation/*, VerilatorFlags(Seq("-DPRINTF_COND=0")) */, chiseltest.internal.WriteVcdAnnotation)) { c =>
      val masterCount = memParam.masterCount
      c.io.bus.request_channel.initSource().setSourceClock(c.clock)
      for { i <- 0 until masterCount } {
        c.io.bus.data_channel(i).initSource().setSourceClock(c.clock)
        c.io.bus.response_data_channel(i).initSink().setSinkClock(c.clock)
        c.io.bus.response_channel(i).initSink().setSinkClock(c.clock)
      }
      // we first issue a request for getM in core 2
      c.io.bus.request_channel.enqueue(chiselTypeOf(c.io.bus.request_channel.bits).Lit(
        _.criticality -> 0.U,
        _.address -> 0.U,
        _.req_type -> RequestType.GETM.U,
        _.requester_id -> 2.U,
        _.req_wb -> 0.U,
        _.dirty -> 0.U
      ))
      // and now we wait for the response and the controller
      c.io.bus.response_channel(2).ready.poke(true.B)
      c.io.bus.response_data_channel(2).ready.poke(true.B)
      while(!c.io.bus.response_channel(2).valid.peek().litToBoolean) {
        c.clock.step()
      }
      while(!c.io.bus.request_channel.ready.peek().litToBoolean) {
        c.clock.step()
      }
      // we send a request for low crit request (core 3)
      c.io.bus.request_channel.enqueue(chiselTypeOf(c.io.bus.request_channel.bits).Lit(
        _.criticality -> 5.U,
        _.address -> 0.U,
        _.req_type -> RequestType.GETS.U,
        _.requester_id -> 3.U,
        _.req_wb -> 0.U,
        _.dirty -> 0.U
      ))
      // it should be in the pending req, now put another request from core 1
      c.io.bus.request_channel.enqueue(chiselTypeOf(c.io.bus.request_channel.bits).Lit(
        _.criticality -> 0.U,
        _.address -> 0.U,
        _.req_type -> RequestType.GETS.U,
        _.requester_id -> 1.U,
        _.req_wb -> 1.U,
        _.dirty -> 0.U
      ))
      // and we do putm from core 2
      c.io.bus.request_channel.enqueue(chiselTypeOf(c.io.bus.request_channel.bits).Lit(
        _.criticality -> 0.U,
        _.address -> 0.U,
        _.req_type -> RequestType.PUTM.U,
        _.requester_id -> 2.U,
        _.req_wb -> 1.U,
        _.dirty -> 1.U
      ))
      // some data
      c.io.bus.data_channel(2).enqueueSeq(Seq.fill(8) { 0.U })

      // now we should receive response for core 1 but not core 3
      while(!c.io.bus.response_channel(1).valid.peek().litToBoolean) {
        assert(!c.io.bus.response_channel(3).valid.peek().litToBoolean, "no crit request should be cancelled")
        c.clock.step()
      }

      // and we should see cancelled request
      c.io.cancelledInLLC.expect(1.U, "should have cancelled request")
    }
  }
  it should "allow response in LLC for low critical cores" in {
    val memParam = TargetPlatformCARP.memorySystemParams.copy(masterCount = 4)
    test(new BareMemorySubsystemWithPMSILLC (
      TargetPlatformCARP.coreParam,
      memParam)).withAnnotations(Seq(VerilatorBackendAnnotation/*, VerilatorFlags(Seq("-DPRINTF_COND=0")) */, chiseltest.internal.WriteVcdAnnotation)) { c =>
      val masterCount = memParam.masterCount
      c.io.bus.request_channel.initSource().setSourceClock(c.clock)
      for { i <- 0 until masterCount } {
        c.io.bus.data_channel(i).initSource().setSourceClock(c.clock)
        c.io.bus.response_data_channel(i).initSink().setSinkClock(c.clock)
        c.io.bus.response_channel(i).initSink().setSinkClock(c.clock)
      }
      // we first issue a request for getM in core 2
      c.io.bus.request_channel.enqueue(chiselTypeOf(c.io.bus.request_channel.bits).Lit(
        _.criticality -> 0.U,
        _.address -> 0.U,
        _.req_type -> RequestType.GETM.U,
        _.requester_id -> 2.U,
        _.req_wb -> 0.U,
        _.dirty -> 0.U
      ))
      // and now we wait for the response and the controller
      c.io.bus.response_channel(2).ready.poke(true.B)
      c.io.bus.response_data_channel(2).ready.poke(true.B)
      while(!c.io.bus.response_channel(2).valid.peek().litToBoolean) {
        c.clock.step()
      }
      while(!c.io.bus.request_channel.ready.peek().litToBoolean) {
        c.clock.step()
      }
      // we put hi crit req in side
      c.io.bus.request_channel.enqueue(chiselTypeOf(c.io.bus.request_channel.bits).Lit(
        _.criticality -> 0.U,
        _.address -> 0.U,
        _.req_type -> RequestType.GETS.U,
        _.requester_id -> 1.U,
        _.req_wb -> 1.U,
        _.dirty -> 0.U
      ))
      // we send a request for low crit request (core 3)
      c.io.bus.request_channel.enqueue(chiselTypeOf(c.io.bus.request_channel.bits).Lit(
        _.criticality -> 5.U,
        _.address -> 0.U,
        _.req_type -> RequestType.GETS.U,
        _.requester_id -> 3.U,
        _.req_wb -> 0.U,
        _.dirty -> 0.U
      ))

      // and we do putm from core 2
      c.io.bus.request_channel.enqueue(chiselTypeOf(c.io.bus.request_channel.bits).Lit(
        _.criticality -> 0.U,
        _.address -> 0.U,
        _.req_type -> RequestType.PUTM.U,
        _.requester_id -> 2.U,
        _.req_wb -> 1.U,
        _.dirty -> 1.U
      ))
      // some data
      c.io.bus.data_channel(2).enqueueSeq(Seq.fill(8) { 0.U })

      // now we should receive response for core 1 AND core 3
      while(!c.io.bus.response_channel(1).valid.peek().litToBoolean) {
        c.clock.step()
      }
      while(!c.io.bus.response_channel(3).valid.peek().litToBoolean) {
        c.clock.step()
      }

      // and we should see cancelled request
      c.io.cancelledInLLC.expect(0.U, "should not cancel low crit request")
    }
  }
  it should "see low crit core issue the same request multiple times" in {

  }
}

