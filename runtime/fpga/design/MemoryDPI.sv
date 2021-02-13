import coreparam::XLEN;

/**
 * the module utilizes dpi interfaces to access host memory,
 * only used for 
 */
module MemoryDPI(
    input  logic              clock,
    input  logic              reset,
    input  logic              valid,
    input  logic [XLEN-1:0]   reqAddr,
    input  logic              reqType,
    input  logic [XLEN-1:0]   reqData,
    input  logic [XLEN/8-1:0] reqStrb,
    output logic [XLEN-1:0]   respData
);

  import "DPI-C" context function void put_host_memory_byte(input longint unsigned address, input byte data);
  import "DPI-C" context function void get_host_memory(input longint unsigned address, output longint unsigned data);
  int i;
  logic[XLEN-1:0] data;
  assign respData = data;
  always @(posedge clock) begin
    if(reset) begin
    end else begin
      if(valid) begin
        if(reqType == memoryRead) begin
          // read always returns the whole word, axi translation layer will
          // translate it into proper length and location
          get_host_memory(reqAddr, data);
        end else if(reqType == memoryWrite) begin
          // query on the reqStrb and store byte by byte
          for(i = 0; i < XLEN / 8; i = i + 1) begin
            if(reqStrb[i]) begin
              put_host_memory_byte(reqAddr + XLEN'(i), reqData[i*8 +: 8]);
            end
          end
        end
      end
    end
  end
endmodule // MemoryDPI
