#include <cstdlib>
#include <iostream>
#include <string>

#include <cstring>
#include <chrono>
#include <thread>

#include <unistd.h>
#include <sys/mman.h>

// The size of the memory we are simulating, in bytes
// default value is 1G
#define MEM_SIZE (1UL * 1024UL * 1024UL * 1024UL)

#include "zmq.hpp"
#include "zmq_daemon.hpp"

#include "svdpi.h"
#include "VVU9PTop.h"
#include "verilated_vcd_c.h"
// #include "verilated_fst_c.h"
#include "loguru.hpp"
#include "VVU9PTop__Dpi.h"

void* mem = nullptr;

VVU9PTop* top = nullptr;
vluint64_t main_time = 0;
double sc_time_stamp() {
  return main_time;
}

void allocate_memory() {
  mem = mmap((void*)0x800000, MEM_SIZE, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANON | MAP_FIXED, -1, (off_t)0);
  *(uint32_t*)mem = 0xdeadbeef;
  if(mem == MAP_FAILED) {
    LOG_F(ERROR, "Cannot mmap a chunk of memory for simulation");
    std::abort();
  } else {
    LOG_F(INFO, "mmap a chunk of memory for simulation, starting at: %p", mem);
  }
}

int main(int argc, char** argv) {

  LOG_F(INFO, "Verilator Device Simulation");
  allocate_memory();
  LOG_F(INFO, "Launching Daemon");
  ZMQDaemon mq{};
  auto thread = mq.launch();

  LOG_F(INFO, "new VVU9PTop");
  top = new VVU9PTop;
  LOG_F(INFO, "after VVU9PTop");
  Verilated::traceEverOn(true);
  // VerilatedFstC* tfp = new VerilatedFstC;
  VerilatedVcdC* tfp = new VerilatedVcdC;
  top->trace(tfp, 99);
  // std::string vcd_path = "/home/allen/mount/simlog/riscv.log.fst";
  std::string vcd_path = "/home/allen/mount/simlog/riscv.log.vcd";
  if(argc == 2) {
    vcd_path = argv[1];
  }
  tfp->open(vcd_path.c_str());

  LOG_F(INFO, "cmdArgs");
  Verilated::commandArgs(argc, argv);
  LOG_F(INFO, "After CmdArgs");
  LOG_F(INFO, "current scope: %p", svGetScopeFromName("TOP.VU9PTop.sh.memory_model"));

  // Initial
  for(int i = 0; i < 10; i++) {
    top->sys_clk_p ^= 1;
    top->eval();
    main_time += 1;
    tfp->dump(main_time);
  }

  std::cout << "Press Any Key To Start: ";
  char c;
  std::cin >> c;
  bool with_req = false;

  while(true) {
    // check for requests
    if(main_time % 500 == 0) {
      Command req;
      if (mq.try_get_remote_request(req, false))
      {
        // send the request thru dpi
        // note that we cannot get slot and conf at the same time!
        int res;
        LOG_F(INFO, "Req: addr: %lx, data: %lx", req.address, req.data);
        if (0x100000UL <= req.address && req.address < 0x200000UL)
        {

          LOG_F(INFO, "Sending to CONF");
          svSetScope(svGetScopeFromName("TOP.VU9PTop.sh.conf_model"));
          res = send_conf_req(
              req.address,
              req.data,
              req.size,
              req.req_type);
          with_req = true;
        }
        else if (0x100000000UL <= req.address && req.address < 0x200000000UL)
        {
          LOG_F(INFO, "Sending to SLOT");
          svSetScope(svGetScopeFromName("TOP.VU9PTop.sh.slot_model"));
          res = send_slot_req(
              req.address,
              req.data,
              req.size,
              req.req_type);
          with_req = true;
        }
        else
        {
          LOG_F(ERROR, "Unsupported address range");
          std::abort();
        }

        // if sent successfully,
        if (res)
        {
          mq.try_get_remote_request(req, true);
        }
      }
    }
    Response resp;
    unsigned long long addr, data;
    uint64_t resp_cnt = 0;

    if (with_req && top->sys_clk_p == 0)
    {
      svSetScope(svGetScopeFromName("TOP.VU9PTop.sh.slot_model"));
      if (recv_slot_resp(&addr, &data))
      {
        LOG_F(INFO, "Slot Resp: %lx, %lx", addr, data);
        resp.address = addr;
        resp.data = data;
        mq.push_remote_response(resp);
        resp_cnt++;
        with_req = false;
      }
      svSetScope(svGetScopeFromName("TOP.VU9PTop.sh.conf_model"));
      if (recv_conf_resp(&addr, &data))
      {
        LOG_F(INFO, "Conf Resp: %lx, %lx", addr, data);
        resp.address = addr;
        resp.data = data;
        mq.push_remote_response(resp);
        resp_cnt++;
        with_req = false;
      }

      if (resp_cnt > 1)
      {
        LOG_F(ERROR, "More than 1 response");
        std::abort();
      }
    }

    // check for responses
    // get the responses from the dpi
    top->sys_clk_p ^= 1;
    top->eval();
    main_time += 1;
    tfp->dump(main_time);
  }

  mq.stop();
  thread.join();
}
