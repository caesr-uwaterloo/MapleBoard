
package components

import chisel3._
import chisel3.util._
import params._
import chisel3.experimental._

class DataArray(val cacheParams: SimpleCacheParams) extends BlackBox(
  Map(
    "ID" -> "ID",
    "data_width" -> cacheParams.lineWidth,
    "data_depth" -> cacheParams.nSets
  )
) with HasBlackBoxInline {

  val io = IO(new Bundle {
    val reset        = Input(Reset())
    val clock        = Input(Clock())
    val read_en      = Input(Bool())
    val read_addr    = Input(UInt(cacheParams.lineAddrWidth.W))
    val write_en     = Input(Bool())
    val write_addr   = Input(UInt(cacheParams.lineAddrWidth.W))
    val write_data   = Input(UInt(cacheParams.lineWidth.W))
    val write_byte_en= Input(UInt(cacheParams.lineBytes.W))
    val read_data    = Output(UInt(cacheParams.lineWidth.W))
    val clock_b      = Input(Clock())
    val read_addr_b  = Input(UInt(cacheParams.lineAddrWidth.W))
    val read_data_b  = Output(UInt(cacheParams.lineWidth.W))
  })
  override def desiredName = "data_array"
  setInline("cache_data_array.v",
    """
      |// data_array module
      |module data_array #(
      |      parameter ID = 0,
      |      parameter data_width = 512,
      |      parameter data_depth = 16
      |  )(
      |  input                                 reset        ,
      |  input                                 clock        ,
      |  input                                 read_en      ,
      |  input        [$clog2(data_depth)-1:0] read_addr    ,
      |  input                                 write_en     ,
      |  input        [$clog2(data_depth)-1:0] write_addr   ,
      |  input        [        data_width-1:0] write_data   ,
      |  input        [      data_width/8-1:0] write_byte_en, // this component will be set according to the write_addr
      |  output logic [        data_width-1:0] read_data    ,
      |  //Port B Reading only
      |  input                                 clock_b      ,
      |  input        [$clog2(data_depth)-1:0] read_addr_b  ,
      |  output logic [        data_width-1:0] read_data_b
      |);
      |  // TODO: move the parameters out to the defines.vh
      |
      |
      |  // TODO: replace this with memory other than latch
      |  (* ram_style = "block" *) logic[data_width-1:0] data[0:data_depth-1];
      |
      |  integer i;
      |  always @ (posedge clock) begin
      |    if (write_en) begin
      |      for (i=0; i < (data_width/8); i=i+1)
      |        begin
      |          if (write_byte_en[i]) begin
      |            data[write_addr][(i*8)+:8] <= write_data[(i*8) +: 8];
      |          end
      |        end
      |    end
      |
      |    if (read_en) begin
      |        read_data <= data[read_addr];
      |    end
      |  end
      |
      |  // logic write_en_reg ;
      |  // always @(posedge clock ) begin : displ
      |  //   write_en_reg <= write_en;
      |  //   if(write_en_reg || write_en) begin
      |  //     display();
      |  //   end
      |  // end
      |
      |  // Port B
      |  always @(posedge clock_b) begin
      |    read_data_b <= data[read_addr_b];
      |  end
      |
      |//  initial begin
      |//    integer i;
      |//    for (i = 0; i < 16; i = i + 1) begin
      |//      data[i] = {16{32'h1*i}};
      |//    end
      |//  end
      |
      |  task display;
      |    integer idx = 0;
      |    integer j = 0;
      |    begin
      |      if(write_en || read_en) begin
      |        $display("[CC%0d] ------------------- DATA ARRAY -------------------", ID);
      |        $display("[CC%0d] write_en:%0d\twrite_addr:%0h \nwrite_data:%0h\nwrite_byte_en:%0b",ID , write_en, write_addr, write_data, write_byte_en);
      |        for(idx = 0; idx < data_depth; idx = idx + 1) begin
      |          $write("[CC%0d] DATA[%0h]=",ID , idx);
      |          $write("%h", data[idx]);
      |          $write("\n");
      |        end
      |      end // if(write_en || read_en)
      |    end
      |  endtask
      |endmodule // data_array
      |
      |""".stripMargin)
}
