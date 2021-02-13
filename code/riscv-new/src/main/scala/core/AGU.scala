
package core

import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum
import param.{CoreParam, RISCVParam}

class AGUIO(private val coreParam: CoreParam) extends Bundle {
  private val XLEN = coreParam.isaParam.XLEN
  val address = Input(UInt(XLEN.W))
  val offset = Input(UInt(XLEN.W))
  val target = Output(UInt(XLEN.W))
}

class AGU(private val coreParam: CoreParam) extends Module {
  val io = IO(new AGUIO(coreParam))

  io.target := io.address + io.offset
}
