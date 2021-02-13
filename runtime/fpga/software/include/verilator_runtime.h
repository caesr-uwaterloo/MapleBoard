#pragma once

#include "runtime.h"

class VerilatorRuntime : public Runtime {
 public:
  virtual void set_init_pc(address_t pc);
  virtual void set_reset(int value);
  virtual void set_base_address(address_t baseAddress);
  virtual uint64_t get_stats(int offset);
  VerilatorRuntime();

 protected:
  virtual void _allocate_host_memory(address_t start, size_t size);
};
