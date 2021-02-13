#if defined(VERILATOR)
#include "verilator_runtime.h"
#include "verilator_systemcall.h"
#endif

#if defined(ZCU102)
#include "syscall.h"
#include "zcu102_runtime.h"
#endif

#if defined(VU9P)
#include "syscall.h"
#include "vu9p_runtime.h"
#endif

#include "common.h"
#include "utility.h"
#include "runtime.h"
#include "handler.h"
#include "loguru.hpp"

#include <iostream>
#include <tuple>

/**
 * only supported in linux
 */
#include <wordexp.h>

extern char **environ;

wordexp_t Utility::riscv_args;

Runtime *Utility::create_platform_runtime() {
    Runtime *rt = nullptr;
#if defined(VERILATOR)
    rt = new VerilatorRuntime();
#endif
#if defined(ZCU102)
    rt = new ZCU102Runtime();
#endif
#if defined(VU9P)
    rt = new VU9PRuntime();
#endif
    return rt;
}

SystemCall* Utility::create_system_call_object() {
    SystemCall* syscallobj = nullptr;
#if defined(VERILATOR)
    syscallobj = new VerilatorSystemCall();
#endif
#if defined(ZCU102)
    syscallobj = new SystemCall();
#endif
#if defined(VU9P)
    syscallobj = new SystemCall();
#endif
    return syscallobj;
}

Handler* Utility::create_handlers(size_t number_of_cores, Runtime* runtime, SystemCall* syscallobj) {
    Handler* h = reinterpret_cast<Handler*>(operator new[](sizeof(Handler) * number_of_cores));
    for(int i = 0; i < number_of_cores; i++) {
        new(&h[i]) Handler(i, runtime, syscallobj);
    }
    return h;
}

address_t Utility::round_up_power(address_t addr, unsigned exponent) {
    if (exponent > 30)
        std::exit(-1);
    address_t mask = 1ull << exponent;
    return ((addr - 1) & ~(mask - 1)) + mask;
}

char** Utility::get_riscv_envp() {
    return environ;
}

char** Utility::get_riscv_argv() {
  if (riscv_args.we_wordv != nullptr) return riscv_args.we_wordv;
  _parse_args();
  return riscv_args.we_wordv;
}

int Utility::get_riscv_argc() {
  if (riscv_args.we_wordv != nullptr) return riscv_args.we_wordc;
  _parse_args();
  return riscv_args.we_wordc;
}

void Utility::_parse_args() {
  // parse the commandline options if it is not present
  if(wordexp(args.c_str(), &riscv_args, 0) != 0) {
      LOG_F(ERROR, "Failed to expand the argument.");
      std::abort();
  }
  LOG_F(INFO, "riscv argc: %d", riscv_args.we_wordc);
  LOG_SCOPE_F(INFO, "riscv argv:");
  for(unsigned int i = 0; i < riscv_args.we_wordc; i++) {
    LOG_F(INFO, "%s", riscv_args.we_wordv[i]);
  }
}

size_t Utility::str_arr_storage(char** const arrv) {
    size_t res = 0;
    for(unsigned int i = 0; arrv[i]; i++) {
        // including the '\0' in the end
        res += strlen(arrv[i]) + 1;
    }
    return res;
}

std::tuple<const Elf32_auxv_t*, size_t> Utility::get_aux_array_32(Runtime* rt) {
    const static Elf32_auxv_t auxv[] = {
        {.a_type = AT_ENTRY, .a_un = {.a_val = rt->entry_point } },
        {.a_type = AT_PHNUM, .a_un = {.a_val = rt->phnum } },
        {.a_type = AT_PHENT, .a_un = {.a_val = rt->phent } },
        {.a_type = AT_PHDR, .a_un = {.a_val = rt->phdr } },
        {.a_type = AT_RANDOM, .a_un = {.a_val = 0x800000 } },
        {.a_type = AT_NULL, .a_un = {.a_val = 0} },
    };
    return std::make_tuple(auxv, sizeof(auxv));// / sizeof(Elf32_auxv_t));
}
std::tuple<const Elf64_auxv_t*, size_t> Utility::get_aux_array_64(Runtime* rt) {
    const static Elf64_auxv_t auxv[] = {
        /**
         * these information helps the glibc to locate the tls data section
         * without which there could be illegal access in __ctype_init (to _nl_current_LC_CTYPE)
         */
        {.a_type = AT_ENTRY, .a_un = {.a_val = rt->entry_point } },
        {.a_type = AT_PHNUM, .a_un = {.a_val = rt->phnum } },
        {.a_type = AT_PHENT, .a_un = {.a_val = rt->phent } },
        {.a_type = AT_PHDR, .a_un = {.a_val = rt->phdr } },
        {.a_type = AT_RANDOM, .a_un = {.a_val = 0x800000 } },
        {.a_type = AT_NULL, .a_un = {.a_val = 0} },
    };
    return std::make_tuple(auxv, sizeof(auxv));// / sizeof(Elf64_auxv_t));
}
