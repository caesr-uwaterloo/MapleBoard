#include "intrinsics.h"

unsigned long __csrr(unsigned int csrid) {
  // only 4096 csrs available
  csrid &= 0xfff;
  unsigned long res;
  if (csrid == CSR_MHARTID) {
    __asm__ volatile("csrr %0, " STR(CSR_MHARTID) "\n\t" : "=r"(res));
  } else if(csrid == CSR_MEPC) {
    __asm__ volatile("csrr %0, " STR(CSR_MEPC) "\n\t" : "=r"(res));
  } else if(csrid == CSR_MTVAL) {
    __asm__ volatile("csrr %0, " STR(CSR_MTVAL) "\n\t" : "=r"(res));
  }
  return res;
}

unsigned long __csrw(unsigned int csrid, unsigned long value) {
  if(csrid == CSR_MEPC) {
    __asm__ volatile("csrw " STR(CSR_MEPC) ", %0" "\n\t" : :"r"(value));
  } else if(csrid == CSR_MSCRATCH) {
    __asm__ volatile("csrw " STR(CSR_MSCRATCH) ", %0" "\n\t" : :"r"(value));
  }
}

unsigned long __getmhartid() { return __csrr(CSR_MHARTID); }
unsigned long __getmepc() { return __csrr(CSR_MEPC); }
unsigned long __getmtval() { return __csrr(CSR_MTVAL); }
unsigned long __setmepc(unsigned long epc) { return __csrw(CSR_MEPC, epc); }
unsigned long __setmscratch(unsigned long mscratch) {
    return __csrw(CSR_MSCRATCH, mscratch);
}