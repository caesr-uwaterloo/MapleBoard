
package utils

import chisel3.{Input, MultiIOModule, Output}
import chisel3.util._
import param._
import chisel3._
import _root_.core._
import components.{AXI4, CacheReq, CacheResp, ErrorMessage, MemToAXI4, MemorySubsystem}
import params.MemorySystemParams

class BRAMPort extends Bundle {
  val bram_clk_a = Input(Bool())
  val bram_rst_a = Input(Bool())
  val bram_addr_b = Input(UInt(16.W))
  val bram_rdata_b = Output(UInt(64.W))
  val bram_en_b = Input(Bool())
}

class CoreGroupAXI(coreParam: CoreParam, memorySystemParams: MemorySystemParams) extends MultiIOModule {
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

  private def connectCoreCacheInterface(idx: Int, core: Core, memory: MemorySubsystem): Unit = {
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
    val initPC = Input(UInt(coreParam.isaParam.XLEN.W))
    val baseAddress = Input(UInt(coreParam.isaParam.XLEN.W))
    val coreAddr = Output(UInt(64.W))
    val coreData = Output(UInt(32.W))
    val coreValid = Output(Bool())
    val coreReady = Output(Bool())

    val err = Output(genErrorMessage)
    val stats = new BRAMPort
  })
  val m_axi = IO(genAXI4)


  val memory = Module(new MemorySubsystem(coreParam, memorySystemParams, genErrorMessage))
  // tie-off the debug interface
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
  }

  val cores = for { i <- 0 until coreParam.nCore } yield {
    val core = Module(new Core(coreParam.copy(coreID = i)))

    connectCoreCacheInterface(i, core, memory)

    core.io.irq := 0.U
    core.io.initPC := m.initPC

    core
  }

  m.coreData := cores(0).io.iCacheResp.bits.data
  m.coreAddr := cores(0).io.iCacheResp.bits.address
  m.coreValid := cores(0).io.iCacheResp.valid
  m.coreReady := cores(0).io.iCacheResp.ready

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



/**
  * this module contains a simple AXI Memory module that we can use
  */
class CoreGroupAXIWithMemory(coreParam: CoreParam, memorySystemParams: MemorySystemParams) extends MultiIOModule {
  val genErrorMessage = new ErrorMessage()
  val m = IO(new Bundle {
    val initPC = Input(UInt(coreParam.isaParam.XLEN.W))
    val baseAddress = Input(UInt(coreParam.isaParam.XLEN.W))
    val coreAddr = Output(UInt(64.W))
    val coreData = Output(UInt(32.W))
    val coreValid = Output(Bool())
    val coreReady = Output(Bool())

    val err = Output(genErrorMessage)
    val stats = new BRAMPort
  })

  val cg = Module(new CoreGroupAXI(coreParam, memorySystemParams))
  val mem = Module(new AXIMemory(coreParam))
  cg.m <> m
  connectAXI()

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
