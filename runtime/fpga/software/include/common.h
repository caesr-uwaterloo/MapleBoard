#pragma once

#include "types.h"

#if defined(VERILATOR)
#include <verilated.h>
#include "Vtb__Dpi.h"
#include "Vtb.h"
#include "Vtb_tb.h"
#elif defined(XSIM)
#elif defined(PYNQ)
#elif defined(ZCU102)
extern "C" void test_main(int* exit_code);
#elif defined(VU9P)
extern "C" void test_main(int* exit_code);
#elif defined(F1)
#endif

#include <thread>
#include <future>

extern int  _argc;
extern char **_argv;
extern std::thread handler_thread;
// stop the handler thread
// https://thispointer.com/c11-how-to-stop-or-terminate-a-thread/
extern std::promise<void> exitSignal;
// when handler is done, for example, when checking for isa or exit is called, it will issue such signal
// to stop the main thread
extern std::promise<void> handlerDoneSignal;
extern std::string args;

extern unsigned long has_mon;
extern unsigned long mon_core;
