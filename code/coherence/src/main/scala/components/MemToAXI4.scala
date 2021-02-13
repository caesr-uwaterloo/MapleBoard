
package components

import chisel3._
import chisel3.experimental._
import chisel3.util._
import coherences.PMESI
// import dbgutil.exposeTop
import params.{MemorySystemParams, SimpleCacheParams}

import scala.collection.immutable.ListMap

class MemToAXI4(val memorySystemParams: MemorySystemParams,
                val sameLatency: Boolean = true,
                val memoryLatency: Int = 40) extends MultiIOModule{
  /* constants */
  val transactionIdWidth: Int = 1
  val beatWidth: Int = 64
  val addrWidth = memorySystemParams.addrWidth
  val genAR = new AXI4ReadAddress(transactionIdWidth, addrWidth)
  val genR = new AXI4ReadData(transactionIdWidth, beatWidth)
  val genAW = new AXI4WriteAddress(transactionIdWidth, addrWidth)
  val genW = new AXI4WriteData(beatWidth)
  val genB = new AXI4WriteResponse(transactionIdWidth)

  val nState = 7
  val sIdle :: sAR :: sR :: sAW :: sW :: sB :: sResp :: Nil = Enum(nState)

  /* interface */
  val io = IO(new Bundle {
    val dramReq = Flipped(Decoupled(memorySystemParams.getGenDramReq))
    val dramResp = Decoupled(memorySystemParams.getGenDramResp)
    val state = Output(UInt(nState.W))
  })
  val m_axi = IO(new AXI4(transactionIdWidth, addrWidth, beatWidth))

  /* logic */
  val beatsPerData = memorySystemParams.getGenDramReq.data.getWidth / beatWidth

  val dramReqReg = RegEnable(io.dramReq.bits, io.dramReq.fire())
  val dramRespDataReg = Reg(Vec(beatsPerData, UInt(beatWidth.W)))
  val dramReqDataReg = WireInit(VecInit.tabulate(beatsPerData)(i => {
    dramReqReg.data((i + 1) * beatWidth - 1, i * beatWidth)
  }))
  require(dramRespDataReg.getWidth == memorySystemParams.getGenDramResp.data.getWidth)
  val state = RegInit(sIdle)
  // exposeTop(state)
  io.state := state
  val (wdataCounter, wdataCounterWrap) = Counter(m_axi.wfire(), beatsPerData)
  val (rdataCounter, rdataCounterWrap) = Counter(m_axi.rfire(), beatsPerData)
  val latencyCounter = RegInit(0.U(64.W))
  val maxLatency = RegInit(0.U(64.W))
  // val latencyCounter = RegInit(0.U( (1 + log2Ceil(memoryLatency)).W))
  // val latencyEn = RegInit(false.B)

  io.dramReq.ready := state === sIdle
  io.dramResp.valid := (state === sResp)
  io.dramResp.bits.data := dramRespDataReg.asUInt()
  io.dramResp.bits.length := dramReqReg.length
  io.dramResp.bits.mem_type := dramReqReg.mem_type

  /* set default values */
  m_axi.awid    := 0.U
  m_axi.awprot  := m_axi.awprotDefault
  m_axi.awcache := m_axi.awcacheDefault
  m_axi.awvalid := state === sAW
  m_axi.awsize := log2Ceil(beatWidth / 8).U
  m_axi.awburst := 1.U
  m_axi.awlen := (beatsPerData - 1).U
  m_axi.awaddr := dramReqReg.address
  m_axi.awlock := 0.U

  m_axi.arid    := 0.U
  m_axi.arprot  := m_axi.arprotDefault
  m_axi.arcache := m_axi.arcacheDefault
  m_axi.arvalid := state === sAR
  m_axi.arsize := log2Ceil(beatWidth / 8).U
  m_axi.arburst := 1.U
  m_axi.arlen := (beatsPerData - 1).U
  m_axi.araddr := dramReqReg.address
  m_axi.arlock := 0.U

  m_axi.wvalid := state === sW
  m_axi.wstrb := ( (1 << (beatWidth / 8)) - 1).U
  m_axi.wlast := wdataCounterWrap
  m_axi.wdata := dramReqDataReg(wdataCounter)

  m_axi.bready := state === sB

  m_axi.rready := state === sR

  when(m_axi.rfire()) { dramRespDataReg(rdataCounter) := m_axi.rdata }
  when(state =/= sIdle) {
    latencyCounter := latencyCounter + 1.U
  }.otherwise {
    when(latencyCounter > maxLatency) {
      maxLatency := latencyCounter
    }
    latencyCounter := 0.U
  }
  dontTouch(maxLatency)
  switch(state) {
    is(sIdle) {
      when(io.dramReq.fire()) {
        when(io.dramReq.bits.mem_type === 0.U) { // write
          state := sAW
        }.otherwise { // read
          state := sAR
        }
      }
    }
    is(sAW)   { when(m_axi.awfire()              ) { state := sW    } }
    is(sW)    { when(m_axi.wfire() && m_axi.wlast) { state := sB    } }
    is(sB)    { when(m_axi.bfire()               ) { state := sResp } }
    is(sAR)   { when(m_axi.arfire()              ) { state := sR    } }
    is(sR)    { when(m_axi.rfire() && m_axi.rlast) { state := sResp } }
    is(sResp) { when(io.dramResp.fire()          ) { state := sIdle } }
  }

  when(m_axi.wfire()) {
    printf(p"[MemToAXI] ${Hexadecimal(m_axi.wdata)}\n")
  }

  dontTouch(m_axi)


}


