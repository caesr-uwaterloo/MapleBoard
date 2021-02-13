
module CONF(
  input sys_clk_clk_p,
  output logic [63:0]CONF_araddr,
  output logic [1:0]CONF_arburst,
  output logic [3:0]CONF_arcache,
  output logic [7:0]CONF_arlen,
  output logic [0:0]CONF_arlock,
  output logic [2:0]CONF_arprot,
  output logic [3:0]CONF_arqos,
  input CONF_arready,
  output logic [2:0]CONF_arsize,
  output logic CONF_arvalid,
  output logic [63:0]CONF_awaddr,
  output logic [1:0]CONF_awburst,
  output logic [3:0]CONF_awcache,
  output logic [7:0]CONF_awlen,
  output logic [0:0]CONF_awlock,
  output logic [2:0]CONF_awprot,
  output logic [3:0]CONF_awqos,
  input CONF_awready,
  output logic [2:0]CONF_awsize,
  output logic CONF_awvalid,
  output logic CONF_bready,
  input [1:0]CONF_bresp,
  input CONF_bvalid,
  input [63:0]CONF_rdata,
  input CONF_rlast,
  output logic CONF_rready,
  input [1:0]CONF_rresp,
  input CONF_rvalid,
  output logic [63:0]CONF_wdata,
  output logic CONF_wlast,
  input CONF_wready,
  output logic [7:0]CONF_wstrb,
  output logic CONF_wvalid
);

  export "DPI-C" function send_conf_req;
  export "DPI-C" function recv_conf_resp;
  // export "DPI-C" function send_conf_req;
  // export "DPI-C" function recv_conf_resp;

  logic       req_valid;
  // ideally, this will be some form of FIFO, but we only allow one transaction, so this is enoguh.
  logic       write_req_tag; // only operated by host
  logic       read_req_tag; // only operated by shell
  logic       req_done;
  logic[63:0] req_address;
  logic[63:0] req_data;
  logic[63:0] req_size;
  logic[63:0] req_type;
  logic[2:0]  req_offset;
  logic       is_conf; 
  assign req_done   = write_req_tag == read_req_tag;
  assign is_conf = 1; // 64'h100000 <= req_address && req_address < 64'h200000;
  always @(*) begin
    if(!(
      req_size == 0 || 
      req_size == 2 && req_address[0:0] == 0 ||
      req_size == 4 && req_address[1:0] == 0 ||
      req_size == 8 && req_address[2:0] == 0
    )) $error("Unaligned access");

    // these are set in the address editor
    if(!req_done) begin
      if(! is_conf) $error("CONF request not with in address range");
    end
  end

  logic         read_resp_tag;
  logic         write_resp_tag;
  logic       resp_done;
  logic[63:0] resp_address;
  logic[63:0] resp_data;
  initial begin
    state = idle;
    read_req_tag = 0;
    write_req_tag = 0;
    read_resp_tag = 0;
    write_resp_tag = 0;
  end
  /* we cannot do unpacked struct */
  function longint unsigned  send_conf_req(input longint unsigned address, input longint unsigned data, input longint unsigned size, input longint unsigned req_type_);
    if(req_done) begin
      $display("Called recv conf_req, %b %b %b", write_req_tag, read_req_tag, req_done);
      write_req_tag = write_req_tag ^ 1;
      req_address = address;
      req_data = data;
      req_size = size;
      req_type = req_type_;
      send_conf_req = 1;
    end else begin
      send_conf_req = 0;
    end
  endfunction

  function longint unsigned recv_conf_resp(output longint unsigned address ,output longint unsigned data);

    if(!resp_done) begin
      $display("Called recv conf_resp, %b %b %b %b", write_resp_tag, read_resp_tag, resp_done, 
        write_resp_tag == read_resp_tag
      );
      read_resp_tag = read_resp_tag ^ 1;
      address = resp_address;
      data    = resp_data;
      recv_conf_resp = 1;
    end else begin
      recv_conf_resp = 0;
    end
  endfunction
 assign resp_done   = write_resp_tag == read_resp_tag;

  enum { idle, aw, ar, r, w, b } state, state_conf;
  always @(posedge sys_clk_clk_p) begin
    state_conf <= state;
    if(state_conf != state) begin
      $display("state: %s", state.name());
    end
  end
  // the state machine for slot
  always @(posedge sys_clk_clk_p) begin
    case(state)
    idle: begin
      if(!req_done) begin
        req_offset <= req_address[2:0];
        if(req_type == `_MB_RD && is_conf) begin
          state <= ar;
          CONF_arvalid <= 1;
          CONF_araddr <= req_address;
          CONF_arsize <= 3;
          CONF_arlen <= 0;
        end else if(req_type == `_MB_WR && is_conf) begin
          state <= aw;
          CONF_awvalid <= 1;
          CONF_awaddr <= req_address;
          CONF_awsize <= 3;
          CONF_awlen <= 0;
        end
        resp_address <= req_address;
      end
    end
    aw: begin
      if(CONF_awvalid && CONF_awready) begin
        CONF_wlast <= 1;
        read_req_tag <= read_req_tag + 1;
        state <= w;
        CONF_wvalid <= 1;
        CONF_awvalid <= 0;
        if(req_size == 1) begin
          CONF_wstrb <= 8'b1 << req_offset;
          CONF_wdata <= req_data << (req_offset * 8);
        end else if(req_size == 2) begin
          CONF_wstrb <= 8'b11 << req_offset;
          CONF_wdata <= req_data << (req_offset * 8);
        end else if(req_size == 4) begin
          CONF_wstrb <= 8'b1111 << req_offset;
          CONF_wdata <= req_data << (req_offset * 8);
        end else if(req_size == 8) begin
          CONF_wstrb <= 8'b11111111 << req_offset;
          CONF_wdata <= req_data << (req_offset * 8);
        end
        // also play around with 
      end
    end
    ar: begin
      if(CONF_arvalid && CONF_arready) begin
        read_req_tag <= read_req_tag + 1;
        CONF_rready <= 1;
        CONF_arvalid <= 0;
        state <= r;
      end
    end
    r: begin
      if(CONF_rvalid && CONF_rready) begin
        if(! resp_done) $error("Cannot handle multiple outstanding resposnes");
        if(! CONF_rlast) $error("Must be last piece of data");
        write_resp_tag <= write_resp_tag + 1;
        CONF_rready <= 0;
        resp_data <= CONF_rdata >> (req_offset * 8);
        state <= idle;
      end
    end
    w: begin
      if(CONF_wvalid && CONF_wready) begin
        CONF_wvalid <= 0;
        CONF_bready <= 1;
        state <= b;
      end
    end
    b: begin
      if(CONF_bvalid && CONF_bready) begin
        if(! resp_done) $error("Cannot handle multiple outstanding resposnes");
        write_resp_tag <= write_resp_tag + 1;
        CONF_bready <= 0;
        state <= idle;
      end
    end
    endcase
  end
endmodule