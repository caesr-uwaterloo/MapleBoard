
package components

import chisel3._
import chisel3.util._
import params._
import chisel3.experimental._

/*
class PriorityEncoder(val width: Int, val lsb_priority: String) extends BlackBox(
  Map(
  "WIDTH" -> width,
  "LSB_PRIORITY" -> lsb_priority
  )
) {
  val io = IO(new Bundle {
    val input_unencoded  = Input(UInt(width.W))
    val output_valid     = Output(UInt(1.W))
    val output_encoded   = Output(UInt(log2Ceil(width).W))
    val output_unencoded = Output(UInt(width.W))
  })

  override def desiredName: String = "priority_encoder"
}
 */
class PriorityEncoder(val width: Int, val lsb_priority: String) extends Module {
  val io = IO(new Bundle {
    val input_unencoded  = Input(UInt(width.W))
    val output_valid     = Output(UInt(1.W))
    val output_encoded   = Output(UInt(log2Ceil(width).W))
    val output_unencoded = Output(UInt(width.W))
  })
  io.output_valid := io.input_unencoded.orR
  if(lsb_priority == "HIGH") {
    io.output_encoded := (width - 1).U
    for{i <- (0 until width).reverse } {
      when(io.input_unencoded(i) === 1.U) {
        io.output_encoded := i.U
      }
    }
  } else if(lsb_priority == "LOW") {
    io.output_encoded := 0.U
    for{i <- 0 until width} {
      when(io.input_unencoded(i) === 1.U) {
        io.output_encoded := i.U
      }
    }
  }
  io.output_unencoded := (1.U << io.output_encoded).asUInt
}