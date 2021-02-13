module Queue(
  input         clock,
  input         reset,
  output        io_enq_ready,
  input         io_enq_valid,
  input  [63:0] io_enq_bits_addr,
  input  [2:0]  io_enq_bits_prot,
  input         io_deq_ready,
  output        io_deq_valid,
  output [63:0] io_deq_bits_addr,
  output [2:0]  io_deq_bits_prot
);
  reg [63:0] _T_addr [0:0]; // @[Decoupled.scala 209:24]
  reg [63:0] _RAND_0;
  wire [63:0] _T_addr__T_14_data; // @[Decoupled.scala 209:24]
  wire  _T_addr__T_14_addr; // @[Decoupled.scala 209:24]
  wire [63:0] _T_addr__T_10_data; // @[Decoupled.scala 209:24]
  wire  _T_addr__T_10_addr; // @[Decoupled.scala 209:24]
  wire  _T_addr__T_10_mask; // @[Decoupled.scala 209:24]
  wire  _T_addr__T_10_en; // @[Decoupled.scala 209:24]
  reg [2:0] _T_prot [0:0]; // @[Decoupled.scala 209:24]
  reg [31:0] _RAND_1;
  wire [2:0] _T_prot__T_14_data; // @[Decoupled.scala 209:24]
  wire  _T_prot__T_14_addr; // @[Decoupled.scala 209:24]
  wire [2:0] _T_prot__T_10_data; // @[Decoupled.scala 209:24]
  wire  _T_prot__T_10_addr; // @[Decoupled.scala 209:24]
  wire  _T_prot__T_10_mask; // @[Decoupled.scala 209:24]
  wire  _T_prot__T_10_en; // @[Decoupled.scala 209:24]
  reg  _T_1; // @[Decoupled.scala 212:35]
  reg [31:0] _RAND_2;
  wire  _T_6; // @[Decoupled.scala 40:37]
  wire  _T_8; // @[Decoupled.scala 40:37]
  wire  _T_11; // @[Decoupled.scala 227:16]
  assign _T_addr__T_14_addr = 1'h0;
  assign _T_addr__T_14_data = _T_addr[_T_addr__T_14_addr]; // @[Decoupled.scala 209:24]
  assign _T_addr__T_10_data = io_enq_bits_addr;
  assign _T_addr__T_10_addr = 1'h0;
  assign _T_addr__T_10_mask = 1'h1;
  assign _T_addr__T_10_en = io_enq_ready & io_enq_valid;
  assign _T_prot__T_14_addr = 1'h0;
  assign _T_prot__T_14_data = _T_prot[_T_prot__T_14_addr]; // @[Decoupled.scala 209:24]
  assign _T_prot__T_10_data = io_enq_bits_prot;
  assign _T_prot__T_10_addr = 1'h0;
  assign _T_prot__T_10_mask = 1'h1;
  assign _T_prot__T_10_en = io_enq_ready & io_enq_valid;
  assign _T_6 = io_enq_ready & io_enq_valid; // @[Decoupled.scala 40:37]
  assign _T_8 = io_deq_ready & io_deq_valid; // @[Decoupled.scala 40:37]
  assign _T_11 = _T_6 != _T_8; // @[Decoupled.scala 227:16]
  assign io_enq_ready = ~_T_1; // @[Decoupled.scala 232:16]
  assign io_deq_valid = _T_1; // @[Decoupled.scala 231:16]
  assign io_deq_bits_addr = _T_addr__T_14_data; // @[Decoupled.scala 233:15]
  assign io_deq_bits_prot = _T_prot__T_14_data; // @[Decoupled.scala 233:15]
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
  _RAND_0 = {2{`RANDOM}};
  `ifdef RANDOMIZE_MEM_INIT
  for (initvar = 0; initvar < 1; initvar = initvar+1)
    _T_addr[initvar] = _RAND_0[63:0];
  `endif // RANDOMIZE_MEM_INIT
  _RAND_1 = {1{`RANDOM}};
  `ifdef RANDOMIZE_MEM_INIT
  for (initvar = 0; initvar < 1; initvar = initvar+1)
    _T_prot[initvar] = _RAND_1[2:0];
  `endif // RANDOMIZE_MEM_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_2 = {1{`RANDOM}};
  _T_1 = _RAND_2[0:0];
  `endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`endif // SYNTHESIS
  always @(posedge clock) begin
    if(_T_addr__T_10_en & _T_addr__T_10_mask) begin
      _T_addr[_T_addr__T_10_addr] <= _T_addr__T_10_data; // @[Decoupled.scala 209:24]
    end
    if(_T_prot__T_10_en & _T_prot__T_10_mask) begin
      _T_prot[_T_prot__T_10_addr] <= _T_prot__T_10_data; // @[Decoupled.scala 209:24]
    end
    if (reset) begin
      _T_1 <= 1'h0;
    end else if (_T_11) begin
      _T_1 <= _T_6;
    end
  end
