
package components

import chisel3._
import chisel3.util.HasBlackBoxInline

// another version is to use ultra rams
object DualPortedRamBB {
  var id = 0
  def getID = {
    val res = id
    id = id + 1
    res
  }
}
class DualPortedRamBB(val addr_width: Int, val data_width: Int) extends BlackBox(
  Map(
    "addr_width" -> addr_width,
    "data_width" -> data_width
  )
) with HasBlackBoxInline {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val reset = Input(Reset())
    val wen = Input(Bool())
    val waddr = Input(UInt(addr_width.W))
    val din = Input(UInt(data_width.W))

    val ren = Input(Bool())
    val raddr = Input(UInt(addr_width.W))
    val dout = Output(UInt(data_width.W))
  })
  setInline("DualPortedRamBB.v",
    s"""
       |module DualPortedRamBB #(
       | parameter addr_width = 0,
       | parameter data_width = 0
       |)(
       |  input                  clock,
       |  input                  reset,
       |  input                  wen,
       |  input [addr_width-1:0] waddr,
       |  input [data_width-1:0] din,
       |
       |  input                  ren,
       |  input [addr_width-1:0] raddr,
       |  output reg[data_width-1:0] dout
       |);
       |  (* ram_style = "ultra" *) reg[data_width-1:0] mem[0:(1 << addr_width)-1];
       |  always @(posedge clock) begin
       |    if(wen) begin
       |      mem[waddr] <= din;
       |    end
       |    dout <= mem[raddr];
       |    if(reset) begin
       |      dout <= 0;
       |    end
       |  end
       |endmodule
       |""".stripMargin)
}
class DualPortedRam[T <: Data](val genT: T, val depth: Int) extends Module {
  val io = IO(new Bundle {
    val wen = Input(Bool())
    val waddr = Input(UIntHolding(depth))
    val din = Input(genT)

    val ren = Input(Bool())
    val raddr = Input(UIntHolding(depth))
    val dout = Output(genT)
  })
  // val mem = SyncReadMem(depth, genT)
  // when(io.wen) {
  //   mem.write(io.waddr, io.din)
  // }
  // io.dout := mem.read(io.raddr, io.ren)
  val mem = Module(new DualPortedRamBB(util.log2Ceil(depth), genT.getWidth))
  mem.io.clock := clock
  mem.io.reset := reset
  mem.io.wen := io.wen
  mem.io.waddr := io.waddr
  mem.io.din := io.din.asUInt
  mem.io.ren := io.ren
  mem.io.raddr := io.raddr
  io.dout := mem.io.dout.asTypeOf(genT)
}

