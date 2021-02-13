
package core

import chisel3._
import chisel3.util._
import param.CoreParam

class WriteBackIO(private val coreParam: CoreParam) extends Bundle {
  val controlInput = Flipped(Decoupled(new Control(coreParam)))
  val dataInput = Flipped(Decoupled(new DataPath(coreParam)))

  val controlOutput = Decoupled(new Control(coreParam))
  val dataOutput = Decoupled(new DataPath(coreParam))
}

class WriteBack(coreParam: CoreParam) extends Module {
  val io = IO(new WriteBackIO(coreParam))
  io.controlInput <> io.controlOutput
  io.dataInput <> io.dataOutput

}