endmodule
module Queue_2(
  input         clock,
  input         reset,
  output        io_enq_ready,
  input         io_enq_valid,
  input  [63:0] io_enq_bits_data,
  input  [1:0]  io_enq_bits_rresp,
  input         io_deq_ready,
  output        io_deq_valid,
  output [63:0] io_deq_bits_data,
  output [1:0]  io_deq_bits_rresp
);
  reg [63:0] _T_data [0:0]; // @[Decoupled.scala 209:24]
  reg [63:0] _RAND_0;
  wire [63:0] _T_data__T_14_data; // @[Decoupled.scala 209:24]
  wire  _T_data__T_14_addr; // @[Decoupled.scala 209:24]
  wire [63:0] _T_data__T_10_data; // @[Decoupled.scala 209:24]
  wire  _T_data__T_10_addr; // @[Decoupled.scala 209:24]
  wire  _T_data__T_10_mask; // @[Decoupled.scala 209:24]
  wire  _T_data__T_10_en; // @[Decoupled.scala 209:24]
  reg [1:0] _T_rresp [0:0]; // @[Decoupled.scala 209:24]
  reg [31:0] _RAND_1;
  wire [1:0] _T_rresp__T_14_data; // @[Decoupled.scala 209:24]
  wire  _T_rresp__T_14_addr; // @[Decoupled.scala 209:24]
  wire [1:0] _T_rresp__T_10_data; // @[Decoupled.scala 209:24]
  wire  _T_rresp__T_10_addr; // @[Decoupled.scala 209:24]
  wire  _T_rresp__T_10_mask; // @[Decoupled.scala 209:24]
  wire  _T_rresp__T_10_en; // @[Decoupled.scala 209:24]
  reg  _T_1; // @[Decoupled.scala 212:35]
  reg [31:0] _RAND_2;
  wire  _T_6; // @[Decoupled.scala 40:37]
  wire  _T_8; // @[Decoupled.scala 40:37]
  wire  _T_11; // @[Decoupled.scala 227:16]
  assign _T_data__T_14_addr = 1'h0;
  assign _T_data__T_14_data = _T_data[_T_data__T_14_addr]; // @[Decoupled.scala 209:24]
  assign _T_data__T_10_data = io_enq_bits_data;
  assign _T_data__T_10_addr = 1'h0;
  assign _T_data__T_10_mask = 1'h1;
  assign _T_data__T_10_en = io_enq_ready & io_enq_valid;
  assign _T_rresp__T_14_addr = 1'h0;
  assign _T_rresp__T_14_data = _T_rresp[_T_rresp__T_14_addr]; // @[Decoupled.scala 209:24]
  assign _T_rresp__T_10_data = io_enq_bits_rresp;
  assign _T_rresp__T_10_addr = 1'h0;
  assign _T_rresp__T_10_mask = 1'h1;
  assign _T_rresp__T_10_en = io_enq_ready & io_enq_valid;
  assign _T_6 = io_enq_ready & io_enq_valid; // @[Decoupled.scala 40:37]
  assign _T_8 = io_deq_ready & io_deq_valid; // @[Decoupled.scala 40:37]
  assign _T_11 = _T_6 != _T_8; // @[Decoupled.scala 227:16]
  assign io_enq_ready = ~_T_1; // @[Decoupled.scala 232:16]
  assign io_deq_valid = _T_1; // @[Decoupled.scala 231:16]
  assign io_deq_bits_data = _T_data__T_14_data; // @[Decoupled.scala 233:15]
  assign io_deq_bits_rresp = _T_rresp__T_14_data; // @[Decoupled.scala 233:15]
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
  _RAND_0 = {2{`RANDOM}};
  `ifdef RANDOMIZE_MEM_INIT
  for (initvar = 0; initvar < 1; initvar = initvar+1)
    _T_data[initvar] = _RAND_0[63:0];
  `endif // RANDOMIZE_MEM_INIT
  _RAND_1 = {1{`RANDOM}};
  `ifdef RANDOMIZE_MEM_INIT
  for (initvar = 0; initvar < 1; initvar = initvar+1)
    _T_rresp[initvar] = _RAND_1[1:0];
  `endif // RANDOMIZE_MEM_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_2 = {1{`RANDOM}};
  _T_1 = _RAND_2[0:0];
  `endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`endif // SYNTHESIS
  always @(posedge clock) begin
    if(_T_data__T_10_en & _T_data__T_10_mask) begin
      _T_data[_T_data__T_10_addr] <= _T_data__T_10_data; // @[Decoupled.scala 209:24]
    end
    if(_T_rresp__T_10_en & _T_rresp__T_10_mask) begin
      _T_rresp[_T_rresp__T_10_addr] <= _T_rresp__T_10_data; // @[Decoupled.scala 209:24]
    end
    if (reset) begin
      _T_1 <= 1'h0;
    end else if (_T_11) begin
      _T_1 <= _T_6;
    end
  end
