
package utils

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util._
import components.{AXI4Lite, AXI4X}
import params.MemorySystemParams
import AXI4Lite.AddMethodsToAXI4Lite

object ConfregState extends ChiselEnum {
  val idle, w, r, b, rreq, wreq, bwait = Value
}

class Confreg(memorySystemParams: MemorySystemParams) extends MultiIOModule {

  val dataWidth = 64
  val addrWidth = 64
  val transactionIDWidth = 1
  val s_axi = IO(Flipped(new AXI4X(addrWidth, dataWidth, transactionIDWidth)))
  val reg = IO(new Bundle {
    val initPC = Output(UInt(dataWidth.W))
    val resetReg = Output(UInt(dataWidth.W))
    val st = Output(ConfregState())
    // Note that base address can be set via the AXI BAR
  })
  val stats = IO(new Bundle {
    val lat = Input(Vec(memorySystemParams.masterCount + 1, UInt(64.W)))
  })
  val initPC = RegInit(0.U(dataWidth.W))
  val initPCPipe = Module(new util.Pipe(UInt(dataWidth.W), 3))
  initPCPipe.io.enq.bits := initPC
  initPCPipe.io.enq.valid := true.B
  // defaults to be reset high
  val resetReg = RegInit(1.U(dataWidth.W))
  val resetPipe = Module(new util.Pipe(UInt(dataWidth.W), 3))
  resetPipe.io.enq.bits := resetReg
  resetPipe.io.enq.valid := true.B

  val addr = RegInit(0.U(addrWidth.W))
  val len = Reg(UInt(8.W))
  val size = Reg(UInt(log2Ceil(dataWidth / 8).W))
  val burstType = Reg(AXI4X.BurstType())

  val state = RegInit(ConfregState.idle)
  reg.initPC := initPCPipe.io.deq.bits
  reg.resetReg := resetPipe.io.deq.bits
  s_axi.aw.ready := false.B
  s_axi.ar.ready := false.B
  s_axi.r.valid := false.B
  s_axi.w.ready := false.B
  s_axi.b.valid := false.B
  s_axi.r.bits.resp := AXI4X.Resp.OKAY
  s_axi.r.bits.id := 0.U
  s_axi.r.bits.data := 0.U
  s_axi.r.bits.last := 0.U
  s_axi.b.bits.resp := AXI4X.Resp.OKAY
  s_axi.b.bits.id := 0.U

  reg.st := state

  val incr = (1.U << size).asUInt

  switch(state) {
    is(ConfregState.idle) {
      s_axi.aw.ready := true.B
      s_axi.ar.ready := !s_axi.aw.valid
      when(s_axi.aw.fire()) {
        state := ConfregState.w
        addr := s_axi.aw.bits.addr
        len := s_axi.aw.bits.len
        size := s_axi.aw.bits.size
        burstType := s_axi.aw.bits.burst
      }.elsewhen(s_axi.ar.fire()) {
        state := ConfregState.r
        addr := s_axi.ar.bits.addr
        len := s_axi.ar.bits.len
        size := s_axi.ar.bits.size
        burstType := s_axi.ar.bits.burst
      }
    }
    is(ConfregState.r) {
      s_axi.r.valid := true.B
      s_axi.r.bits.last := (len === 0.U).asUInt
      when(addr(15, 0) === 0x3000L.U) {
        s_axi.r.bits.resp := AXI4X.Resp.OKAY // EOKAY
        s_axi.r.bits.data := initPC
      }.elsewhen(addr(15, 0) === 0x0000L.U) {
        s_axi.r.bits.resp := AXI4X.Resp.OKAY // EOKAY
        s_axi.r.bits.data := resetReg
      }.elsewhen(addr(15, 12) === 0x1L.U) {
        when(addr(11, 3) < (memorySystemParams.masterCount + 1).U) {
          s_axi.r.bits.resp := AXI4X.Resp.OKAY
          s_axi.r.bits.data := stats.lat(addr(11,3))

        }.otherwise {
          s_axi.r.bits.resp := AXI4X.Resp.OKAY
          s_axi.r.bits.data := "hdeadffffffffdead".U
        }
      }.otherwise {
        s_axi.r.bits.resp := AXI4X.Resp.OKAY
        s_axi.r.bits.data := "hdeadffffffffdead".U
      }

      when(s_axi.r.fire()) {
        when(burstType === AXI4X.BurstType.INCR) {
          addr := addr + incr
        }
        len := len - 1.U
        when(s_axi.r.bits.last.asBool) {
          state := ConfregState.idle
        }
      }
    }
    is(ConfregState.w) {
      s_axi.w.ready := true.B

      when(s_axi.w.fire()) {
        when(burstType === AXI4X.BurstType.INCR) {
          addr := addr + incr
        }
        len := len - 1.U
        when(addr(15, 0) === 0x0003000L.U && s_axi.w.bits.strb === "b11111111".U) {
          initPC := s_axi.w.bits.data
        }.elsewhen(addr(15, 0) === 0x0000000L.U && s_axi.w.bits.strb === "b11111111".U) {
          resetReg := s_axi.w.bits.data
        }
        when(s_axi.w.bits.last.asBool()) {
          state := ConfregState.b
        }
      }
    }
    is(ConfregState.b) {
      s_axi.b.valid := true.B
      s_axi.b.bits.resp := AXI4X.Resp.OKAY
      when(s_axi.b.fire()) {
        state := ConfregState.idle
      }
    }
  }

  dontTouch(s_axi)
}
