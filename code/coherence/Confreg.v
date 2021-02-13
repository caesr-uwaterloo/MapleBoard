module Confreg(
  input         clock,
  input         reset,
  input  [63:0] s_axi_awaddr,
  input  [2:0]  s_axi_awprot,
  input         s_axi_awvalid,
  output        s_axi_awready,
  input  [63:0] s_axi_wdata,
  input  [7:0]  s_axi_wstrb,
  input         s_axi_wvalid,
  output        s_axi_wready,
  output [1:0]  s_axi_bresp,
  output        s_axi_bvalid,
  input         s_axi_bready,
  input  [63:0] s_axi_araddr,
  input  [2:0]  s_axi_arprot,
  input         s_axi_arvalid,
  output        s_axi_arready,
  output [63:0] s_axi_rdata,
  output [1:0]  s_axi_rresp,
  output        s_axi_rvalid,
  input         s_axi_rready,
  output [63:0] reg_initPC,
  output [63:0] reg_resetReg
);
  reg [63:0] initPC; // @[Confreg.scala 26:23]
  reg [63:0] _RAND_0;
  reg [63:0] resetReg; // @[Confreg.scala 27:25]
  reg [63:0] _RAND_1;
  reg [63:0] addr; // @[Confreg.scala 28:21]
  reg [63:0] _RAND_2;
  reg [1:0] state; // @[Confreg.scala 29:22]
  reg [31:0] _RAND_3;
  wire  _T_2; // @[Conditional.scala 37:30]
  wire  _T_4; // @[package.scala 330:43]
  wire  _T_5; // @[package.scala 333:43]
  wire  _T_8; // @[Conditional.scala 37:30]
  wire  _T_9; // @[Confreg.scala 54:17]
  wire  _T_10; // @[Confreg.scala 57:23]
  wire [63:0] _GEN_5; // @[Confreg.scala 57:42]
  wire [63:0] _GEN_7; // @[Confreg.scala 54:36]
  wire  _T_11; // @[package.scala 334:41]
  wire  _T_14; // @[Conditional.scala 37:30]
  wire  _T_15; // @[package.scala 331:41]
  wire  _T_20; // @[Conditional.scala 37:30]
  wire  _T_21; // @[package.scala 332:41]
  wire  _GEN_23; // @[Conditional.scala 39:67]
  wire [63:0] _GEN_27; // @[Conditional.scala 39:67]
  wire  _GEN_29; // @[Conditional.scala 39:67]
  wire  _GEN_32; // @[Conditional.scala 39:67]
  assign _T_2 = 2'h0 == state; // @[Conditional.scala 37:30]
  assign _T_4 = s_axi_awready & s_axi_awvalid; // @[package.scala 330:43]
  assign _T_5 = s_axi_arready & s_axi_arvalid; // @[package.scala 333:43]
  assign _T_8 = 2'h2 == state; // @[Conditional.scala 37:30]
  assign _T_9 = addr == 64'h80003000; // @[Confreg.scala 54:17]
  assign _T_10 = addr == 64'h80000000; // @[Confreg.scala 57:23]
  assign _GEN_5 = _T_10 ? resetReg : 64'hffffffffffffffff; // @[Confreg.scala 57:42]
  assign _GEN_7 = _T_9 ? initPC : _GEN_5; // @[Confreg.scala 54:36]
  assign _T_11 = s_axi_rready & s_axi_rvalid; // @[package.scala 334:41]
  assign _T_14 = 2'h1 == state; // @[Conditional.scala 37:30]
  assign _T_15 = s_axi_wready & s_axi_wvalid; // @[package.scala 331:41]
  assign _T_20 = 2'h3 == state; // @[Conditional.scala 37:30]
  assign _T_21 = s_axi_bready & s_axi_bvalid; // @[package.scala 332:41]
  assign _GEN_23 = _T_14 ? 1'h0 : _T_20; // @[Conditional.scala 39:67]
  assign _GEN_27 = _T_8 ? _GEN_7 : 64'h0; // @[Conditional.scala 39:67]
  assign _GEN_29 = _T_8 ? 1'h0 : _T_14; // @[Conditional.scala 39:67]
  assign _GEN_32 = _T_8 ? 1'h0 : _GEN_23; // @[Conditional.scala 39:67]
  assign s_axi_awready = 2'h0 == state; // @[Confreg.scala 32:17 Confreg.scala 42:21]
  assign s_axi_wready = _T_2 ? 1'h0 : _GEN_29; // @[Confreg.scala 35:16 Confreg.scala 69:20]
  assign s_axi_bresp = 2'h0; // @[Confreg.scala 38:15 Confreg.scala 81:19]
  assign s_axi_bvalid = _T_2 ? 1'h0 : _GEN_32; // @[Confreg.scala 36:16 Confreg.scala 80:20]
  assign s_axi_arready = _T_2 & ~s_axi_awvalid; // @[Confreg.scala 33:17 Confreg.scala 43:21]
  assign s_axi_rdata = _T_2 ? 64'h0 : _GEN_27; // @[Confreg.scala 39:15 Confreg.scala 56:21 Confreg.scala 59:21 Confreg.scala 62:21]
  assign s_axi_rresp = 2'h0; // @[Confreg.scala 37:15 Confreg.scala 55:21 Confreg.scala 58:21 Confreg.scala 61:21]
  assign s_axi_rvalid = _T_2 ? 1'h0 : _T_8; // @[Confreg.scala 34:16 Confreg.scala 53:20]
  assign reg_initPC = initPC; // @[Confreg.scala 30:14]
  assign reg_resetReg = resetReg; // @[Confreg.scala 31:16]
