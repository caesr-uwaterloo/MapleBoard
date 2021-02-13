// the function in this file is only intended to be used in simulation
// in the runtime on board, these functions will not be compiled
#if defined(VERILATOR) || defined(XSIM)

#include "common.h"
#include <iostream>

// DPI IMPORTS
// DPI import at /home/allen/Dropbox/Caesr/riscv-new/fpga//design//tb.sv:7
extern "C" void get_host_memory (unsigned long long address, unsigned long long* data) {
  *data = *reinterpret_cast<unsigned long long*>(address);
#if defined(SIM_LOG)
  if(SIM_LOG) {
    std:: cerr <<"[CDPI] reading from " << std::hex << address <<": "<< std::flush;
    std:: cerr << *data << std::endl;
  }
#endif
}
// DPI import at /home/allen/Dropbox/Caesr/riscv-new/fpga//design//tb.sv:8
extern "C" void put_host_memory (unsigned long long address, unsigned long long data) {
}

extern "C" void put_host_memory_byte (unsigned long long address, char data) {
#if defined(SIM_LOG)
  if(SIM_LOG) {
    unsigned int* peek = reinterpret_cast<unsigned int*>(0x87d710);
    std:: cerr <<"[CDPI] writing to " << std::hex << address <<": "<< std::flush;
    std:: cerr << ((int)data & 0xff) << std::endl;
  }
#endif
  char* pdata = reinterpret_cast<char*>(address);
  *pdata = data;
}

#endif
