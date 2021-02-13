#include "common.h"
#include "utility.h"
#include "systemcall.h"
#include "handler.h"
#include "runtime.h"
#include "loguru.hpp"

#include <unistd.h>
#include <iostream>
using namespace std;

// fields exposed to other sources
int _argc;
char** _argv;
std::thread handler_thread;
std::promise<void> exitSignal;
std::promise<void> handlerDoneSignal;
std::string args;

unsigned long has_mon = 0;
unsigned long mon_core = 0;

// sources private to current file
std::string elf_path;
size_t memsize = 0;
size_t number_of_cores = 8;
// NOTE: runtime object can only be accessed by the handler thread
// Accessing it from the main thread may result in racing condition
Runtime* runtime = nullptr;
SystemCall* syscallobj = nullptr;
Handler* handlers = nullptr;

void handler_proc(std::future<void>);
void initialize_objects();
void release_objects();
void parse_arguments();

/**
 * the function is either launched from the test bench or from the main function
 * the function launches a separate thread for handling system calls and setup
 * the environment
 */
extern "C" void test_main(int* exit_code) {
  loguru::g_preamble_date = false;
  loguru::g_preamble_time = false;
  loguru::init(_argc, _argv);
  LOG_F(INFO, "test_main: launched from run_elf or from the tb");
  parse_arguments();
  LOG_F(INFO, "allocating memory: %d Bytes", memsize);
  LOG_F(INFO, "loading elf: %s", elf_path.c_str());
  handler_thread = thread(handler_proc, std::move(exitSignal.get_future()));
}

void parse_arguments() {
  int opt;
  while ((opt = getopt(_argc, _argv, "a:m:n:")) != -1) {
    switch (opt) {
      case 'a':
        args = optarg;
        LOG_F(INFO, "args to Program: %s", args.c_str());
        break;
      case 'm':
        std::sscanf(optarg, "%zu", &memsize);
        break;
      case 'n':
        has_mon = 1;
        std::sscanf(optarg, "%zu", &mon_core);
        LOG_F(INFO, "has mon, the mon core id is %zu", mon_core);
        break;
      case '?':
        if (optopt == 'm') {
          std::cerr << "Option -m requires an argument." << std::endl;
        } else {
          std::cerr << "Unknown argument." << std::endl;
        }
        std::exit(-1);
      default:
        std::cerr << "Unknown argument." << std::endl;
        std::exit(-1);
    }
  }
  if (optind != _argc - 1) {
    LOG_F(ERROR, "Exactly one ELF file must be specified");
  }
  elf_path = _argv[optind];
  args = elf_path + " " + args;
  LOG_F(WARNING, "riscv program commandline: %s", args.c_str());
}

void initialize_objects() {
  LOG_F(INFO, "CREATE PF RT");
  runtime = Utility::create_platform_runtime();
  if(::has_mon) {
    runtime->set_mon_core(::mon_core);
  }
  LOG_F(INFO, "CREATE SYSOBJ");
  syscallobj = Utility::create_system_call_object();
  LOG_F(INFO, "CREATE HANDLER");
  handlers = Utility::create_handlers(number_of_cores, runtime, syscallobj);
  LOG_F(INFO, "CREATE LOADELF");
  runtime->load_elf(::elf_path, memsize);
  LOG_F(INFO, "CREATE STACK");
  runtime->load_stack();
  /**
   * TODO: remove this
   */
  // std::cout << "Stopping the host runtime" << std::endl;
  // std::abort();
}

void release_objects(){};

/**
 * the function is launched in a separate thread to handle system calls
 * the function runs a
 */
void handler_proc(std::future<void> ft) {
  LOG_F(INFO, "hello from thread!");
  initialize_objects();

  LOG_F(INFO, "Setting initPc = %x", runtime->entry_point);
  runtime->set_init_pc(runtime->entry_point);

  LOG_F(INFO, "Setting base (physical) address = 0x%x", runtime->get_phys_start());
  runtime->set_base_address(runtime->get_phys_start());

  LOG_F(INFO, "Setting reset = 1");
  runtime->set_reset(1);


  LOG_F(INFO, "Setup host");
  runtime->setup_post(runtime->memory_start);

  LOG_F(INFO, "Delay for 500ms so the core can get reset");
  std::this_thread::sleep_for(1500ms);
  LOG_F(INFO, "Setting reset = 0");
  // awaits for user's input to continue
  // note, on board, these functions will be blocking calls
  runtime->set_reset(0);

  // LOG_F(INFO, "PATH=%s", (char*)0x7ffffa4);
  // LOG_F(INFO, "PATH(1)=%s", (char*)0x7ffffa4 + 1);
  // LOG_F(INFO, "PATH(2)=%s", (char*)0x7ffffa4 + 2);
  // LOG_F(INFO, "PATH(3)=%s", (char*)0x7ffffa4 + 3);
  // LOG_F(INFO, "PATH(4)=%s", (char*)0x7ffffa4 + 4);
  // LOG_F(INFO, "PATH(8)=%s", (char*)0x7ffffa4 + 8);
  // LOG_F(INFO, "PATH(16)=%s", (char*)0x7ffffa4 + 16);


  // wait for the signal from the mainthread to end
  // delay for 1ms and wait
  std::cout << " ======================= start handler loop =========================== " << std::endl;
  // std::cout << " read 0x800000 = " << std::hex << *(volatile uint32_t*)0x800000 << std::endl;
  // std::cout << " read 0x802000 = " << std::hex << *(volatile uint32_t*)0x802000 << std::endl;
  // char c;
  // std::cin >> c;
  // std::cout << " read 0x887400 = (syscallargs)" << std::hex << *(volatile uint64_t*)0x887400 << std::endl;
  // *(volatile uint64_t*)0x887200 = 0;
  // std::cout << " write 0x887200 = deadbeef..." << std::endl;
  //
  uint64_t counter = 0;

  while(ft.wait_for(std::chrono::milliseconds(1)) == std::future_status::timeout) {
    counter ++ ;
    if(counter == 1000000) {
      counter = 0;
      LOG_F(WARNING, "----- recording WCL -----");
      for(size_t i = 0; i < number_of_cores; i++) {
        LOG_F(INFO, "the WCL[%d]: %lld", 2 * i, runtime->get_stats(2 * i));
        LOG_F(INFO, "the WCL[%d]: %lld", 2 * i + 1, runtime->get_stats(2 * i + 1));
      }
    }
    for(size_t i = 0; i < number_of_cores; i++) {
      if(handlers[i].check_ecall()) {
        // std::cout << "encountered ecall... hart " << i << std::endl;
        LOG_F(INFO, "encountered ecall... hart %d", i);
        handlers[i].handle_ecall();
      }
    }
  }
}
