import coreparam::*;

module ICacheMemory(
  input  logic           clk,
  input  logic           reset,
  input  logic[XLEN-1:0] req_address,
  input  logic[1:0]      req_length,
  input  logic           req_valid,
  output logic           req_ready,

  output logic[XLEN-1:0] resp_address,
  output logic[fetchWidth-1:0] resp_data,
  output logic           resp_valid,
  input  logic           resp_ready
);
  localparam idle = 0;
  localparam resp = 1;
  localparam wait_ = 2;
  localparam wait_cycles = 7;
  import "DPI-C" context function void get_host_memory(input longint unsigned address, output longint unsigned data);
  import "DPI-C" context function void put_host_memory(input longint unsigned address, input longint unsigned data);

  logic [1:0] state;
  logic [31:0] counter;
  longint unsigned data_temp;

  assign req_ready  = state == idle;
  assign resp_valid = state == resp;

  always @(posedge clk) begin
    if(reset) begin
      state <= idle;
    end else begin
      case(state)
        idle: begin
          if(req_valid && req_ready) begin
            get_host_memory(req_address, data_temp);
            resp_data <= data_temp[31:0];
            resp_address <= req_address;
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
