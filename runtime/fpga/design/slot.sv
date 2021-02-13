
module SLOT(
  input sys_clk_clk_p,
  output logic [63:0]SLOT_araddr,
  output logic [2:0]SLOT_arprot,
  input SLOT_arready,
  output logic SLOT_arvalid,
  output logic [63:0]SLOT_awaddr,
  output logic [2:0]SLOT_awprot,
  input SLOT_awready,
  output logic SLOT_awvalid,
  output logic SLOT_bready,
  input [1:0]SLOT_bresp,
  input SLOT_bvalid,
  input [63:0]SLOT_rdata,
  output logic SLOT_rready,
  input [1:0]SLOT_rresp,
  input SLOT_rvalid,
  output logic [63:0]SLOT_wdata,
  input SLOT_wready,
  output logic [7:0]SLOT_wstrb,
  output logic SLOT_wvalid
);
  export "DPI-C" function send_slot_req;
  export "DPI-C" function recv_slot_resp;
  // export "DPI-C" function send_conf_req;
  // export "DPI-C" function recv_conf_resp;
  initial begin
    state = idle;
    read_req_tag = 0;
    write_req_tag = 0;
    read_resp_tag = 0;
    write_resp_tag = 0;
  end
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
  logic       is_slot; 
  assign req_done   = write_req_tag == read_req_tag;
  always @(*) begin // we always check this
    if(!(
      req_size == 0 ||
      req_size == 2 && req_address[0:0] == 0 ||
      req_size == 4 && req_address[1:0] == 0 ||
      req_size == 8 && req_address[2:0] == 0
    )) $error("Unaligned access");

    // these are set in the address editor
    if(req_valid) begin
      if(! is_slot) $error("SLOT request not with in address range");
    end
  end

  bit       read_resp_tag;
  bit       write_resp_tag;
  logic       resp_done;
  logic[63:0] resp_address;
  logic[63:0] resp_data;
  /* we cannot do unpacked struct */
  function longint unsigned send_slot_req(input longint unsigned address, input longint unsigned data, input longint unsigned size, input longint unsigned req_type_);
    if(req_done) begin
      $display("Called recv slot_req, %b %b %b", write_req_tag, read_req_tag, req_done);
      write_req_tag = write_req_tag + 1;
      req_address = address;
      req_data = data;
      req_size = size;
      req_type = req_type_;
      send_slot_req = 1;
    end else begin
      send_slot_req = 0;
    end
  endfunction

  assign is_slot = 1; // 64'h200000000 > req_address && req_address >= 64'h100000000;

  function longint unsigned recv_slot_resp(output longint unsigned address ,output longint unsigned data);
    if(!resp_done) begin
      $display("Called recv slot_resp, %b %b %b", write_resp_tag, read_resp_tag, resp_done);
      read_resp_tag = read_resp_tag + 1;
      address = resp_address;
      data    = resp_data;
      recv_slot_resp = 1;
    end else begin
      recv_slot_resp = 0;
    end
  endfunction

  assign resp_done   = write_resp_tag == read_resp_tag;

  enum { idle, aw, ar, r, w, b } state, state_conf;

  // the state machine for slot

  always @(posedge sys_clk_clk_p) begin
    case(state)
    idle: begin
      if(!req_done) begin
        req_offset <= req_address[2:0];
        if(req_type == `_MB_RD && is_slot) begin
          state <= ar;
          SLOT_arvalid <= 1;
          SLOT_araddr <= req_address;
        end else if(req_type == `_MB_WR && is_slot) begin
          state <= aw;
          SLOT_awvalid <= 1;
          SLOT_awaddr <= req_address;
        end
        resp_address <= req_address;
      end
    end
    aw: begin
      if(SLOT_awvalid && SLOT_awready) begin
        read_req_tag <= read_req_tag + 1;
        state <= w;
        SLOT_wvalid <= 1;
        SLOT_awvalid <= 0;
        if(req_size == 1) begin
          SLOT_wstrb <= 8'b1 << req_offset;
          SLOT_wdata <= req_data << (req_offset * 8);
        end else if(req_size == 2) begin
          SLOT_wstrb <= 8'b11 << req_offset;
          SLOT_wdata <= req_data << (req_offset * 8);
        end else if(req_size == 4) begin
          SLOT_wstrb <= 8'b1111 << req_offset;
          SLOT_wdata <= req_data << (req_offset * 8);
        end else if(req_size == 8) begin
          SLOT_wstrb <= 8'b11111111;
          SLOT_wdata <= req_data;
        end
        // also play around with 
      end
    end
    ar: begin
      if(SLOT_arvalid && SLOT_arready) begin
        read_req_tag <= read_req_tag + 1;
        SLOT_rready <= 1;
        SLOT_arvalid <= 0;
        state <= r;
      end
    end
    r: begin
      if(SLOT_rvalid && SLOT_rready) begin
        if(! resp_done) $error("Cannot handle multiple outstanding resposnes");
        write_resp_tag <= write_resp_tag + 1;
        SLOT_rready <= 0;
        resp_data <= SLOT_rdata >> (req_offset * 8);
        state <= idle;
      end
    end
    w: begin
      if(SLOT_wvalid && SLOT_wready) begin
        SLOT_wvalid <= 0;
        SLOT_bready <= 1;
        state <= b;
      end
    end
    b: begin
      if(SLOT_bvalid && SLOT_bready) begin
        if(! resp_done) $error("Cannot handle multiple outstanding resposnes");
        write_resp_tag <= write_resp_tag + 1;
        SLOT_bready <= 0;
        state <= idle;
      end
    end
    endcase
  end
endmodule