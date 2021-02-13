
package fixtures

import chisel3.experimental.ChiselEnum
import chisel3.util.{Cat, is, log2Ceil, switch}
import chisel3.{Bundle, Flipped, Module, Reg, RegInit, UInt, when}
import core.{AXI, MemoryRequestType}
import param.CoreParam
import chisel3._
import chisel3.util._

class MemoryDPIBlank(coreParam: CoreParam) extends Module {
  private val addrWidth = 64
  private val wordWidth = 64
  private val depth = 65536
  val memoryRead = 0
  val memoryWrite = 1
  val io = IO(
    new Bundle {
      val valid = Input(Bool())
      val reqAddr = Input(UInt(addrWidth.W))
      val reqType = Input(UInt(1.W))
      val reqData = Input(UInt(wordWidth.W))
      val reqStrb = Input(UInt((wordWidth / 8).W))
      val respData = Output(UInt(wordWidth.W))
    }
  )
  // what's the latency?
  val data = RegInit(0.U(wordWidth.W))
  val ram = SyncReadMem(depth, Vec(wordWidth / 8, UInt(8.W)))
  val addr = (io.reqAddr >> log2Ceil(wordWidth / 8).U).asUInt
  io.respData := ram.read(addr, io.valid && (io.reqType === memoryRead.U)).asUInt
  ram.write(addr, io.reqData.asTypeOf(Vec(wordWidth / 8, UInt(8.W))), for { i <- 0 until wordWidth / 8 } yield {
    io.reqStrb(i).asBool && io.valid && (io.reqType === memoryWrite.U)
  })
}

// This module is only supposed to be in test for latency
// INITIALIZATION OF THIS MODULE IS NOT SUPPORTED!!!
class AXIMemoryBlank(coreParam: CoreParam) extends Module {
  object AXIStates extends ChiselEnum { val idle, ar, r, aw, w, b, req, resp, wt = Value }
  private val wordWidth = coreParam.axiParam.get.axiWordWidth
  private val read = MemoryRequestType.read.litValue().U
  private val write = MemoryRequestType.write.litValue().U
  val io = IO(new Bundle {
    val m_axi = Flipped(new AXI(coreParam.isaParam.XLEN, wordWidth, log2Ceil(2 * coreParam.nCore)))
  })
  val memoryDPI = Module(new MemoryDPIBlank(coreParam))
  val state = RegInit(AXIStates.idle)

  val reqAddr = Reg(UInt(coreParam.isaParam.XLEN.W))
  val reqType = Reg(UInt(1.W))
  val reqData = Reg(UInt(wordWidth.W))
  val reqLen = Reg(UInt(8.W))
  val reqCounter = Reg(UInt(8.W))
  val reqStrb = Reg(UInt((wordWidth / 8).W))
  val respData = Reg(UInt(coreParam.isaParam.XLEN.W))

  val reqDelayer = RegInit(0.U(32.W))
  val nextState = RegInit(AXIStates.idle)

  /* set default values */
  io.m_axi.arready := state === AXIStates.ar
  io.m_axi.awready := state === AXIStates.aw

  io.m_axi.wready := state === AXIStates.w

  io.m_axi.bvalid := state === AXIStates.b
  io.m_axi.bresp  := 0.U // EOKAY
  io.m_axi.bid    := 0.U

  io.m_axi.rvalid := state === AXIStates.r
  io.m_axi.rid    := 0.U
  io.m_axi.rresp  := 0.U // EOKAY
  io.m_axi.rdata  := respData
  io.m_axi.rlast  := reqLen === 1.U


  memoryDPI.io.valid := (state === AXIStates.req && reqType === read) || (state === AXIStates.r && io.m_axi.rready && io.m_axi.rvalid && io.m_axi.rlast =/= true.B) || (state === AXIStates.w && io.m_axi.wready && io.m_axi.wvalid)
  memoryDPI.io.reqType := reqType
  memoryDPI.io.reqAddr := reqAddr + Cat(reqCounter, 0.U(log2Ceil(wordWidth / 8).W))
  memoryDPI.io.reqData := reqData
  memoryDPI.io.reqStrb := reqStrb

