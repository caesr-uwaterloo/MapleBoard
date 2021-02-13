
package utils

import chisel3._
import chisel3.util._
import components.{AXI4Lite, AXI4X}
import params.MemorySystemParams
import AXI4Lite.AddMethodsToAXI4Lite
import _root_.core.AMOOP
import _root_.core.MemoryRequestType

// This port act as a port from the core to the cache
class AXI4ToReq (val memorySystemParams: MemorySystemParams) extends MultiIOModule {
  val addrWidth = 64
  val dataWidth = 64
  val tranId = 1
  val io = IO(new Bundle {
    val req = Decoupled(memorySystemParams.getGenCacheReq)
    val resp = Flipped(Decoupled(memorySystemParams.getGenCacheResp))
  })
  val s_axi = IO(Flipped(new AXI4X(addrWidth, dataWidth, tranId)))
  val state = RegInit(ConfregState.idle)

  val addr = RegInit(0.U(addrWidth.W))
  // Two same counter for generating the last signal
  val len = Reg(UInt(9.W))
  val lenreq = Reg(UInt(9.W))
  val size = Reg(UInt(log2Ceil(dataWidth / 8).W))
  val burstType = Reg(AXI4X.BurstType())

  // the host might generate 8 requests at once for the 64-bit accesses...
  val reqQ = Module(new Queue(memorySystemParams.getGenCacheReq, 8))
  val respQ = Module(new Queue(memorySystemParams.getGenCacheResp, 8))
  io.req <> reqQ.io.deq
  respQ.io.enq <> io.resp

  val incr = (1.U << size).asUInt
  val bdone = Reg(Bool())
  val shiftCount = RegInit(0.U(3.W))

  reqQ.io.enq.bits.is_amo := false.B
  reqQ.io.enq.bits.amo_alu_op := AMOOP.none
  reqQ.io.enq.bits.address := 0.U
  reqQ.io.enq.bits.mem_type := MemoryRequestType.read
  reqQ.io.enq.bits.data := 0.U
  reqQ.io.enq.bits.length := 0.U
  reqQ.io.enq.bits.llcc_flush := false.B
  reqQ.io.enq.bits.aq := 0.U
  reqQ.io.enq.bits.rl := 0.U
  reqQ.io.enq.bits.flush := false.B
  reqQ.io.enq.bits.use_wstrb := false.B
  reqQ.io.enq.bits.wstrb := 0.U
  reqQ.io.enq.valid := false.B


  s_axi.aw.ready := false.B
  s_axi.ar.ready := false.B
  s_axi.w.ready := false.B
  s_axi.b.valid := false.B
  s_axi.b.bits.id := 0.U
  s_axi.b.bits.resp := AXI4X.Resp.OKAY
  s_axi.r.valid := false.B
  s_axi.r.bits.data := "hdeadffffffffdead".U
  s_axi.r.bits.resp := AXI4X.Resp.OKAY
  s_axi.r.bits.id := 0.U
  s_axi.r.bits.last := false.B
  respQ.io.deq.ready := false.B

  switch(state) {
    is(ConfregState.idle) {
      // only process read or right at a time
      s_axi.aw.ready := true.B
      s_axi.ar.ready := !s_axi.aw.valid
      when(s_axi.aw.fire()) {
        // Offset the addresses so that they got translated correctly
        state := ConfregState.w
        addr := s_axi.aw.bits.addr - "h100000000".U
        // len := s_axi.aw.bits.len
        len := 0.U
        lenreq := s_axi.aw.bits.len
        size := s_axi.aw.bits.size
        burstType := s_axi.aw.bits.burst
        shiftCount := s_axi.aw.bits.addr(2, 0)
      }.elsewhen(s_axi.ar.fire()) {
        // Offset the addresses so that they got translated correctly
        state := ConfregState.rreq
        addr := s_axi.ar.bits.addr - "h100000000".U
        len := s_axi.ar.bits.len
        lenreq := s_axi.ar.bits.len
        size := s_axi.ar.bits.size
        burstType := s_axi.ar.bits.burst
        shiftCount := s_axi.ar.bits.addr(2, 0)
      }
    }
    is(ConfregState.rreq) {
      reqQ.io.enq.valid := true.B
      reqQ.io.enq.bits.address := addr
      reqQ.io.enq.bits.mem_type := MemoryRequestType.read
      reqQ.io.enq.bits.length := size
      when(reqQ.io.enq.fire()) {
        when(burstType === AXI4X.BurstType.INCR) {
          addr := addr + incr
        }
        lenreq := lenreq - 1.U
        when(lenreq === 0.U) {
          state := ConfregState.r
        }
      }
    }
    is(ConfregState.wreq) {

    }
    is(ConfregState.r) {
      s_axi.r.valid := respQ.io.deq.valid
      respQ.io.deq.ready := s_axi.r.ready
      s_axi.r.bits.last := (len === 0.U).asUInt
      s_axi.r.bits.data := (respQ.io.deq.bits.data << Cat(shiftCount, 0.U(3.W))).asUInt
      s_axi.r.bits.resp := AXI4X.Resp.OKAY
      when(s_axi.r.fire()) {
        shiftCount := shiftCount + incr
        len := len - 1.U
        when(s_axi.r.bits.last.asBool) {
          state := ConfregState.idle
        }
      }
    }
    is(ConfregState.w) {
      reqQ.io.enq.valid := s_axi.w.valid && s_axi.w.bits.strb =/= 0.U
      s_axi.w.ready := reqQ.io.enq.ready || s_axi.w.bits.strb === 0.U
      reqQ.io.enq.bits.address := Cat(addr(63, 3), 0.U(3.W))
      reqQ.io.enq.bits.data := s_axi.w.bits.data // shiftedData
      // generate data
      reqQ.io.enq.bits.mem_type := MemoryRequestType.write
      reqQ.io.enq.bits.length := size
      reqQ.io.enq.bits.use_wstrb := true.B
      reqQ.io.enq.bits.wstrb := s_axi.w.bits.strb

      when(reqQ.io.enq.fire()) {
        len := len + 1.U
      }
      when(s_axi.w.fire()) {
        when(burstType === AXI4X.BurstType.INCR) {
          addr := addr + incr
        }
        lenreq := lenreq - 1.U
        when(lenreq === 0.U) {
          bdone := false.B
          state := ConfregState.b
        }
      }
    }
    is(ConfregState.b) {
      s_axi.b.valid := true.B
      s_axi.b.bits.resp := AXI4X.Resp.OKAY
      when(s_axi.b.fire()) {
        state := ConfregState.bwait
      }
    }

    is(ConfregState.bwait) {
      when(!bdone) {
        respQ.io.deq.ready := true.B
        when(respQ.io.deq.fire()) {
          len := len - 1.U
          when(len === 1.U) {
            bdone := true.B
          }
        }.elsewhen(len === 0.U) {
          bdone := true.B
        }
      }
      when(bdone) {
        state := ConfregState.idle
      }
    }
  }

}
