
package components

import chisel3._
import chisel3.experimental._

class FIFO(val depthWidth: Int, val dataWidth: Int) extends BlackBox(
  Map(
    "DEPTH_WIDTH" -> depthWidth,
    "DATA_WIDTH" -> dataWidth
  )
){
  override def desiredName: String = "fifo"
  val io = IO(new Bundle {
    val clk       = Input(Clock())
    val rst       = Input(UInt(1.W))
    val wr_data_i = Input(UInt(dataWidth.W))
    val wr_en_i   = Input(UInt(1.W))
    val rd_data_o = Output(UInt(dataWidth.W))
    val rd_en_i   = Input(UInt(1.W))
    val full_o    = Output(UInt(1.W))
    val empty_o   = Output(UInt(1.W))
    val one_left  = Output(UInt(1.W))
  })
}
