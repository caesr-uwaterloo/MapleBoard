
package components

import chisel3._
import chisel3.util._

// This module receives command and data from separate interface and combine the two
// Since there is no buffer inside this module, the command and data should arrive in order
// The module receives a command and receive a maximal of maxData pieces of genData,
// the combine them into a genRes
abstract class CommandDataConcat[CmdT <: Data, DataT <: Data, ResT <: Data](val genCmd: CmdT,
                                                                   val genData: DataT,
                                                                   val genRes: ResT,
                                                                   val maxData: Int) extends Module {
  val totState = 3
  val sIdle :: sWaitData :: sWaitOut :: Nil = Enum(totState)

  val io = IO(new Bundle {
    val in = new Bundle {
      val command = Flipped(Decoupled(genCmd))
      val data = Flipped(Decoupled(genData))
    }
    val out = Decoupled(genRes)
  })

  val state = RegInit(sIdle)
  val command_reg = Reg(genCmd)
  val data_reg = Reg(Vec(maxData, genData))
  val data_ptr = Reg(UInt(log2Ceil(maxData).W))

  def getFlitsToReceive(): UInt
  def concat(cmd: CmdT, data: Vec[DataT]): ResT

  io.in.command.ready := state === sIdle
  io.in.data.ready := state === sWaitData
  io.out.valid := state === sWaitOut
  io.out.bits := concat(command_reg, data_reg)

  /*
  when(io.in.data.fire()) {
    printf(p"[CDC] data: ${Hexadecimal(io.in.data.bits.asUInt)}\n")
  }

  when(io.out.fire()) {
    printf(p"[Concat] command: $command_reg data: ${Hexadecimal(data_reg.asUInt)}\n")
  }
  */

  switch(state) {
    is(sIdle) {
      when(io.in.command.fire()) {
        state := sWaitData
        command_reg := io.in.command.bits
        data_ptr := 0.U
      }
    }
    is(sWaitData) {
      when(io.in.data.fire()) {
        data_reg(data_ptr) := io.in.data.bits
        data_ptr := data_ptr + 1.U
        when(data_ptr === getFlitsToReceive() - 1.U) {
          state := sWaitOut
        }
      }
    }

    is(sWaitOut) {
      when(io.out.fire()) { state := sIdle }
    }
  }

}
