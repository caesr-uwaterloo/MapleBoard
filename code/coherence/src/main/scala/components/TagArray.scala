
package components

import chisel3._
import chisel3.util._
import params._
import chisel3.experimental._

class TagArray(val cacheParams: SimpleCacheParams) extends BlackBox (
  Map(
    "ID" -> "ID",
    "data_width" -> cacheParams.tagWidth,
    "data_depth" -> cacheParams.nSets
  )
) with HasBlackBoxInline {
  override def desiredName: String = "tag_array"
  val io = IO(new Bundle {
    val reset = Input(Reset())
    val clock = Input(Clock())
    val read_en = Input(Bool())
    val read_addr = Input(UInt(cacheParams.lineAddrWidth.W))
    val read_data = Output(UInt(cacheParams.tagWidth.W))
    val write_en = Input(Bool())
    val write_addr = Input(UInt(cacheParams.lineAddrWidth.W))
    val write_data = Input(UInt(cacheParams.tagWidth.W))
    val clock_b = Input(Clock())
    val read_addr_b = Input(UInt(cacheParams.lineAddrWidth.W))
    val read_data_b = Output(UInt(cacheParams.tagWidth.W))
  })
  setInline("cache_tag_array.v",
    """
      |// tag array module
      |module tag_array #(
      |  parameter ID = 0,
      |  parameter data_width = 28,
      |  parameter data_depth = 16
      |) (
      |  input                                 reset      ,
      |  input                                 clock      ,
      |  input                                 read_en    ,
      |  input        [$clog2(data_depth)-1:0] read_addr  ,
      |  input                                 write_en   ,
      |  input        [$clog2(data_depth)-1:0] write_addr ,
      |  input        [        data_width-1:0] write_data ,
      |  output       [        data_width-1:0] read_data  ,
      |
      |  input                                 clock_b    ,
      |  input        [$clog2(data_depth)-1:0] read_addr_b,
      |  output logic [        data_width-1:0] read_data_b
      |);
      |
      |  (* ram_style = "block" *) logic[data_width-1:0] data[0:data_depth-1];
      |
      |  assign read_data = read_en ? data[read_addr] : '0;
      |
      |  // TODO: replace this with memory other than latch
      |  always @(posedge clock) begin
      |    if(reset) begin
      |      /* code */
      |    end else begin
      |      if(write_en) begin
      |        data[write_addr] <= write_data;
      |      end
      |    end
      |  end
      |
      |  // Port B
      |  always @(posedge clock_b) begin
      |    read_data_b <= data[read_addr_b];
      |  end
      |
      |  // always @* if(write_en || read_en) display();
      |
      |  task display;
      |    integer idx;
      |    begin
      |      if(write_en || read_en) begin
      |        $display("[CC%0d]------------------- TAG ARRAY  -------------------", ID);
      |        for(idx = 0; idx < data_depth; idx = idx + 1) begin
      |          $write("[CC%0d] TAG [%0h]=", ID, idx);
      |          $write(" %h", data[idx]);
      |          $write("\n");
      |        end
      |      end
      |    end
      |  endtask
      |
      |  function [data_width-1:0] get_value_at_addr;
      |    input [$clog2(data_depth)-1:0] addr;
      |    begin
      |      get_value_at_addr = data[addr];
      |    end
      |  endfunction
      |
      |endmodule // tag_array
      |
      |""".stripMargin)
}
