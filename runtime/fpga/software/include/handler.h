#pragma once

// #include "coreparam.h"
#include "runtime.h"
#include "systemcall.h"

// handler for processing system calls
// the handler utilizes tohost and fromhost in the prolog and epilog of the program
// note that these addresses must conform with the ones in the assembly program
#include <iostream>
class Handler {
 public:
  Handler(int _coreid, Runtime* _rt, SystemCall* _syscallobj): coreid(_coreid),
  rt(_rt), syscallobj(_syscallobj) { }

  /**
   * check whether there is a pending request
   * normally this function will go to the memory system and check for certain address
   */
  bool check_ecall();
  void handle_ecall();

 protected:
  /**
   * the following functions get per-core private date for communication with
   * the host
   */
  void _get_syscall_arguments(word_t regs[8]);  // a[0] to a[7]
  void* _get_core_to_host();
  void* _get_core_from_host();

 private:
  int coreid = -1;
  Runtime* rt = nullptr;
  SystemCall* syscallobj = nullptr;
};  // class Handler
