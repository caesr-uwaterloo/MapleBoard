#pragma once

#include "runtime.h"

const uint32_t DMA_BEAT = 16 * 1024 * 1024;
class VU9PRuntime : public Runtime {
public:
  virtual void set_init_pc(address_t pc);
  virtual void set_reset(int value);
  virtual void set_base_address(address_t baseAddress);
  virtual uint64_t get_stats(int offset);
  virtual void setup_post(address_t start);
  void remap_mem(address_t start);
  VU9PRuntime();

protected:
  virtual void _allocate_host_memory(address_t start, size_t size);

  address_t get_base_address();
  void* get_cdma();
  void transfer_to_ddr(address_t start, size_t sz);
  uint32_t get_cdma_status();
  uint32_t get_cdma_control();
  void     reset_cdma();
  bool is_cdma_idle() {
    auto status = get_cdma_status();
    return (status & 0b10) == 0 ? false : true;
  }
  bool check_cdma_has_err();
  void set_src(address_t start);
  void set_dst(address_t dst);
  void set_len(address_t len);


private:
  // These will be part of the PCIe BAR
  volatile uint64_t* reset;
  volatile uint64_t* initPC;
  // NOTE: in VU9P this one will be set via the S_AXI_BAR for accessing the DRAM
  // That, in turn, is set via the PCIe BAR
  volatile uint64_t* baseAddr;
  void* cdma = nullptr;
};

