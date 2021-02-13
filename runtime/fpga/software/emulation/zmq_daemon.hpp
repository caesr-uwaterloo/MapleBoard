#ifndef _ZMQ_DAEMON_H
#define _ZMQ_DAEMON_H

#include <thread>
#include <queue>
#include <mutex>
#include <future>
#include <condition_variable>

#include "maple_board_comm_emu.h"

struct Command {
  uint64_t address;
  uint64_t size;
  uint64_t data;
  uint64_t req_type;
};

struct Response {
  uint64_t address;
  uint64_t data;
};

struct ZMQDaemon {
  std::mutex qmutex;
  std::mutex rmutex;
  std::condition_variable rcv;

  std::promise<void> exit_prom;
  std::queue<Command> remote_to_main;
  std::queue<Response> main_to_remote;

  ZMQDaemon() { }

  std::thread launch();
  void stop();

  bool try_get_remote_request(Command& cmd, bool pop);
  void push_remote_request(mb_device_request& req);
  void push_remote_response(Response& resp);
  // this is blocking
  void pop_remote_response(mb_device_response& resp);

  // The main loop for receiving requests
private:
  void main();
};

#endif
