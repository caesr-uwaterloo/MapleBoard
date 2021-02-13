
package components

import chisel3._
import chisel3.experimental._
import chisel3.util._

class BRAMVerilog(val depth: Int, val width: Int) extends BlackBox(
  Map(
    "DATA_WIDTH" -> width,
    "DATA_DEPTH" -> depth
  )
) with HasBlackBoxInline {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val reset = Input(Reset())
    val raddr = Input(UInt(log2Ceil(depth).W))
    val rdata = Output(UInt(width.W))
    val we    = Input(UInt(1.W))
    val waddr = Input(UInt(log2Ceil(depth).W))
    val wdata = Input(UInt(width.W))
  })
  setInline("BRAMVerilog.v",
    s"""
       |module BRAMVerilog #(
       |  parameter DATA_WIDTH = 32,
       |  parameter DATA_DEPTH = 16
       |)(
       |  input  clock,
       |  input  reset,
       |  input  [${"$clog2(DATA_DEPTH) - 1"}:0] raddr,
       |  output [DATA_WIDTH - 1:0]           rdata,
       |  input                           we,
       |  input  [${"$clog2(DATA_DEPTH) - 1"}:0] waddr,
       |  input  [DATA_WIDTH - 1:0]           wdata
       |  );
       |  (* ram_style = "block" *) reg [DATA_WIDTH - 1:0] mem [0:DATA_DEPTH - 1];
       |  assign rdata = mem[raddr];
       |  integer i;
       |  initial begin
       |    for(i = 0; i < DATA_DEPTH; i = i + 1) begin
       |      mem[i] = 0;
       |    end
       |  end
       |  always @(posedge clock) begin
       |    if(we) begin
       |      mem[waddr] <= wdata;
       |    end
       |  end
       |endmodule
     """.stripMargin
  )
}
class ByteEnableBRAMVerilog(val depth: Int, val width: Int) extends BlackBox(
  Map(
    "DATA_WIDTH" -> width,
    "DATA_DEPTH" -> depth
  )
) with HasBlackBoxInline {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val reset = Input(Reset())
    val raddr = Input(UInt(log2Ceil(depth).W))
    val rdata = Output(UInt(width.W))
    val ena   = Input(UInt(1.W))
    val we    = Input(UInt((width / 8).W))
    val waddr = Input(UInt(log2Ceil(depth).W))
    val wdata = Input(UInt(width.W))
  })
  assert(width % 8 == 0)
  setInline("ByteEnableBRAMVerilog.v",
    s"""
       |module ByteEnableBRAMVerilog #(
       |  parameter DATA_DEPTH = 16,
       |  parameter DATA_WIDTH = 16
       |)(
       |  input  clock,
       |  input  reset,
       |  input  [$$clog2(DATA_DEPTH)-1:0] raddr,
       |  output [DATA_WIDTH-1:0]           rdata,
       |  input                           ena,
       |  input  [DATA_WIDTH/8-1:0]         we,
       |  input  [$$clog2(DATA_DEPTH)-1:0] waddr,
       |  input  [DATA_WIDTH-1:0]           wdata
       |  );
       |  (* ram_style = "block" *) reg [DATA_WIDTH-1:0] mem [0:DATA_DEPTH-1];
       |  assign rdata = mem[raddr];
       |  generate
       |  genvar i;
       |    for(i = 0; i < DATA_WIDTH / 8; i = i + 1)begin
       |      always @(posedge clock) begin
       |        if(ena) begin
       |          if(we[i]) begin
       |            mem[waddr][(i+1)*8-1:i*8] <= wdata[(i+1)*8-1:i*8];
       |          end
       |        end
       |      end
       |    end
       |  endgenerate
       |endmodule
     """.stripMargin
  )
}
