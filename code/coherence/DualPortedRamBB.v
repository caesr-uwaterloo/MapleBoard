
module DualPortedRamBB #(
 parameter addr_width = 0,
 parameter data_width = 0
)(
  input                  clock,
  input                  reset,
  input                  wen,
  input [addr_width-1:0] waddr,
  input [data_width-1:0] din,

  input                  ren,
  input [addr_width-1:0] raddr,
  output reg[data_width-1:0] dout
);
  (* ram_style = "ultra" *) reg[data_width-1:0] mem[0:(1 << addr_width)-1];
  always @(posedge clock) begin
    if(wen) begin
      mem[waddr] <= din;
    end
    dout <= mem[raddr];
    if(reset) begin
      dout <= 0;
    end
  end
endmodule
