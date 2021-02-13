#ifndef MAX_TIME
#define MAX_TIME 3200000000ULL
#endif

#include "common.h"

#include <iostream>
#include <cstring>
#include "svdpi.h"
#include "verilated_vcd_c.h"

using namespace std::chrono_literals;

Vtb* top = nullptr;
vluint64_t main_time = 0;
double sc_time_stamp() {
  return main_time;
}

// The signal is used by the runtime to trigger the next cycle event
std::atomic<int> count(0);

int main(int argc, char** argv) {
  _argc = argc;
  _argv = argv;
  std::cout << "Verilator Simulation " << std::endl;
  top = new Vtb;
  
  
  Verilated::traceEverOn(true);
  VerilatedVcdC* tfp = new VerilatedVcdC;
  top->trace(tfp, 99);
  tfp->open("/home/allen/harddrive/tmp/riscv.log.vcd");
   

  Verilated::commandArgs(argc, argv);

  auto handlerFuture = handlerDoneSignal.get_future();

  while(MAX_TIME < 0 || main_time < MAX_TIME) {
    top->tb->clk ^= 1;
    bool waiting = count == 1;
    top->eval();
    if(waiting) {
      int orig = 1;
      while(!count.compare_exchange_strong(orig, 2)) orig = 1;
      count = 0;
    }
    main_time += 5;
    auto status = handlerFuture.wait_for(0ms);
    if(status == std::future_status::ready) {
        std::cout << "Prematured break" << std::endl;
        break;
    }
    
    tfp->dump(main_time);
    
  }
  
  tfp->close();
  

  exitSignal.set_value();
  handler_thread.join();

  return 0;
}
