#include "common.h"

#include <iostream>
#include <cstring>

#include "loguru.hpp"

using namespace std::chrono_literals;

int main(int argc, char** argv) {
  _argc = argc;
  _argv = argv;
  std::cout << "VU9P Real Board" << std::endl;
  int exit_code;

  // NOTE: this function will launch a new thread
  test_main(&exit_code);

  auto handlerFuture = handlerDoneSignal.get_future();
  // auto status = handlerFuture.wait_for(10000ms);
  handlerFuture.wait();
  exitSignal.set_value();
  handler_thread.join();

  LOG_F(WARNING, "Exit Code: %d", exit_code);


  return 0;
}
