#include "verilator_runtime.h"
#include "common.h"
#include "Vtb__Dpi.h"
#include "coreparam.h"

#include <iostream>
#include <atomic>
#include <sys/mman.h>
#include "loguru.hpp"

using namespace std::chrono_literals;

extern std::atomic<int> count;

void VerilatorRuntime::set_init_pc(address_t pc) {
    set_register(initPCRegisterAddress, pc);
}
void VerilatorRuntime::set_reset(int value) {
    set_register(resetRegisterAddress, value);
}

void VerilatorRuntime::set_base_address(address_t baseAddress) {
    set_register(baseAddrAddress, baseAddress);
}

void VerilatorRuntime::_allocate_host_memory(address_t start, size_t size) {
  void* mmap_addr = mmap((void*)this->memory_start, size, PROT_READ | PROT_WRITE, MAP_SHARED | MAP_FIXED | MAP_ANONYMOUS, -1, 0);
  if(mmap_addr == MAP_FAILED) {
      std::cerr << " Cannot mmap, maybe try with sudo privilege" << std::endl;
      std::exit(-1);
  }
  this->phys_start = 0ull;
}

uint64_t VerilatorRuntime::get_stats(int offset) {
  long long res = 0;
  svSetScope(svGetScopeFromName("TOP.tb"));
  x_register((long)offset, &res);
  int retry = 0;
retr:
  if(retry > 100) {
    ABORT_F("Failed to wait for the next cycles");
  } else {
    int orig = 0;
    if(count != 0 || !count.compare_exchange_strong(orig, 1)) {
      std::this_thread::sleep_for(50ms);
      retry++;
      orig = 0;
      goto retr;
    }
  }
  svSetScope(svGetScopeFromName("TOP.tb"));
  x_register((long)offset, &res);
  return res;
}

VerilatorRuntime::VerilatorRuntime() {
}
