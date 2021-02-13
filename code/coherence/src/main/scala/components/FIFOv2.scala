
package components

import chisel3._
import chisel3.util._
import params._
import chisel3.experimental._

class FIFOv2(val depthWidth: Int, val dataWidth: Int, val prLookupTable: Int) extends BlackBox(
  Map(
    "DEPTH_WIDTH" -> depthWidth,
    "DATA_WIDTH" -> dataWidth,
    "PR_LOOKUP_TABLE" -> prLookupTable
  )
) with HasBlackBoxInline {
  val io = IO(new Bundle {
    val  clk           = Input(Clock())
    val  rst           = Input(Reset())
    val  wr_data_i     = Input(UInt(dataWidth.W))
    val  wr_en_i       = Input(Bool())
    val  rd_data_o     = Output(UInt(dataWidth.W))
    val  rd_en_i       = Input(Bool())
    val  full_o        = Output(Bool())
    val  empty_o       = Output(Bool())
    val  one_left      = Output(Bool())
  })

  override def desiredName: String = "fifo_v2"
  setInline("fifo_v2.v",
    """
      |
      | /* verilator lint_off WIDTH */
      |
      |module ram_v2 #(
      |  parameter     ADDR_WIDTH    = 32,
      |  parameter     DATA_WIDTH    = 32,
      |  parameter     PR_LOOKUP_TABLE = 0
      |) (
      |  input                   clk  ,
      |  input                   rst  ,
      |  input  [ADDR_WIDTH-1:0] raddr,
      |  input                   re   ,
      |  input  [ADDR_WIDTH-1:0] waddr,
      |  input                   we   ,
      |  input  [DATA_WIDTH-1:0] din  ,
      |  output reg [DATA_WIDTH-1:0] dout
      |);
      |
      |  (* ram_style = "block" *) reg [DATA_WIDTH-1:0] mem  [0:(1<<ADDR_WIDTH)-1];
      |  // reg [DATA_WIDTH-1:0] rdata                     ;
      |  logic log;
      |  // assign dout = mem[raddr];
      |  integer i;
      |  initial begin
      |    if(PR_LOOKUP_TABLE) begin
      |      for (i = 0; i < (1<<ADDR_WIDTH); i=i+1) begin
      |        // if(DATA_WIDTH > 32) begin
      |          mem[i] = i;
      |        // end else begin
      |        //   mem[i] = i;//[DATA_WIDTH-1:0];
      |        // end
      |      end
      |    end
      |
      |  end
      |
      |  always @(posedge clk) begin
      |    if(rst) begin
      |      dout <= {DATA_WIDTH{1'b0}};
      |    end else begin
      |      if (we) mem[waddr] <= din;
      |      if (re) dout <= mem[raddr];
      |    end
      |    //    log <= we;
      |  end
      |
      |  // always @(posedge clk) if(log) display();
      |
      |  integer idx;
      |  task display;
      |    begin
      |      $display("-----------------FIFO RAM---------------------");
      |      for(idx = 0; idx < ADDR_WIDTH; idx = idx + 1) begin
      |        $write("DATA[%0h]=", idx);
      |        $write("%h", mem[idx]);
      |        $write("\n");
      |      end
      |    end
      |  endtask
      |
      |  task display_2;
      |    begin
      |      // $display("-----------------FIFO RAM---------------------");
      |       $write("DATA");
      |      for(idx = 0; idx < ADDR_WIDTH; idx = idx + 1) begin
      |        $write("[%0h]=", idx);
      |        $write("%h ", mem[idx]);
      |      end
      |      $write("\n");
      |    end
      |  endtask
      |
      |endmodule
      | /* verilator lint_on WIDTH */
      |
      |module fifo_v2 #(
      |  parameter   DEPTH_WIDTH = 32,
      |  parameter   DATA_WIDTH  = 32,
      |  parameter   PR_LOOKUP_TABLE = 0
      |) (
      |  input                   clk          ,
      |  input                   rst          ,
      |  input  [DATA_WIDTH-1:0] wr_data_i    ,
      |  input                   wr_en_i      ,
      |  output reg [DATA_WIDTH-1:0] rd_data_o    ,
      |  input                   rd_en_i      ,
      |  output                  full_o       ,
      |  output                  empty_o      ,
      |  output                  one_left
      |);
      |
      |  localparam DW = (DATA_WIDTH  < 1) ? 1 : DATA_WIDTH ;
      |  localparam AW = (DEPTH_WIDTH < 1) ? 1 : DEPTH_WIDTH;
      |
      |  // //synthesis translate_off
      |  // initial begin
      |  //    if(DEPTH_WIDTH < 1) $display("%m : Warning: DEPTH_WIDTH must be > 0. Setting minimum value (1)");
      |  //    if(DATA_WIDTH < 1) $display("%m : Warning: DATA_WIDTH must be > 0. Setting minimum value (1)");
      |  // end
      |  // //synthesis translate_on
      |
      |  reg [AW:0] write_pointer;
      |  reg [AW:0] read_pointer ;
      |
      |  reg [DATA_WIDTH-1:0] rd_data_ram, wr_data_reg;
      |  reg rd_en_reg, wr_en_reg, empty_reg;
      |
      |  wire empty_int     = (write_pointer[AW] == read_pointer[AW])        ;
      |  wire full_or_empty = (write_pointer[AW-1:0] == read_pointer[AW-1:0]);
      |
      |  assign full_o        = full_or_empty & !empty_int;
      |  assign empty_o       = full_or_empty & empty_int;
      |  assign one_left = ((read_pointer + 1) == write_pointer);
      |
      |  always @(posedge clk) begin
      |    if (rst) begin
      |      read_pointer  <= 0;
      |      write_pointer <= 0;
      |
      |      if(PR_LOOKUP_TABLE) begin
      |        write_pointer <= (1<< DEPTH_WIDTH) ;
      |      end
      |    end else begin
      |      if (wr_en_i & ~full_o)
      |        write_pointer <= write_pointer + 1'd1;
      |
      |      if (rd_en_i & ~empty_o)
      |        read_pointer <= read_pointer + 1'd1;
      |
      |
      |    end
      |  end
      |
      |  always @(posedge clk) begin
      |    if(rst) begin
      |      wr_data_reg <= '0;
      |      rd_en_reg <= '0;
      |      wr_en_reg <= '0;
      |      empty_reg <= '0;
      |    end else begin
      |      wr_data_reg <= wr_data_i;
      |      rd_en_reg <= rd_en_i;
      |      wr_en_reg <= wr_en_i;
      |      empty_reg <= empty_o;
      |    end
      |  end
      |
      |  always @* begin
      |    if(wr_en_reg & ~rd_en_reg & empty_reg) begin
      |      rd_data_o = wr_data_reg;
      |    end else
      |    if(empty_reg) begin
      |      rd_data_o = '0;
      |    end else begin
      |      rd_data_o = rd_data_ram;
      |    end
      |  end
      |
      |   task display_2;
      |    begin
      |      $write("WR_PTR=%h, RD_PTR=%h ", write_pointer, read_pointer);
      |     fifo_ram.display_2();
      |    end
      |  endtask
      |
      |  ram_v2 #(
      |    .ADDR_WIDTH   (AW),
      |    .DATA_WIDTH   (DW),
      |    .PR_LOOKUP_TABLE(PR_LOOKUP_TABLE)
      |  ) fifo_ram (
      |    .clk  (clk                  ),
      |    .rst  (rst                  ),
      |    .dout (rd_data_ram          ),
      |    .raddr(read_pointer[AW-1:0] ),
      |    .re   ('1              ),
      |    .waddr(write_pointer[AW-1:0]),
      |    .we   (wr_en_i              ),
      |    .din  (wr_data_i            )
      |  );
      |
      |endmodule
      |
      |""".stripMargin)
}
