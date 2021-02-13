`define _MB_RD  2
`define _MB_WR  3
// Directly get data from the host memory space
module AXI4Memory(
  input sys_clk_clk_p,
  input [63:0]SMEM_araddr,
  input [1:0]SMEM_arburst,
  input [3:0]SMEM_arcache,
  input [0:0]SMEM_arid,
  input [7:0]SMEM_arlen,
  input [0:0]SMEM_arlock,
  input [2:0]SMEM_arprot,
  input [3:0]SMEM_arqos,
  output logic SMEM_arready,
  input [2:0]SMEM_arsize,
  input SMEM_arvalid,
  input [63:0]SMEM_awaddr,
  input [1:0]SMEM_awburst,
  input [3:0]SMEM_awcache,
  input [0:0]SMEM_awid,
  input [7:0]SMEM_awlen,
  input [0:0]SMEM_awlock,
  input [2:0]SMEM_awprot,
  input [3:0]SMEM_awqos,
  output logic SMEM_awready,
  input [2:0]SMEM_awsize,
  input SMEM_awvalid,
  output logic [0:0]SMEM_bid,
  input SMEM_bready,
  output logic [1:0]SMEM_bresp,
  output logic SMEM_bvalid,
  output logic [63:0]SMEM_rdata,
  output logic [0:0]SMEM_rid,
  output logic SMEM_rlast,
  input SMEM_rready,
  output logic [1:0]SMEM_rresp,
  output logic SMEM_rvalid,
  input [63:0]SMEM_wdata,
  input SMEM_wlast,
  output logic SMEM_wready,
  input [7:0]SMEM_wstrb,
  input SMEM_wvalid
);
  logic clk;
  // these interfaces assumes using full width, sort of like axi lite
  import "DPI-C" context function void write64(input longint unsigned addr, input longint unsigned value);
  import "DPI-C" context function longint unsigned read64(input longint unsigned addr);
  import "DPI-C" context function void init();

  enum { idle, ar, aw, w, b, r } state;

  logic[63:0] address;
  logic[7:0] counter;
  logic[63:0] offset;
  logic rvalid;
  logic[63:0] rdata;

  initial begin
    state = idle;
    SMEM_rresp = 0;
    SMEM_bresp = 0;
    rdata = 0;
    init();
  end

  always_comb begin
    SMEM_rvalid = rvalid;
    SMEM_rdata = rdata;
    SMEM_wready = state == w;
    SMEM_bvalid = state == b;
    SMEM_arready = state == idle;
    SMEM_awready = state == idle;
    SMEM_rlast = state == r && counter == 0;
    clk = sys_clk_clk_p;
  end

  always_ff @(posedge clk) begin
    case(state)
    idle: begin
      rvalid <= 0;
      offset <= 0;
      if(SMEM_arvalid && SMEM_arready) begin
        state <= r;
        address <= SMEM_araddr;
        counter <= SMEM_arlen;
        assert (SMEM_arlen == 7) else $fatal("Invalid SMEM arlen");
        assert (SMEM_arsize == 3) else $fatal("Invalid SMEM arsize");
      end else if(SMEM_awvalid && SMEM_awready) begin
        state <= w;
        address <= SMEM_awaddr;
        counter <= SMEM_awlen;
        assert (SMEM_awlen == 7) else $fatal("Invalid SMEM awlen");
        assert (SMEM_awsize == 3) else $fatal("Invalid SMEM awsize");
      end
    end
    r: begin
      if(!rvalid) begin
        rvalid <= 1;
        rdata <= read64(address + offset - 64'h120000000);
        offset <= offset + 8;
      end
      if(SMEM_rvalid && SMEM_rready) begin
        counter <= counter - 1;
        offset <= offset + 8;
        rdata <= read64(address + offset - 64'h120000000);
        if(counter == 0) begin
          state <= idle;
          rvalid <= 0;
        end
      end
    end
    w: begin
      if(SMEM_wvalid && SMEM_wready) begin
        counter <= counter - 1;
        offset <= offset + 8;
        write64(address + offset - 64'h120000000, SMEM_wdata);
        if(counter == 0) begin
          if(!SMEM_wlast) begin
            $error("Protocol Error: should assert wlast");
          end
          state <= b;
        end
      end
    end
    b: begin
      if(SMEM_bvalid && SMEM_bready) begin
        state <= idle;
      end
    end
    endcase
  end

endmodule


// the shell interface, that provides interface for the core
module shell(C1_DDR4_0_act_n, C1_DDR4_0_adr, C1_DDR4_0_ba, 
  C1_DDR4_0_bg, C1_DDR4_0_ck_c, C1_DDR4_0_ck_t, C1_DDR4_0_cke, C1_DDR4_0_cs_n, 
  C1_DDR4_0_dm_n, C1_DDR4_0_dq, C1_DDR4_0_dqs_c, C1_DDR4_0_dqs_t, C1_DDR4_0_odt, 
  C1_DDR4_0_reset_n, CONF_araddr, CONF_arburst, CONF_arcache, CONF_arlen, CONF_arlock, 
  CONF_arprot, CONF_arqos, CONF_arready, CONF_arsize, CONF_arvalid, CONF_awaddr, CONF_awburst, 
  CONF_awcache, CONF_awlen, CONF_awlock, CONF_awprot, CONF_awqos, CONF_awready, CONF_awsize, 
  CONF_awvalid, CONF_bready, CONF_bresp, CONF_bvalid, CONF_rdata, CONF_rlast, CONF_rready, 
  CONF_rresp, CONF_rvalid, CONF_wdata, CONF_wlast, CONF_wready, CONF_wstrb, CONF_wvalid, 
  SLOT_araddr, SLOT_arprot, SLOT_arready, SLOT_arvalid, SLOT_awaddr, SLOT_awprot, SLOT_awready, 
  SLOT_awvalid, SLOT_bready, SLOT_bresp, SLOT_bvalid, SLOT_rdata, SLOT_rready, SLOT_rresp, 
  SLOT_rvalid, SLOT_wdata, SLOT_wready, SLOT_wstrb, SLOT_wvalid, SMEM_araddr, SMEM_arburst, 
  SMEM_arcache, SMEM_arid, SMEM_arlen, SMEM_arlock, SMEM_arprot, SMEM_arqos, SMEM_arready, 
  SMEM_arsize, SMEM_arvalid, SMEM_awaddr, SMEM_awburst, SMEM_awcache, SMEM_awid, SMEM_awlen, 
  SMEM_awlock, SMEM_awprot, SMEM_awqos, SMEM_awready, SMEM_awsize, SMEM_awvalid, SMEM_bid, 
  SMEM_bready, SMEM_bresp, SMEM_bvalid, SMEM_rdata, SMEM_rid, SMEM_rlast, SMEM_rready, 
  SMEM_rresp, SMEM_rvalid, SMEM_wdata, SMEM_wlast, SMEM_wready, SMEM_wstrb, SMEM_wvalid, 
  dimm1_refclk_clk_n, dimm1_refclk_clk_p, pci_express_x16_rxn, pci_express_x16_rxp, 
  pci_express_x16_txn, pci_express_x16_txp, pcie_perstn, pcie_refclk_clk_n, 
  pcie_refclk_clk_p, peripheral_reset_0, root_clk, sys_clk_clk_n, sys_clk_clk_p);
  output C1_DDR4_0_act_n;
  output [16:0]C1_DDR4_0_adr;
  output [1:0]C1_DDR4_0_ba;
  output [1:0]C1_DDR4_0_bg;
  output [0:0]C1_DDR4_0_ck_c;
  output [0:0]C1_DDR4_0_ck_t;
  output [0:0]C1_DDR4_0_cke;
  output [0:0]C1_DDR4_0_cs_n;
  inout [7:0]C1_DDR4_0_dm_n;
  inout [63:0]C1_DDR4_0_dq;
  inout [7:0]C1_DDR4_0_dqs_c;
  inout [7:0]C1_DDR4_0_dqs_t;
  output [0:0]C1_DDR4_0_odt;
  output C1_DDR4_0_reset_n;
  output [63:0]CONF_araddr;
  output [1:0]CONF_arburst;
  output [3:0]CONF_arcache;
  output [7:0]CONF_arlen;
  output [0:0]CONF_arlock;
  output [2:0]CONF_arprot;
  output [3:0]CONF_arqos;
  input CONF_arready;
  output [2:0]CONF_arsize;
  output CONF_arvalid;
  output [63:0]CONF_awaddr;
  output [1:0]CONF_awburst;
  output [3:0]CONF_awcache;
  output [7:0]CONF_awlen;
  output [0:0]CONF_awlock;
  output [2:0]CONF_awprot;
  output [3:0]CONF_awqos;
  input CONF_awready;
  output [2:0]CONF_awsize;
  output CONF_awvalid;
  output CONF_bready;
  input [1:0]CONF_bresp;
  input CONF_bvalid;
  input [63:0]CONF_rdata;
  input CONF_rlast;
  output CONF_rready;
  input [1:0]CONF_rresp;
  input CONF_rvalid;
  output [63:0]CONF_wdata;
  output CONF_wlast;
  input CONF_wready;
  output [7:0]CONF_wstrb;
  output CONF_wvalid;
  output [63:0]SLOT_araddr;
  output [2:0]SLOT_arprot;
  input SLOT_arready;
  output SLOT_arvalid;
  output [63:0]SLOT_awaddr;
  output [2:0]SLOT_awprot;
  input SLOT_awready;
  output SLOT_awvalid;
  output SLOT_bready;
  input [1:0]SLOT_bresp;
  input SLOT_bvalid;
  input [63:0]SLOT_rdata;
  output SLOT_rready;
  input [1:0]SLOT_rresp;
  input SLOT_rvalid;
  output [63:0]SLOT_wdata;
  input SLOT_wready;
  output [7:0]SLOT_wstrb;
  output SLOT_wvalid;
  input [63:0]SMEM_araddr;
  input [1:0]SMEM_arburst;
  input [3:0]SMEM_arcache;
  input [0:0]SMEM_arid;
  input [7:0]SMEM_arlen;
  input [0:0]SMEM_arlock;
  input [2:0]SMEM_arprot;
  input [3:0]SMEM_arqos;
  output SMEM_arready;
  input [2:0]SMEM_arsize;
  input SMEM_arvalid;
  input [63:0]SMEM_awaddr;
  input [1:0]SMEM_awburst;
  input [3:0]SMEM_awcache;
  input [0:0]SMEM_awid;
  input [7:0]SMEM_awlen;
  input [0:0]SMEM_awlock;
  input [2:0]SMEM_awprot;
  input [3:0]SMEM_awqos;
  output SMEM_awready;
  input [2:0]SMEM_awsize;
  input SMEM_awvalid;
  output [0:0]SMEM_bid;
  input SMEM_bready;
  output [1:0]SMEM_bresp;
  output SMEM_bvalid;
  output [63:0]SMEM_rdata;
  output [0:0]SMEM_rid;
  output SMEM_rlast;
  input SMEM_rready;
  output [1:0]SMEM_rresp;
  output SMEM_rvalid;
  input [63:0]SMEM_wdata;
  input SMEM_wlast;
  output SMEM_wready;
  input [7:0]SMEM_wstrb;
  input SMEM_wvalid;
  input dimm1_refclk_clk_n;
  input dimm1_refclk_clk_p;
  input [15:0]pci_express_x16_rxn;
  input [15:0]pci_express_x16_rxp;
  output [15:0]pci_express_x16_txn;
  output [15:0]pci_express_x16_txp;
  input pcie_perstn;
  input pcie_refclk_clk_n;
  input pcie_refclk_clk_p;
  output [0:0]peripheral_reset_0;
  output root_clk;
  input [0:0]sys_clk_clk_n;
  input [0:0]sys_clk_clk_p;

  // assign root_clk and resets
  assign root_clk = sys_clk_clk_p;
  reg[3:0] resetCounter = 0;
  always @(posedge sys_clk_clk_p) begin
    if(resetCounter < 10) begin
      resetCounter <= resetCounter + 1;
    end
  end
  assign peripheral_reset_0 = resetCounter < 10;

  AXI4Memory memory_model(
    .*
  );

  SLOT slot_model(
    .*
  );
  CONF conf_model(
    .*
  );


  // the state machine for conf

endmodule

