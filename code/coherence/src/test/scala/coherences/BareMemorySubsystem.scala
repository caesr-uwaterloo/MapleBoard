
package coherences

import chisel3._
import chisel3.util.Decoupled
import components.{CacheReq, CacheResp, ErrorMessage, MemToAXI4, MemorySubsystem}
import param.CoreParam
import params.MemorySystemParams

class BareMemorySubsystem(coreParam: CoreParam, memorySystemParams: MemorySystemParams) extends Module {
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
  })
  // we need to compose the memory subsystem
  val memory = Module(new MemorySubsystem(coreParam, memorySystemParams, genErrorMessage))
  val cg = Module(new MemToAXI4(memorySystemParams))
  val mem = Module(new fixtures.AXIMemoryBlank(coreParam))
  io.latency := memory.io.latency

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