  val respQ = Module(new Queue(UInt(wordWidth.W), 8, pipe = true))
  io.m_axi.rvalid := respQ.io.deq.valid
  respQ.io.deq.ready := io.m_axi.rready
  io.m_axi.rdata := respQ.io.deq.bits
  val lastValid = RegNext(memoryDPI.io.valid)
  respQ.io.enq.bits := memoryDPI.io.respData
  assert(respQ.io.enq.ready)
  respQ.io.enq.valid := lastValid && (reqType === read)

  when(state === AXIStates.w) {
    memoryDPI.io.reqData := io.m_axi.wdata
    memoryDPI.io.reqStrb := io.m_axi.wstrb
  }
  when(state === AXIStates.idle) {
    reqDelayer := 0.U
  }.elsewhen(state === AXIStates.wt) {
    reqDelayer := reqDelayer + 1.U
  }

  switch(state) {
    is(AXIStates.idle) {
      // printf("[AXIMem] idle\n")
      when(io.m_axi.awvalid) { state := AXIStates.aw }
        .elsewhen(io.m_axi.arvalid) { state := AXIStates.ar }
    }
    is(AXIStates.aw) {
      // printf("[AXIMem] aw\n")
      state := AXIStates.idle
      when(io.m_axi.awready && io.m_axi.awvalid) {
        reqAddr := io.m_axi.awaddr
        reqType := write
        reqLen := io.m_axi.awlen + 1.U
        reqCounter := 0.U
        nextState := AXIStates.w
        state := AXIStates.wt
      }
    }
    is(AXIStates.ar) {
      // printf("[AXIMem] ar\n")
      // printf(p"[AXIMem] reqLen: ${Hexadecimal(reqLen)}")
      state := AXIStates.idle
      when(io.m_axi.arready && io.m_axi.arvalid) {
        reqAddr := io.m_axi.araddr
        reqType := read
        reqLen := io.m_axi.arlen + 1.U
        reqCounter := 0.U
        nextState := AXIStates.req
        state  := AXIStates.wt
      }
    }
    is(AXIStates.wt) {
      when(reqDelayer >= 20.U) {
        state := nextState
      }
    }

    is(AXIStates.w) {
      // must be last
      when(io.m_axi.wready && io.m_axi.wvalid) {
        reqLen := reqLen - 1.U
        reqCounter := reqCounter + 1.U
        // printf(p"[AXIMem] w, addr ${Hexadecimal(reqAddr)} ${Hexadecimal(io.m_axi.wdata)} reqStrb ${Binary(reqStrb)}\n")
        when(io.m_axi.wlast) {
          reqData := io.m_axi.wdata
          reqStrb := io.m_axi.wstrb
          state := AXIStates.req
        }
      }
    }

    is(AXIStates.req) {
      // printf(p"[AXIMem] req, addr: ${Hexadecimal(reqAddr)}\n")
      state := AXIStates.resp
      reqCounter := reqCounter + 1.U
    }
    is(AXIStates.resp) {
      // printf("[AXIMem] resp\n")
      when(reqType === read) {
        state := AXIStates.r
        respData := memoryDPI.io.respData
        // printf(p"[AXIMem] respdata: ${Hexadecimal(memoryDPI.io.respData)}\n")
      }.elsewhen(reqType === write) {
        state := AXIStates.b
      }
    }
    is(AXIStates.r) {
      // printf(p"[AXIMem] r, reqCounter: ${Hexadecimal(reqCounter)}, reqLen: ${Hexadecimal(reqLen)}\n")
      respData := memoryDPI.io.respData
      when(io.m_axi.rvalid && io.m_axi.rready) {
        // printf(p"[AXIMem] r ${Hexadecimal(io.m_axi.rdata)}\n")
        reqLen := reqLen - 1.U
        reqCounter := reqCounter + 1.U
        when(io.m_axi.rlast) {
          state := AXIStates.idle
        }
      }
    }
    is(AXIStates.b) {
      // printf("[AXIMem] b\n")
      when(io.m_axi.bvalid && io.m_axi.bready) {
        state := AXIStates.idle
      }
    }
  }
}
