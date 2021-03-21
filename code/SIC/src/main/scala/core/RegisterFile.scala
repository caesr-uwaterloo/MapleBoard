
package core

import chisel3._
import chisel3.util._
import param.RISCVParam

class RegisterFileIO(private val isaParam: RISCVParam) extends Bundle {
  // read ports
  val raddr1 = Input(UInt(log2Ceil(isaParam.registerCount).W))
  val raddr2 = Input(UInt(log2Ceil(isaParam.registerCount).W))
  val rdata1 = Output(UInt(isaParam.XLEN.W))
  val rdata2 = Output(UInt(isaParam.XLEN.W))
  // write port
  val wen = Input(Bool())
  val waddr = Input(UInt(log2Ceil(isaParam.registerCount).W))
  val wdata = Input(UInt(isaParam.XLEN.W))
}

class RegisterFile(isaParam: RISCVParam) extends Module {
  val io = IO(new RegisterFileIO(isaParam))
  val data = RegInit(VecInit(Seq.fill(isaParam.registerCount) {
    0.U(isaParam.XLEN.W)
  }))

  io.rdata1 := data(io.raddr1)
  io.rdata2 := data(io.raddr2)

  when(io.wen && io.waddr =/= 0.U) {
    data(io.waddr) := io.wdata
  }
  // printf(p"[Reg] x3 = ${Hexadecimal(data(3))}, raddr2: ${Hexadecimal(io.raddr2)}\n")
}