endmodule
module Queue_3(
  input         clock,
  input         reset,
  output        io_enq_ready,
  input         io_enq_valid,
  input  [63:0] io_enq_bits_data,
  input  [7:0]  io_enq_bits_strb,
  input         io_deq_ready,
  output        io_deq_valid,
  output [63:0] io_deq_bits_data,
  output [7:0]  io_deq_bits_strb
);
  reg [63:0] _T_data [0:0]; // @[Decoupled.scala 209:24]
  reg [63:0] _RAND_0;
  wire [63:0] _T_data__T_14_data; // @[Decoupled.scala 209:24]
  wire  _T_data__T_14_addr; // @[Decoupled.scala 209:24]
  wire [63:0] _T_data__T_10_data; // @[Decoupled.scala 209:24]
  wire  _T_data__T_10_addr; // @[Decoupled.scala 209:24]
  wire  _T_data__T_10_mask; // @[Decoupled.scala 209:24]
  wire  _T_data__T_10_en; // @[Decoupled.scala 209:24]
  reg [7:0] _T_strb [0:0]; // @[Decoupled.scala 209:24]
  reg [31:0] _RAND_1;
  wire [7:0] _T_strb__T_14_data; // @[Decoupled.scala 209:24]
  wire  _T_strb__T_14_addr; // @[Decoupled.scala 209:24]
  wire [7:0] _T_strb__T_10_data; // @[Decoupled.scala 209:24]
  wire  _T_strb__T_10_addr; // @[Decoupled.scala 209:24]
  wire  _T_strb__T_10_mask; // @[Decoupled.scala 209:24]
  wire  _T_strb__T_10_en; // @[Decoupled.scala 209:24]
  reg  _T_1; // @[Decoupled.scala 212:35]
  reg [31:0] _RAND_2;
  wire  _T_6; // @[Decoupled.scala 40:37]
  wire  _T_8; // @[Decoupled.scala 40:37]
  wire  _T_11; // @[Decoupled.scala 227:16]
  assign _T_data__T_14_addr = 1'h0;
  assign _T_data__T_14_data = _T_data[_T_data__T_14_addr]; // @[Decoupled.scala 209:24]
  assign _T_data__T_10_data = io_enq_bits_data;
  assign _T_data__T_10_addr = 1'h0;
  assign _T_data__T_10_mask = 1'h1;
  assign _T_data__T_10_en = io_enq_ready & io_enq_valid;
  assign _T_strb__T_14_addr = 1'h0;
  assign _T_strb__T_14_data = _T_strb[_T_strb__T_14_addr]; // @[Decoupled.scala 209:24]
  assign _T_strb__T_10_data = io_enq_bits_strb;
  assign _T_strb__T_10_addr = 1'h0;
  assign _T_strb__T_10_mask = 1'h1;
  assign _T_strb__T_10_en = io_enq_ready & io_enq_valid;
  assign _T_6 = io_enq_ready & io_enq_valid; // @[Decoupled.scala 40:37]
  assign _T_8 = io_deq_ready & io_deq_valid; // @[Decoupled.scala 40:37]
  assign _T_11 = _T_6 != _T_8; // @[Decoupled.scala 227:16]
  assign io_enq_ready = ~_T_1; // @[Decoupled.scala 232:16]
  assign io_deq_valid = _T_1; // @[Decoupled.scala 231:16]
  assign io_deq_bits_data = _T_data__T_14_data; // @[Decoupled.scala 233:15]
  assign io_deq_bits_strb = _T_strb__T_14_data; // @[Decoupled.scala 233:15]
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
  _RAND_0 = {2{`RANDOM}};
  `ifdef RANDOMIZE_MEM_INIT
  for (initvar = 0; initvar < 1; initvar = initvar+1)
    _T_data[initvar] = _RAND_0[63:0];
  `endif // RANDOMIZE_MEM_INIT
  _RAND_1 = {1{`RANDOM}};
  `ifdef RANDOMIZE_MEM_INIT
  for (initvar = 0; initvar < 1; initvar = initvar+1)
    _T_strb[initvar] = _RAND_1[7:0];
  `endif // RANDOMIZE_MEM_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_2 = {1{`RANDOM}};
  _T_1 = _RAND_2[0:0];
  `endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`endif // SYNTHESIS
  always @(posedge clock) begin
    if(_T_data__T_10_en & _T_data__T_10_mask) begin
      _T_data[_T_data__T_10_addr] <= _T_data__T_10_data; // @[Decoupled.scala 209:24]
    end
    if(_T_strb__T_10_en & _T_strb__T_10_mask) begin
      _T_strb[_T_strb__T_10_addr] <= _T_strb__T_10_data; // @[Decoupled.scala 209:24]
    end
    if (reset) begin
      _T_1 <= 1'h0;
    end else if (_T_11) begin
      _T_1 <= _T_6;
    end
  end
