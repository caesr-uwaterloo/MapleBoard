 /* verilator lint_off WIDTH */

module ram_v2 #(
  parameter     ADDR_WIDTH    = 32,
  parameter     DATA_WIDTH    = 32,
  parameter     PR_LOOKUP_TABLE = 0
) (
  input                   clk  ,
  input                   rst  ,
  input  [ADDR_WIDTH-1:0] raddr,
  input                   re   ,
  input  [ADDR_WIDTH-1:0] waddr,
  input                   we   ,
  input  [DATA_WIDTH-1:0] din  ,
  output reg [DATA_WIDTH-1:0] dout
);

  (* ram_style = "block" *) reg [DATA_WIDTH-1:0] mem  [0:(1<<ADDR_WIDTH)-1];
  // reg [DATA_WIDTH-1:0] rdata                     ;
  logic log;
  // assign dout = mem[raddr];
  integer i;
  initial begin
    if(PR_LOOKUP_TABLE) begin
      for (i = 0; i < (1<<ADDR_WIDTH); i=i+1) begin
        // if(DATA_WIDTH > 32) begin
          mem[i] = i;
        // end else begin
        //   mem[i] = i;//[DATA_WIDTH-1:0];
        // end
      end
    end

  end

  always @(posedge clk) begin
    if(rst) begin
      dout <= {DATA_WIDTH{1'b0}};
    end else begin
      if (we) mem[waddr] <= din;
      if (re) dout <= mem[raddr];
    end
    //    log <= we;
  end

  // always @(posedge clk) if(log) display();

  integer idx;
  task display;
    begin
      $display("-----------------FIFO RAM---------------------");
      for(idx = 0; idx < ADDR_WIDTH; idx = idx + 1) begin
        $write("DATA[%0h]=", idx);
        $write("%h", mem[idx]);
        $write("\n");
      end
    end
  endtask

  task display_2;
    begin
      // $display("-----------------FIFO RAM---------------------");
       $write("DATA");
      for(idx = 0; idx < ADDR_WIDTH; idx = idx + 1) begin
        $write("[%0h]=", idx);
        $write("%h ", mem[idx]);
      end
      $write("\n");
    end
  endtask

endmodule
 /* verilator lint_on WIDTH */