class AXI4ToMem(val memorySystemParams: MemorySystemParams) extends MultiIOModule{
  /* constants */
  val transactionIdWidth: Int = 1
  val beatWidth: Int = 128
  val addrWidth = memorySystemParams.addrWidth
  val interfaceAddrWidth = memorySystemParams.interfaceAddrWidth
  val genAR = new AXI4ReadAddress(transactionIdWidth, addrWidth)
  val genR = new AXI4ReadData(transactionIdWidth, beatWidth)
  val genAW = new AXI4WriteAddress(transactionIdWidth, addrWidth)
  val genW = new AXI4WriteData(beatWidth)
  val genB = new AXI4WriteResponse(transactionIdWidth)

  /* interface */
  val io = IO(new Bundle {
    val dramReq = Decoupled(memorySystemParams.getGenDramReq)
    val dramResp = Flipped(Decoupled(memorySystemParams.getGenDramResp))
  })
  val s_axi = IO(Flipped(new AXI4(transactionIdWidth, interfaceAddrWidth, beatWidth)))

  /* logic */
  val beatsPerData = memorySystemParams.getGenDramReq.data.getWidth / beatWidth
  val nState = 8
  val sIdle :: sAR :: sR :: sAW :: sW :: sB :: sReq :: sResp :: Nil = Enum(nState)
  val memTypeReg = Reg(UInt(1.W))
  val addressReg = RegInit((0xffffffffL).U(addrWidth.W))
  val dramReqDataReg = Reg(Vec(beatsPerData, UInt(beatWidth.W)))
  require(dramReqDataReg.getWidth == memorySystemParams.getGenDramReq.data.getWidth)
  val dramRespReg = RegEnable(io.dramResp.bits, io.dramResp.fire())
  val dramRespDataReg = WireInit(VecInit.tabulate(beatsPerData)(i => {
    dramRespReg.data(beatWidth * (i + 1) - 1, beatWidth * i)
  }))
  val state = RegInit(sIdle)
  val (wdataCounter, wdataCounterWrap) = Counter(s_axi.wfire(), beatsPerData)
  val (rdataCounter, rdataCounterWrap) = Counter(s_axi.rfire(), beatsPerData)
  val (rwCounter, rwCounterWrap) = Counter(s_axi.awfire() || s_axi.arfire(), 2)

  io.dramResp.ready := state === sResp

  io.dramReq.valid := state === sReq
  io.dramReq.bits.data := dramReqDataReg.asUInt()
  io.dramReq.bits.length := log2Ceil(memorySystemParams.cacheParams.lineBytes).U
  io.dramReq.bits.mem_type := memTypeReg
  io.dramReq.bits.address := addressReg

  /* set default values */
  s_axi.arready := state === sAR
  s_axi.awready := state === sAW

  s_axi.wready := state === sW

  s_axi.bvalid := state === sB
  s_axi.bresp  := 0.U // EOKAY
  s_axi.bid    := s_axi.defaultID

  s_axi.rvalid := state === sR
  s_axi.rid    := s_axi.defaultID
  s_axi.rresp  := 0.U // EOKAY
  s_axi.rdata  := dramRespDataReg(rdataCounter)
  s_axi.rlast  := rdataCounterWrap


  switch(state) {
    is(sIdle) {
      when(rwCounter === 0.U && s_axi.arvalid) { state := sAR }
        .elsewhen(s_axi.awvalid) { state := sAW }
        .elsewhen(s_axi.arvalid) { state := sAR }
    }
    is(sAW) {
      state := sIdle
      when(s_axi.awfire()) {
        addressReg := s_axi.awaddr
        memTypeReg := 0.U
        state := sW
      }
    }
    is(sAR) {
      state := sIdle
      when(s_axi.arfire()) {
        addressReg := s_axi.araddr
        memTypeReg := 1.U
        state := sReq
      }
    }

    is(sW) {
      when(s_axi.wfire()) {
        dramReqDataReg(wdataCounter) := s_axi.wdata
      }
      when(s_axi.wfire() && s_axi.wlast) {
        state := sReq
      }
    }
    is(sB) {
      when(s_axi.bfire()) {
        state := sIdle
      }
    }

    is(sR) {
      when(s_axi.rfire() && s_axi.rlast) {
        state := sIdle
      }
    }

    is(sReq)  { when(io.dramReq.fire() ) { state := sResp } }
    is(sResp) {
      when(io.dramResp.fire()) {
        when(memTypeReg === 0.U) { state := sB }.otherwise { state := sR }
      }
    }
  }
  //val s = (for((n, d) <- s_axi.elements) yield {
  //  s"wire [${d.getWidth-1}:0] ${d.toNamed};"
  //}).mkString("\n")
  //println(s)
}
