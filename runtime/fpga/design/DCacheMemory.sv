import coreparam::*;

module DCacheMemory(
  input  logic           clk,
  input  logic           reset,
  input  logic           req_type,
  input  logic[XLEN-1:0] req_address,
  input  logic[XLEN-1:0] req_data,
  input  logic[1:0]      req_length,
  input  logic           req_valid,
  output logic           req_ready,

  output logic[XLEN-1:0] resp_address,
  output logic[XLEN-1:0] resp_data,
  output logic           resp_valid,
  input  logic           resp_ready
);
  localparam idle = 0;
  localparam resp = 1;
  localparam wait_ = 2;
  localparam wait_cycles = 5;
  import "DPI-C" context function void get_host_memory(input longint unsigned address, output longint unsigned data);
  import "DPI-C" context function void put_host_memory(input longint unsigned address, input longint unsigned data);
  import "DPI-C" context function void put_host_memory_byte(input longint unsigned address, input byte data);

  logic [1:0] state;
  logic [31:0] counter;
  longint unsigned data_temp;

  assign req_ready  = state == idle;
  assign resp_valid = state == resp;
  longint byte_inc = 0;
  always @(posedge clk) begin
    if(reset) begin
      state <= idle;
    end else begin
      case(state)
        idle: begin
          if(req_valid && req_ready) begin
            if(req_type == memoryRead) begin
              get_host_memory(req_address, data_temp);
              resp_data <= data_temp;
              resp_address <= req_address;
            end else if(req_type == memoryWrite) begin
              `ifdef SIM_LOG
                if(`SIM_LOG != 0) begin
                  $display("Req Length: %x", req_length);
                end
              `endif
              for(byte_inc = 0; byte_inc < (1 << req_length); byte_inc = byte_inc + 1) begin
                put_host_memory_byte(req_address + byte_inc, req_data[8 * byte_inc[5:0] +: 8]);
              end
              resp_address <= req_address;
            end
            state <= wait_;
            counter <= 0;
          end
        end
        wait_: begin
          counter <= counter + 1;
          if(counter == wait_cycles) begin
            state <= resp;
          end
        end
        resp: begin
          if(resp_valid && resp_ready) begin
            state <= idle;
          end
        end
      endcase // case state
    end
  end

endmodule
