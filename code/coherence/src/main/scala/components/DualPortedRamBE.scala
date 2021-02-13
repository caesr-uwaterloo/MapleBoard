
package components

import chisel3._
import chisel3.util._

class DualPortedRamBE[T <: Data](val depth: Int, val len: Int, val genT: T) extends Module {
  val io = IO(new Bundle {
    val wen = Input(Vec(len, Bool()))
    val waddr = Input(UIntHolding(depth))
    val din = Input(Vec(len, genT))

    val ren = Input(Bool())
    val raddr = Input(UIntHolding(depth))
    val dout = Output(Vec(len, genT))
  })
  val mem = SyncReadMem(depth, Vec(len, genT))
  mem.write(io.waddr, io.din, io.wen)
  io.dout := mem.read(io.raddr, io.ren)
}
