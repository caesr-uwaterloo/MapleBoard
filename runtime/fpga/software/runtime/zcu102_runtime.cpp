#include "zcu102_runtime.h"
#include "coreparam.h"
#include "loguru.hpp"

#include <iostream>
#include <sys/mman.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>



void ZCU102Runtime::set_init_pc(address_t pc) {
    // set_register(initPCRegisterAddress, pc);
    // pc = 0x803510;
    *initPCLo = pc & (0xffffffff);
    *initPCHi = (pc >> 32) & (0xffffffff);

    // read back
    CHECK_EQ_F(*initPCLo, pc & (0xffffffff), "initPCLo not matched, expected 0x%x, got 0x%x", pc & 0xffffffff, *initPCLo);
    CHECK_EQ_F(*initPCHi, (pc >> 32) & (0xffffffff), "initPCHi not matched, expected 0x%x, got 0x%x", (pc >> 32) & 0xffffffff, *initPCHi);
}
void ZCU102Runtime::set_reset(int value) {
    // set_register(resetRegisterAddress, value);
    *reset = value;
}

void ZCU102Runtime::set_base_address(address_t baseAddress) {
    // set_register(baseAddrAddress, baseAddress);

    *baseAddrLo = baseAddress & (0xffffffff);
    *baseAddrHi = (baseAddress >> 32) & (0xffffffff);
}

void ZCU102Runtime::_allocate_host_memory(address_t start, size_t size) {
  _xlnk_reset();
  int fd = open("/dev/mem", O_SYNC | O_RDWR);
  // first allocate memory from the cma memory space
  LOG_F(INFO, "Allocating contiguous memory (sds): %d MB", size / 1024 / 1024);
  void* VDMEM = cma_alloc((uint32_t)size, 0);

  if(VDMEM == nullptr) {
      LOG_F(ERROR, "Cannot allocate contiguous memory.");
      std::abort();
  }
  address_t physical_addr = cma_get_phy_addr(VDMEM);
  LOG_F(INFO, "Physical address=0x%lx", physical_addr);
  cma_munmap(VDMEM, size);
  sds_mmap((void*)physical_addr, size, (void*)start);
  // LOG_F(INFO, "unmap vdmem 0x%x from 0x%x", VDMEM, physical_addr);
  void* new_vdmem = mmap(reinterpret_cast<void*>(start), size, PROT_READ | PROT_WRITE, MAP_SHARED | MAP_FIXED, fd, physical_addr);
  if(new_vdmem == MAP_FAILED) {
      LOG_F(ERROR, "Cannot map contiguous memory to needed address");
      std::abort();
  }
  LOG_F(INFO, "map virtual 0x%lx for physical 0x%lx", new_vdmem, physical_addr);

  this->phys_start = physical_addr - start;


  // allows the sds library to track the mapping
  memset((void*)start, 0, size);
  LOG_F(INFO, "sds_mmap result: 0x%lx", new_vdmem);
  LOG_F(INFO, "get physical addr: 0x%lx", cma_get_phy_addr((void*)0x8033c0));
  // read the first byte...

  // close(fd);

}

ZCU102Runtime::ZCU102Runtime() {
    // initializing the memory and mapping
    int fd = open("/dev/mem", O_RDWR | O_SYNC);
    if(fd == -1) {
        LOG_F(ERROR, "Cannot open /dev/mem for mapping control registers, maybe check for permission.");
        std::abort();
    }

    this->reset = (uint32_t*)mmap(0, 4096, PROT_READ | PROT_WRITE, MAP_SHARED, fd, resetRegisterAddress);
    if(this->reset == MAP_FAILED) {
        LOG_F(ERROR, "Cannot mmap resetRegister.");
        std::abort();
    }
    LOG_F(WARNING, "Reset (0x%x) mapped to 0x%x", resetRegisterAddress, this->reset);

    this->initPCLo = (uint32_t*)mmap(0, 4096, PROT_READ | PROT_WRITE, MAP_SHARED, fd, initPCRegisterAddress);
    this->initPCHi = (uint32_t*)mmap(0, 4096, PROT_READ | PROT_WRITE, MAP_SHARED, fd, initPCRegisterAddress + 0x1000);
    if(this->initPCLo == MAP_FAILED || this->initPCHi == MAP_FAILED) {
        LOG_F(ERROR, "Cannot mmap initPC: %s.", strerror(errno));
        std::abort();
    }
    LOG_F(WARNING, "initPCLo (0x%x) mapped to 0x%x", initPCRegisterAddress, this->initPCLo);
    LOG_F(WARNING, "initPCHi (0x%x) mapped to 0x%x", initPCRegisterAddress, this->initPCHi);

    this->baseAddrLo = (uint32_t*)mmap(0, 4096, PROT_READ | PROT_WRITE, MAP_SHARED, fd, baseAddrAddress);
    this->baseAddrHi = (uint32_t*)mmap(0, 4096, PROT_READ | PROT_WRITE, MAP_SHARED, fd, baseAddrAddress + 0x1000);
    if(this->baseAddrLo == MAP_FAILED || this->baseAddrHi == MAP_FAILED) {
        LOG_F(ERROR, "Cannot mmap baseAddres: %s.", strerror(errno));
        std::abort();
    }
    LOG_F(WARNING, "basAddr (0x%x) mapped to 0x%x", baseAddrAddress, this->baseAddrLo);
    close(fd);
    LOG_F(WARNING, "Done");
}
