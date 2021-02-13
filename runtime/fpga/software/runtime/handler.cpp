#include "types.h"
#include "handler.h"
#include "loguru.hpp"

void Handler::_get_syscall_arguments(word_t args[8]) {
  // offset for each of the syscall args
  uint8_t* syscallargs = (uint8_t*)rt->syscallargs + this->coreid * (8 * XLEN / 8);
  LOG_F(INFO, "Trying to get data from this address (and so on): %p", syscallargs);
  // we first bring them into the cache
  for(size_t i = 0; i < 8; i++) {
    args[i] = word_t::from(syscallargs + XLEN / 8 * i);
    LOG_F(INFO, "Getting something from: %p, a = %lx", syscallargs + XLEN / 8 * i, args[i]);
  }
  //// and read again
  // for(size_t i = 0; i < 8; i++) {
  //   args[i] = word_t::from(syscallargs + XLEN / 8 * i);
  //   LOG_F(INFO, "Getting something from: %p, a = %lx", syscallargs + XLEN / 8 * i, args[i]);
  // }
  // LOG_F(INFO, "Get Done...(Should print args right after!) (XLEN = %d)", XLEN);
  // if(XLEN == 64) {
  //   LOG_F(3, "System Call Arguments (a0-a3) %016x %016x %016x %016x", args[0], args[1], args[2], args[3]);
  //   LOG_F(3, "System Call Arguments (a4-a7) %016x %016x %016x %016x", args[4], args[5], args[6], args[7]);
  // } else { // if XLEN == 32
  //   LOG_F(3, "System Call Arguments (a0-a3) %08x %08x %08x %08x", args[0], args[1], args[2], args[3]);
  //   LOG_F(3, "System Call Arguments (a4-a7) %08x %08x %08x %08x", args[4], args[5], args[6], args[7]);
  // }
}

bool Handler::check_ecall() {
    void* to_host = this->_get_core_to_host();
    uint32_t to_host_value = *reinterpret_cast<uint32_t*>(to_host);
    return to_host_value != 0;
}

void Handler::handle_ecall() {
  LOG_F(3, "Received System Call");
  word_t args[8];
  decltype(args[0].v)* to_host = reinterpret_cast<decltype(args[0].v)*>(this->_get_core_to_host());
  // hand shake signal
  *to_host = 0;

  this->_get_syscall_arguments(args);

  word_t res = this->syscallobj->dispatch(args, this->coreid);

  if(args[7].v == this->syscallobj->sysid_check_isa_test) {
      // no need to get back to the core since this is a check isa system call
      // and the program is done
      return;
  } // check isa

  decltype(args[0].v) *from_host = reinterpret_cast<decltype(args[0].v)*>(this->_get_core_from_host());
  *(from_host + 1) = res.v;
  *from_host = 1;
  // wait until the risc-v core set the hand shake value
  while(*from_host != 0) ;

  LOG_F(WARNING, "syscall returns: %llx", res.v);
}

void* Handler::_get_core_to_host() {
  void* to_host = reinterpret_cast<uint8_t*>(rt->to_host) + 64 * this->coreid;
  return to_host;
}
void* Handler::_get_core_from_host() {
  void* from_host = reinterpret_cast<uint8_t*>(rt->from_host)+ 64 * this->coreid;
  return from_host;
}
