
import coreparam::*;

module tb;
  integer exit_code;

  import "DPI-C" context function void test_main(output int exit_code);
  import "DPI-C" context function void get_host_memory(input longint unsigned address, output longint unsigned data);
  import "DPI-C" context function void put_host_memory(input longint unsigned address, input longint unsigned data);

  // simulate memory mapped register
  export "DPI-C" set_register = function set_register; //(input int address, input int data);
  export "DPI-C" x_register = function x_register; //(input int address, output int data);


  // note we need to achieve parallelism between the tb and the host code
  // one way is the spawn new thread in the beginning tasks...
  initial begin
    // call spawn_main, which spwan a new thread for the main function
    // wait for the maximal simulation time, either from the tb or from the
    // spawned thread
    test_main(exit_code);
  end

  logic clk /* verilator public */ /* verilator clocker */;
  logic reset;
  logic [XLEN-1:0] initPC;
  logic [XLEN-1:0] baseAddress;
  logic irq, ipi;
  logic [XLEN-1:0] irqCounter;
  logic [15:0] bram_addr_b;
  logic bram_en_b;
  logic [63:0] bram_rdata_b;
  assign irq = irqCounter == 99;
  assign ipi = 0;

    CoreGroupAXIWithMemory coregroup(
      .clock(clk),
      .reset(reset),
      .m_initPC(initPC),
      .m_baseAddress(baseAddress),
      .m_coreAddr(),
      .m_coreData(),
      .m_coreValid(),
      .m_coreReady(),
      .m_err_valid(),
      .m_err_src(),
      .m_err_msg(),
      .m_stats_bram_clk_a(clk),
      .m_stats_bram_rst_a(reset),
      .m_stats_bram_addr_b(bram_addr_b),
      .m_stats_bram_rdata_b(bram_rdata_b),
      .m_stats_bram_en_b(bram_en_b)
    );


  always @(posedge clk) begin
    if(reset) begin
      irqCounter <= 0;
    end else begin
      irqCounter <= irqCounter + 1;
      if(irqCounter == 99) irqCounter <= 0;
    end
  end


  `ifndef verilator
  // generate clock from verilog if not in verilator
  always @(posedge clk) begin
    #5 clk = !clk;
  end
  `endif
  initial begin
    reset = 1;
  end

  // interface for verilator host to initialize the pc
  function void set_register(input longint address, input longint data);
    case(address)
      resetRegisterAddress: begin
        reset = data[0];
      end
      initPCRegisterAddress: begin
        initPC = data;
      end
      baseAddrAddress: begin
        baseAddress = data;
      end
    endcase // case address
  endfunction
  // Note: this function should be called twice because it's one cycle latency
  function void x_register(input longint address, output longint data);
    bram_addr_b = address[15:0];
    bram_en_b = 1;
    data = bram_rdata_b;
  endfunction

endmodule
