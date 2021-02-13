
package utils

import chisel3._
import chisel3.util._
import components.{AXI4, CacheReq, CacheResp, ErrorMessage, MemToAXI4, MemorySubsystem, PipelinedMemorySubsystem}
import _root_.core.{AMOOP, Core, DCacheReq, DCacheResp, ICacheReq, ICacheResp, MemoryRequestType}
import param.CoreParam
import params.{CoherenceSpec, MemorySystemParams}

class PipelinedCoreGroupAXI[S <: Data, M <: Data, B <: Data](coreParam: CoreParam,
                            memorySystemParams: MemorySystemParams,
                            coherenceSpec: CoherenceSpec[S, M, B]) extends MultiIOModule {
  private def convertICacheReq(iCacheReq: ICacheReq): CacheReq = {
    val convertedWire = Wire(memorySystemParams.getGenCacheReq)
    convertedWire.length := 2.U
    convertedWire.address := iCacheReq.address
    convertedWire.data := 0.U
    convertedWire.mem_type := MemoryRequestType.read

    convertedWire.aq := 0.U
    convertedWire.rl := 0.U
    convertedWire.is_amo := 0.U
    convertedWire.amo_alu_op := AMOOP.none

    convertedWire.flush := 0.U
    convertedWire.llcc_flush := 0.U

    convertedWire.use_wstrb := false.B
    convertedWire.wstrb := 0.U

    convertedWire
  }
  private def convertDCacheReq(dCacheReq: DCacheReq): CacheReq = {
    val convertedWire = Wire(memorySystemParams.getGenCacheReq)
    convertedWire.length := dCacheReq.length
    convertedWire.address := dCacheReq.address
    convertedWire.data := dCacheReq.data
    convertedWire.mem_type := dCacheReq.memoryType

    convertedWire.aq := 0.U
    convertedWire.rl := 0.U
    convertedWire.is_amo := dCacheReq.isAMO
    convertedWire.amo_alu_op := dCacheReq.amoOP

    convertedWire.flush := 0.U
    convertedWire.llcc_flush := 0.U

    convertedWire.use_wstrb := false.B
    convertedWire.wstrb := 0.U

    convertedWire
  }

  private def convertICacheResp(cacheResp: CacheResp): ICacheResp = {
    val convertedWire = Wire(new ICacheResp(coreParam))
    convertedWire.data := cacheResp.data
    convertedWire.address := cacheResp.address
    convertedWire
  }
  private def convertDCacheResp(cacheResp: CacheResp): DCacheResp = {
    val convertedWire = Wire(new DCacheResp(coreParam))
    convertedWire.data := cacheResp.data
    convertedWire.address := cacheResp.address
    convertedWire
  }

  private def connectCoreCacheInterface[S <: Data, M <: Data, B <: Data]
  (idx: Int, core: Core, memory: PipelinedMemorySubsystem[S, M, B]): Unit = {
    val iCacheIdx = 2 * idx
    val dCacheIdx = 2 * idx + 1
    memory.io.core.request_channel(iCacheIdx).bits := convertICacheReq(core.io.iCacheReq.bits)
    memory.io.core.request_channel(iCacheIdx).valid := core.io.iCacheReq.valid
    core.io.iCacheReq.ready := memory.io.core.request_channel(iCacheIdx).ready

    memory.io.core.request_channel(dCacheIdx).bits := convertDCacheReq(core.io.dCacheReq.bits)
    memory.io.core.request_channel(dCacheIdx).valid := core.io.dCacheReq.valid
    core.io.dCacheReq.ready := memory.io.core.request_channel(dCacheIdx).ready

    core.io.iCacheResp.bits := convertICacheResp(memory.io.core.response_channel(iCacheIdx).bits)
    core.io.iCacheResp.valid := memory.io.core.response_channel(iCacheIdx).valid
    memory.io.core.response_channel(iCacheIdx).ready := core.io.iCacheResp.ready

    core.io.dCacheResp.bits := convertDCacheResp(memory.io.core.response_channel(dCacheIdx).bits)
    core.io.dCacheResp.valid := memory.io.core.response_channel(dCacheIdx).valid
    memory.io.core.response_channel(dCacheIdx).ready := core.io.dCacheResp.ready
  }

  private val wordWidth = 64
  // we only have one master
  private val transactionIDWidth = 1
  private val interfaceAddrWidth = coreParam.isaParam.XLEN
  // note: we use this width to accomodate the width in the zcu102 interface
  // but this can also be done using the other techniques
  private val beatWidth = 64
  val genAXI4 = new AXI4(transactionIDWidth, interfaceAddrWidth, beatWidth)
  val genErrorMessage = new ErrorMessage()
  val m = IO(new Bundle {
    val slot_req = Flipped(Decoupled(memorySystemParams.getGenCacheReq))
    val slot_resp = Decoupled(memorySystemParams.getGenCacheResp)
    val initPC = Input(UInt(coreParam.isaParam.XLEN.W))
    val baseAddress = Input(UInt(coreParam.isaParam.XLEN.W))
    val coreAddr = Output(UInt(64.W))
    val coreData = Output(UInt(32.W))
    val coreValid = Output(Bool())
    val coreReady = Output(Bool())

    val err = Output(genErrorMessage)
    // val stats = new BRAMPort
  })
  val m_axi = IO(genAXI4)

  val stats = IO(new Bundle {
    val lat = Output(Vec(memorySystemParams.masterCount + 1, UInt(64.W)))
  })

  val latency = RegInit(VecInit(Seq.fill(memorySystemParams.masterCount + 1) { 0.U(64.W) }))
  val maxLatency = RegInit(VecInit(Seq.fill(memorySystemParams.masterCount + 1) { 0.U(64.W) }))
  val started = RegInit(VecInit(Seq.fill(memorySystemParams.masterCount + 1) { false.B }))
  val rb_fire = Wire(Vec(memorySystemParams.masterCount + 1, Bool()))
  for { i <- 0 until memorySystemParams.masterCount + 1 } {

  }
  stats.lat := maxLatency

  val memory = Module(new PipelinedMemorySubsystem(coreParam,
    memorySystemParams, genErrorMessage, coherenceSpec, 1))
  // tie-off the debug interface
  /*
  withClockAndReset(m.stats.bram_clk_a.asClock, m.stats.bram_rst_a) {
    val bram_read = RegInit(0.U(64.W))
    when(m.stats.bram_en_b) {
      (0 until memorySystemParams.masterCount).foldLeft(when(false.B) {}) { (prev, cur) =>
        prev.elsewhen(m.stats.bram_addr_b === cur.U) {
          bram_read := memory.io.latency(cur)
        }
      }
    }
    m.stats.bram_rdata_b := bram_read
  } */
  rb_fire := memory.io.rb_fire
  dontTouch(rb_fire)
  for { i <- 0 until memorySystemParams.masterCount + 1 } {
    when(memory.io.core.response_channel(i).fire()) {
      when(memory.io.core.response_channel(i).bits.latency > maxLatency(i)) {
        maxLatency(i) := memory.io.core.response_channel(i).bits.latency
      }
    }
    /*
    when(started(i)) {
      latency(i) := latency(i) + 1.U
      when(rb_fire(i)) {
        latency(i) := 0.U
      }
    }
    when(memory.io.core.request_channel(i).fire() && memory.io.core.response_channel(i).fire()) {
      started(i) := true.B
      when(latency(i) > maxLatency(i)) {
        maxLatency(i) := latency(i)
        when(latency(i) > ((memorySystemParams.masterCount + 1) * (memorySystemParams.masterCount + 1) * memorySystemParams.slotWidth).U) {
          tooLarge(i) := true.B
        }
      }
      latency(i) := 1.U
    }.elsewhen(memory.io.core.request_channel(i).fire()) {
      started(i) := true.B
    }.elsewhen(memory.io.core.response_channel(i).fire()) {
      started(i) := false.B
      when(latency(i) > maxLatency(i)) {
        maxLatency(i) := latency(i)
        when(latency(i) > ((memorySystemParams.masterCount + 1) * (memorySystemParams.masterCount + 1) * memorySystemParams.slotWidth).U) {
          tooLarge(i) := true.B
        }
      }
    }*/
  }

  val cores = for { i <- 0 until coreParam.nCore } yield {
    val core = Module(new Core(coreParam.copy(coreID = i)))

    connectCoreCacheInterface(i, core, memory)

    core.io.irq := 0.U
    core.io.initPC := m.initPC

    core
  }

  memory.io.core.request_channel(coreParam.nCore * 2) <> m.slot_req
  memory.io.core.response_channel(coreParam.nCore * 2) <> m.slot_resp

  m.coreData := cores(0).io.iCacheResp.bits.data
  m.coreAddr := cores(0).io.iCacheResp.bits.address
  m.coreValid := cores(0).io.iCacheResp.valid
  m.coreReady := cores(0).io.iCacheResp.ready

  dontTouch(m)

  val mem_to_axi4 = Module(new MemToAXI4(memorySystemParams))

  memory.io.err <> m.err
  memory.io.dram.request_channel <> mem_to_axi4.io.dramReq
  memory.io.dram.response_channel <> mem_to_axi4.io.dramResp
  mem_to_axi4.m_axi <> m_axi

  /**
    * Offset the address by the base address
    */
  m_axi.araddr := mem_to_axi4.m_axi.araddr + m.baseAddress
  m_axi.awaddr := mem_to_axi4.m_axi.awaddr + m.baseAddress
}
