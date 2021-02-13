
package components

import chisel3._
import chisel3.util._
import params._
import _root_.core.{AMOOP, AMOALU => SimpleAMOALU}
import param.CoreParam

class AMOALU(val coreParam: CoreParam, val cacheParams: SimpleCacheParams) extends Module {
  val io = IO(new Bundle {
    val amo_alu_op   = Input(AMOOP())
    val isW          = Input(Bool())
    val in1          = Input(UInt(coreParam.isaParam.XLEN.W))
    val in2          = Input(UInt(coreParam.isaParam.XLEN.W))
    val lrsc_valid   = Input(Bool())
    val lrsc_address = Input(UInt(coreParam.isaParam.XLEN.W))
    val sc_address   = Input(UInt(coreParam.isaParam.XLEN.W))
    val out          = Output(UInt(coreParam.isaParam.XLEN.W))
  })

  val simple_amo_alu = Module(new SimpleAMOALU(coreParam))
  simple_amo_alu.io.amo_alu_op := io.amo_alu_op
  simple_amo_alu.io.isW := io.isW
  simple_amo_alu.io.in1 := io.in1
  simple_amo_alu.io.in2 := io.in2

  io.out := simple_amo_alu.io.out

  when(io.amo_alu_op === AMOOP.lr) {
    io.out := simple_amo_alu.io.in1
  }.elsewhen(io.amo_alu_op === AMOOP.sc) {
    when(io.lrsc_valid && io.lrsc_address(cacheParams.addrWidth - 1, cacheParams.lineOffsetWidth)  === io.sc_address(cacheParams.addrWidth - 1, cacheParams.lineOffsetWidth) ) {
      io.out := 0.U
    }.otherwise {
      io.out := 1.U
    }
  }


}
