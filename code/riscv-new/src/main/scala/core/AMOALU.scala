
package core

import chisel3._
import chisel3.util._
import chisel3.experimental._
import param.CoreParam

class AMOALU(val coreParam: CoreParam) extends Module {
  val io = IO(new Bundle {
    val amo_alu_op   = Input(AMOOP())
    val isW          = Input(Bool())
    val in1          = Input(UInt(coreParam.isaParam.XLEN.W))
    val in2          = Input(UInt(coreParam.isaParam.XLEN.W))
    val out          = Output(UInt(coreParam.isaParam.XLEN.W))
  })
  // by default the return value is meaningless
  io.out := 0.U
  switch(io.amo_alu_op) {
    is(AMOOP.add) {
      io.out := io.in1 + io.in2
    }
    is(AMOOP.and) { io.out := io.in1 & io.in2 }
    is(AMOOP.xor) { io.out := io.in1 ^ io.in2 }
    is(AMOOP.or) { io.out := io.in1 | io.in2 }
    is(AMOOP.swap) { io.out := io.in2 }

    is(AMOOP.max) {
      when(io.isW) {
        io.out := Mux(io.in1(31, 0).asSInt > io.in2(31, 0).asSInt,
          Cat(0.U(32.W), io.in1(31, 0)),
          Cat(0.U(32.W), io.in2(31, 0)))
      }.otherwise {
        io.out := Mux(io.in1.asSInt > io.in2.asSInt, io.in1, io.in2)
      }
    }
    is(AMOOP.maxu) {
      when(io.isW) {
        io.out := Mux(io.in1(31, 0).asUInt > io.in2(31, 0).asUInt,
          Cat(0.U(32.W), io.in1(31, 0)),
          Cat(0.U(32.W), io.in2(31, 0)))
      }.otherwise {
        io.out := Mux(io.in1.asUInt > io.in2.asUInt, io.in1, io.in2)
      }
    }
  }
}
