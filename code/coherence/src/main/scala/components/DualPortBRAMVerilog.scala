
package components

import chisel3._
import chisel3.experimental._
import chisel3.util._

class DualPortBRAMVerilog(val depth: Int, val width: Int) extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val reset = Input(UInt(1.W))
    val raddr = Input(UInt(log2Ceil(depth).W))
    val rdata = Output(UInt(width.W))
    val we    = Input(UInt(1.W))
    val waddr = Input(UInt(log2Ceil(depth).W))
    val wdata = Input(UInt(width.W))
  })
  require(width % 8 == 0)
  // trying to infer dual ported byte enabled BRAM
  setInline("DualPortBRAMVerilog.v",
    s"""
       |module DualPortBRAMVerilog(
       |  input  clock,
       |  input  reset,
       |  input  [${log2Ceil(depth)-1}:0] raddr1,
       |  output [${width-1}:0]           rdata1,
       |  input  [${log2Ceil(depth)-1}:0] raddr2,
       |  output [${width-1}:0]           rdata2,
       |  input  [${width/8-1}:0]         we1,
       |  input  [${log2Ceil(depth)-1}:0] waddr1,
       |  input  [${width-1}:0]           wdata1,
       |  input  [${width/8-1}:0]         we2,
       |  input  [${log2Ceil(depth)-1}:0] waddr2,
       |  input  [${width-1}:0]           wdata2
       |  );
       |  (* ram_style = "block" *) reg [${width-1}:0] mem [0:${depth-1}];
       |  assign rdata1 = mem[raddr1];
       |  assign rdata2 = mem[raddr2];
       |  generate
       |    genvar i;
       |    for (i = 0; i < ${width/8}; i = i+1) begin: byte_write
       |      always @(posedge clock)
       |        if (we1[i])
       |          mem[waddr1][(i+1)*8-1:i*8] <= wdata1[(i+1)*8-1:i*8];
       |      always @(posedge clock)
       |        if (we2[i])
       |          mem[waddr2][(i+1)*8-1:i*8] <= wdata2[(i+1)*8-1:i*8];
       |    end
       |  endgenerate
       |endmodule
     """.stripMargin
  )
}
