#pragma once

#include "handler.h"
#include "systemcall.h"
#include "runtime.h"

#include <wordexp.h>
#include <elf.h>
#include <tuple>

class Utility {
    static wordexp_t riscv_args;
public:
    static Runtime* create_platform_runtime();
    static SystemCall*  create_system_call_object();
    static Handler* create_handlers(size_t number_of_cores, Runtime* runtime,
                                    SystemCall* syscallobj);
    static address_t round_up_power(address_t addr, unsigned exponent);
    /**
     *  the riscv core shares the environment variables as the host
     */
    static char** get_riscv_envp();
    /**
     *  The following functions parse the -a option in the commandline option when launching the runtime
     */
    static char** get_riscv_argv();
    static int    get_riscv_argc();

    static size_t str_arr_storage(char** const arrv);

    static std::tuple<const Elf32_auxv_t*, size_t> get_aux_array_32(Runtime* runtime);
    static std::tuple<const Elf64_auxv_t*, size_t> get_aux_array_64(Runtime* runtime);

private:
    static void _parse_args();
};