`ifdef RANDOMIZE_GARBAGE_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_INVALID_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_REG_INIT
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_MEM_INIT
`define RANDOMIZE
`endif
`ifndef RANDOM
`define RANDOM $random
`endif
`ifdef RANDOMIZE_MEM_INIT
  integer initvar;
`endif
`ifndef SYNTHESIS
initial begin
  `ifdef RANDOMIZE
    `ifdef INIT_RANDOM
      `INIT_RANDOM
    `endif
    `ifndef VERILATOR
      `ifdef RANDOMIZE_DELAY
        #`RANDOMIZE_DELAY begin end
      `else
        #0.002 begin end
      `endif
    `endif
  `ifdef RANDOMIZE_REG_INIT
  _RAND_0 = {2{`RANDOM}};
  initPC = _RAND_0[63:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_1 = {2{`RANDOM}};
  resetReg = _RAND_1[63:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_2 = {2{`RANDOM}};
  addr = _RAND_2[63:0];
  `endif // RANDOMIZE_REG_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_3 = {1{`RANDOM}};
  state = _RAND_3[1:0];
  `endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`endif // SYNTHESIS
  always @(posedge clock) begin
    if (reset) begin
      initPC <= 64'h0;
    end else if (!(_T_2)) begin
      if (!(_T_8)) begin
        if (_T_14) begin
          if (_T_15) begin
            if (_T_9) begin
              initPC <= s_axi_wdata;
            end
          end
        end
      end
    end
    if (reset) begin
      resetReg <= 64'h0;
    end else if (!(_T_2)) begin
      if (!(_T_8)) begin
        if (_T_14) begin
          if (_T_15) begin
            if (!(_T_9)) begin
              if (_T_10) begin
                resetReg <= s_axi_wdata;
              end
            end
          end
        end
      end
    end
    if (reset) begin
      addr <= 64'h0;
    end else if (_T_2) begin
      if (_T_4) begin
        addr <= s_axi_awaddr;
      end else if (_T_5) begin
        addr <= s_axi_awaddr;
      end
    end
    if (reset) begin
      state <= 2'h0;
    end else if (_T_2) begin
      if (_T_4) begin
        state <= 2'h1;
      end else if (_T_5) begin
        state <= 2'h2;
      end
    end else if (_T_8) begin
      if (_T_11) begin
        state <= 2'h0;
      end
    end else if (_T_14) begin
      if (_T_15) begin
        state <= 2'h3;
      end
    end else if (_T_20) begin
      if (_T_21) begin
        state <= 2'h0;
      end
    end
  end
endmodule