endmodule
module Queue_4(
  input        clock,
  input        reset,
  output       io_enq_ready,
  input        io_enq_valid,
  input  [1:0] io_enq_bits_bresp,
  input        io_deq_ready,
  output       io_deq_valid,
  output [1:0] io_deq_bits_bresp
);
  reg [1:0] _T_bresp [0:0]; // @[Decoupled.scala 209:24]
  reg [31:0] _RAND_0;
  wire [1:0] _T_bresp__T_14_data; // @[Decoupled.scala 209:24]
  wire  _T_bresp__T_14_addr; // @[Decoupled.scala 209:24]
  wire [1:0] _T_bresp__T_10_data; // @[Decoupled.scala 209:24]
  wire  _T_bresp__T_10_addr; // @[Decoupled.scala 209:24]
  wire  _T_bresp__T_10_mask; // @[Decoupled.scala 209:24]
  wire  _T_bresp__T_10_en; // @[Decoupled.scala 209:24]
  reg  _T_1; // @[Decoupled.scala 212:35]
  reg [31:0] _RAND_1;
  wire  _T_6; // @[Decoupled.scala 40:37]
  wire  _T_8; // @[Decoupled.scala 40:37]
  wire  _T_11; // @[Decoupled.scala 227:16]
  assign _T_bresp__T_14_addr = 1'h0;
  assign _T_bresp__T_14_data = _T_bresp[_T_bresp__T_14_addr]; // @[Decoupled.scala 209:24]
  assign _T_bresp__T_10_data = io_enq_bits_bresp;
  assign _T_bresp__T_10_addr = 1'h0;
  assign _T_bresp__T_10_mask = 1'h1;
  assign _T_bresp__T_10_en = io_enq_ready & io_enq_valid;
  assign _T_6 = io_enq_ready & io_enq_valid; // @[Decoupled.scala 40:37]
  assign _T_8 = io_deq_ready & io_deq_valid; // @[Decoupled.scala 40:37]
  assign _T_11 = _T_6 != _T_8; // @[Decoupled.scala 227:16]
  assign io_enq_ready = ~_T_1; // @[Decoupled.scala 232:16]
  assign io_deq_valid = _T_1; // @[Decoupled.scala 231:16]
  assign io_deq_bits_bresp = _T_bresp__T_14_data; // @[Decoupled.scala 233:15]
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
  _RAND_0 = {1{`RANDOM}};
  `ifdef RANDOMIZE_MEM_INIT
  for (initvar = 0; initvar < 1; initvar = initvar+1)
    _T_bresp[initvar] = _RAND_0[1:0];
  `endif // RANDOMIZE_MEM_INIT
  `ifdef RANDOMIZE_REG_INIT
  _RAND_1 = {1{`RANDOM}};
  _T_1 = _RAND_1[0:0];
  `endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`endif // SYNTHESIS
  always @(posedge clock) begin
    if(_T_bresp__T_10_en & _T_bresp__T_10_mask) begin
      _T_bresp[_T_bresp__T_10_addr] <= _T_bresp__T_10_data; // @[Decoupled.scala 209:24]
    end
    if (reset) begin
      _T_1 <= 1'h0;
    end else if (_T_11) begin
      _T_1 <= _T_6;
    end
  end
endmodule
module AXI4LiteLoopback(
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
  output [63:0] m_axi_awaddr,
  output [2:0]  m_axi_awprot,
  output        m_axi_awvalid,
  input         m_axi_awready,
  output [63:0] m_axi_wdata,
  output [7:0]  m_axi_wstrb,
  output        m_axi_wvalid,
  input         m_axi_wready,
  input  [1:0]  m_axi_bresp,
  input         m_axi_bvalid,
  output        m_axi_bready,
  output [63:0] m_axi_araddr,
  output [2:0]  m_axi_arprot,
  output        m_axi_arvalid,
  input         m_axi_arready,
  input  [63:0] m_axi_rdata,
  input  [1:0]  m_axi_rresp,
  input         m_axi_rvalid,
  output        m_axi_rready
);
  wire  rAddressQ_clock; // @[AXI4LiteLoopback.scala 11:25]
  wire  rAddressQ_reset; // @[AXI4LiteLoopback.scala 11:25]
  wire  rAddressQ_io_enq_ready; // @[AXI4LiteLoopback.scala 11:25]
  wire  rAddressQ_io_enq_valid; // @[AXI4LiteLoopback.scala 11:25]
  wire [63:0] rAddressQ_io_enq_bits_addr; // @[AXI4LiteLoopback.scala 11:25]
  wire [2:0] rAddressQ_io_enq_bits_prot; // @[AXI4LiteLoopback.scala 11:25]
  wire  rAddressQ_io_deq_ready; // @[AXI4LiteLoopback.scala 11:25]
  wire  rAddressQ_io_deq_valid; // @[AXI4LiteLoopback.scala 11:25]
  wire [63:0] rAddressQ_io_deq_bits_addr; // @[AXI4LiteLoopback.scala 11:25]
  wire [2:0] rAddressQ_io_deq_bits_prot; // @[AXI4LiteLoopback.scala 11:25]
  wire  wAddressQ_clock; // @[AXI4LiteLoopback.scala 15:25]
  wire  wAddressQ_reset; // @[AXI4LiteLoopback.scala 15:25]
  wire  wAddressQ_io_enq_ready; // @[AXI4LiteLoopback.scala 15:25]
  wire  wAddressQ_io_enq_valid; // @[AXI4LiteLoopback.scala 15:25]
  wire [63:0] wAddressQ_io_enq_bits_addr; // @[AXI4LiteLoopback.scala 15:25]
  wire [2:0] wAddressQ_io_enq_bits_prot; // @[AXI4LiteLoopback.scala 15:25]
  wire  wAddressQ_io_deq_ready; // @[AXI4LiteLoopback.scala 15:25]
  wire  wAddressQ_io_deq_valid; // @[AXI4LiteLoopback.scala 15:25]
  wire [63:0] wAddressQ_io_deq_bits_addr; // @[AXI4LiteLoopback.scala 15:25]
  wire [2:0] wAddressQ_io_deq_bits_prot; // @[AXI4LiteLoopback.scala 15:25]
  wire  rDataQ_clock; // @[AXI4LiteLoopback.scala 19:22]
  wire  rDataQ_reset; // @[AXI4LiteLoopback.scala 19:22]
  wire  rDataQ_io_enq_ready; // @[AXI4LiteLoopback.scala 19:22]
  wire  rDataQ_io_enq_valid; // @[AXI4LiteLoopback.scala 19:22]
  wire [63:0] rDataQ_io_enq_bits_data; // @[AXI4LiteLoopback.scala 19:22]
  wire [1:0] rDataQ_io_enq_bits_rresp; // @[AXI4LiteLoopback.scala 19:22]
  wire  rDataQ_io_deq_ready; // @[AXI4LiteLoopback.scala 19:22]
  wire  rDataQ_io_deq_valid; // @[AXI4LiteLoopback.scala 19:22]
  wire [63:0] rDataQ_io_deq_bits_data; // @[AXI4LiteLoopback.scala 19:22]
  wire [1:0] rDataQ_io_deq_bits_rresp; // @[AXI4LiteLoopback.scala 19:22]
  wire  wDataQ_clock; // @[AXI4LiteLoopback.scala 23:22]
  wire  wDataQ_reset; // @[AXI4LiteLoopback.scala 23:22]
  wire  wDataQ_io_enq_ready; // @[AXI4LiteLoopback.scala 23:22]
  wire  wDataQ_io_enq_valid; // @[AXI4LiteLoopback.scala 23:22]
  wire [63:0] wDataQ_io_enq_bits_data; // @[AXI4LiteLoopback.scala 23:22]
  wire [7:0] wDataQ_io_enq_bits_strb; // @[AXI4LiteLoopback.scala 23:22]
  wire  wDataQ_io_deq_ready; // @[AXI4LiteLoopback.scala 23:22]
  wire  wDataQ_io_deq_valid; // @[AXI4LiteLoopback.scala 23:22]
  wire [63:0] wDataQ_io_deq_bits_data; // @[AXI4LiteLoopback.scala 23:22]
  wire [7:0] wDataQ_io_deq_bits_strb; // @[AXI4LiteLoopback.scala 23:22]
  wire  bRespQ_clock; // @[AXI4LiteLoopback.scala 27:22]
  wire  bRespQ_reset; // @[AXI4LiteLoopback.scala 27:22]
  wire  bRespQ_io_enq_ready; // @[AXI4LiteLoopback.scala 27:22]
  wire  bRespQ_io_enq_valid; // @[AXI4LiteLoopback.scala 27:22]
  wire [1:0] bRespQ_io_enq_bits_bresp; // @[AXI4LiteLoopback.scala 27:22]
  wire  bRespQ_io_deq_ready; // @[AXI4LiteLoopback.scala 27:22]
  wire  bRespQ_io_deq_valid; // @[AXI4LiteLoopback.scala 27:22]
  wire [1:0] bRespQ_io_deq_bits_bresp; // @[AXI4LiteLoopback.scala 27:22]
  Queue rAddressQ ( // @[AXI4LiteLoopback.scala 11:25]
    .clock(rAddressQ_clock),
    .reset(rAddressQ_reset),
    .io_enq_ready(rAddressQ_io_enq_ready),
    .io_enq_valid(rAddressQ_io_enq_valid),
    .io_enq_bits_addr(rAddressQ_io_enq_bits_addr),
    .io_enq_bits_prot(rAddressQ_io_enq_bits_prot),
    .io_deq_ready(rAddressQ_io_deq_ready),
    .io_deq_valid(rAddressQ_io_deq_valid),
    .io_deq_bits_addr(rAddressQ_io_deq_bits_addr),
    .io_deq_bits_prot(rAddressQ_io_deq_bits_prot)
  );
  Queue wAddressQ ( // @[AXI4LiteLoopback.scala 15:25]
    .clock(wAddressQ_clock),
    .reset(wAddressQ_reset),
    .io_enq_ready(wAddressQ_io_enq_ready),
    .io_enq_valid(wAddressQ_io_enq_valid),
    .io_enq_bits_addr(wAddressQ_io_enq_bits_addr),
    .io_enq_bits_prot(wAddressQ_io_enq_bits_prot),
    .io_deq_ready(wAddressQ_io_deq_ready),
    .io_deq_valid(wAddressQ_io_deq_valid),
    .io_deq_bits_addr(wAddressQ_io_deq_bits_addr),
    .io_deq_bits_prot(wAddressQ_io_deq_bits_prot)
  );
  Queue_2 rDataQ ( // @[AXI4LiteLoopback.scala 19:22]
    .clock(rDataQ_clock),
    .reset(rDataQ_reset),
    .io_enq_ready(rDataQ_io_enq_ready),
    .io_enq_valid(rDataQ_io_enq_valid),
    .io_enq_bits_data(rDataQ_io_enq_bits_data),
    .io_enq_bits_rresp(rDataQ_io_enq_bits_rresp),
    .io_deq_ready(rDataQ_io_deq_ready),
    .io_deq_valid(rDataQ_io_deq_valid),
    .io_deq_bits_data(rDataQ_io_deq_bits_data),
    .io_deq_bits_rresp(rDataQ_io_deq_bits_rresp)
  );
  Queue_3 wDataQ ( // @[AXI4LiteLoopback.scala 23:22]
    .clock(wDataQ_clock),
    .reset(wDataQ_reset),
    .io_enq_ready(wDataQ_io_enq_ready),
    .io_enq_valid(wDataQ_io_enq_valid),
    .io_enq_bits_data(wDataQ_io_enq_bits_data),
    .io_enq_bits_strb(wDataQ_io_enq_bits_strb),
    .io_deq_ready(wDataQ_io_deq_ready),
    .io_deq_valid(wDataQ_io_deq_valid),
    .io_deq_bits_data(wDataQ_io_deq_bits_data),
    .io_deq_bits_strb(wDataQ_io_deq_bits_strb)
  );
  Queue_4 bRespQ ( // @[AXI4LiteLoopback.scala 27:22]
    .clock(bRespQ_clock),
    .reset(bRespQ_reset),
    .io_enq_ready(bRespQ_io_enq_ready),
    .io_enq_valid(bRespQ_io_enq_valid),
    .io_enq_bits_bresp(bRespQ_io_enq_bits_bresp),
    .io_deq_ready(bRespQ_io_deq_ready),
    .io_deq_valid(bRespQ_io_deq_valid),
    .io_deq_bits_bresp(bRespQ_io_deq_bits_bresp)
  );
  assign s_axi_awready = wAddressQ_io_enq_ready; // @[AXI4LiteLoopback.scala 56:17]
  assign s_axi_wready = wDataQ_io_enq_bits_data[0]; // @[AXI4LiteLoopback.scala 67:16]
  assign s_axi_bresp = bRespQ_io_deq_bits_bresp; // @[AXI4LiteLoopback.scala 79:15]
  assign s_axi_bvalid = bRespQ_io_deq_valid; // @[AXI4LiteLoopback.scala 80:16]
  assign s_axi_arready = rAddressQ_io_enq_ready; // @[AXI4LiteLoopback.scala 34:17]
  assign s_axi_rdata = rDataQ_io_deq_bits_data; // @[AXI4LiteLoopback.scala 47:15]
  assign s_axi_rresp = rDataQ_io_deq_bits_rresp; // @[AXI4LiteLoopback.scala 48:15]
  assign s_axi_rvalid = rDataQ_io_deq_valid; // @[AXI4LiteLoopback.scala 49:16]
  assign m_axi_awaddr = wAddressQ_io_deq_bits_addr; // @[AXI4LiteLoopback.scala 60:16]
  assign m_axi_awprot = wAddressQ_io_deq_bits_prot; // @[AXI4LiteLoopback.scala 59:16]
  assign m_axi_awvalid = wAddressQ_io_deq_valid; // @[AXI4LiteLoopback.scala 58:17]
  assign m_axi_wdata = wDataQ_io_deq_bits_data; // @[AXI4LiteLoopback.scala 71:15]
  assign m_axi_wstrb = wDataQ_io_deq_bits_strb; // @[AXI4LiteLoopback.scala 70:15]
  assign m_axi_wvalid = wDataQ_io_deq_valid; // @[AXI4LiteLoopback.scala 72:16]
  assign m_axi_bready = bRespQ_io_enq_ready; // @[AXI4LiteLoopback.scala 77:16]
  assign m_axi_araddr = rAddressQ_io_deq_bits_addr; // @[AXI4LiteLoopback.scala 38:16]
  assign m_axi_arprot = rAddressQ_io_deq_bits_prot; // @[AXI4LiteLoopback.scala 37:16]
  assign m_axi_arvalid = rAddressQ_io_deq_valid; // @[AXI4LiteLoopback.scala 36:17]
  assign m_axi_rready = rDataQ_io_enq_ready; // @[AXI4LiteLoopback.scala 45:16]
  assign rAddressQ_clock = clock;
  assign rAddressQ_reset = reset;
  assign rAddressQ_io_enq_valid = s_axi_arvalid; // @[AXI4LiteLoopback.scala 31:26]
  assign rAddressQ_io_enq_bits_addr = s_axi_araddr; // @[AXI4LiteLoopback.scala 32:30]
  assign rAddressQ_io_enq_bits_prot = s_axi_arprot; // @[AXI4LiteLoopback.scala 33:30]
  assign rAddressQ_io_deq_ready = m_axi_arready; // @[AXI4LiteLoopback.scala 39:26]
  assign wAddressQ_clock = clock;
  assign wAddressQ_reset = reset;
  assign wAddressQ_io_enq_valid = s_axi_awvalid; // @[AXI4LiteLoopback.scala 53:26]
  assign wAddressQ_io_enq_bits_addr = s_axi_awaddr; // @[AXI4LiteLoopback.scala 54:30]
  assign wAddressQ_io_enq_bits_prot = s_axi_awprot; // @[AXI4LiteLoopback.scala 55:30]
  assign wAddressQ_io_deq_ready = m_axi_awready; // @[AXI4LiteLoopback.scala 61:26]
  assign rDataQ_clock = clock;
  assign rDataQ_reset = reset;
  assign rDataQ_io_enq_valid = m_axi_rvalid; // @[AXI4LiteLoopback.scala 42:23]
  assign rDataQ_io_enq_bits_data = m_axi_rdata; // @[AXI4LiteLoopback.scala 43:27]
  assign rDataQ_io_enq_bits_rresp = m_axi_rresp; // @[AXI4LiteLoopback.scala 44:28]
  assign rDataQ_io_deq_ready = s_axi_rready; // @[AXI4LiteLoopback.scala 50:23]
  assign wDataQ_clock = clock;
  assign wDataQ_reset = reset;
  assign wDataQ_io_enq_valid = s_axi_wvalid; // @[AXI4LiteLoopback.scala 64:23]
  assign wDataQ_io_enq_bits_data = s_axi_wdata + 64'h10; // @[AXI4LiteLoopback.scala 65:27]
  assign wDataQ_io_enq_bits_strb = s_axi_wstrb; // @[AXI4LiteLoopback.scala 66:27]
  assign wDataQ_io_deq_ready = m_axi_wready; // @[AXI4LiteLoopback.scala 69:23]
  assign bRespQ_clock = clock;
  assign bRespQ_reset = reset;
  assign bRespQ_io_enq_valid = m_axi_bvalid; // @[AXI4LiteLoopback.scala 75:23]
  assign bRespQ_io_enq_bits_bresp = m_axi_bresp; // @[AXI4LiteLoopback.scala 76:28]
  assign bRespQ_io_deq_ready = s_axi_bready; // @[AXI4LiteLoopback.scala 81:23]
endmodule
