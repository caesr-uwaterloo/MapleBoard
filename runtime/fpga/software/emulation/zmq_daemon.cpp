#include "zmq_daemon.hpp"
#include "zmq.hpp"

#include <thread>
#include <chrono>
#include <iostream>

#include "loguru.hpp"
#include "maple_board_comm_emu.h"


// we might need to hold dma data here as well, so make it a bit larger
// the buffer will hold during the whole lifetime of the program
const size_t req_buffer_size = 128ul * 1024ul *1024ul;  // 128MB
void ZMQDaemon::main() {
  LOG_F(INFO, "ZMQ Daemon Started");
  using namespace std::chrono_literals;
  int res;

  // initialize the zmq context with a single IO thread
  zmq::context_t context{1};

  // construct a REP (reply) socket and bind to interface
  zmq::socket_t socket{context, zmq::socket_type::rep};
  socket.bind("tcp://*:5555");

  // prepare some static data for responses
  const std::string data{"World"};

  auto fut = exit_prom.get_future();

  // initialize the buffer
  uint8_t* req_buffer = new uint8_t[req_buffer_size];

  for (;;) {
    // exit signal received
    auto exit_status =  fut.wait_for(0ms);
    if(exit_status == std::future_status::ready) { 
      LOG_F(INFO, "Exit Signal Received");
      break; 
    }
    zmq::mutable_buffer buf{req_buffer, req_buffer_size};
    mb_device_response resp;
    const mb_device_request* req = nullptr;
    resp.address = 0;
    resp.data = 0xfeedc0de;

    // receive a request from client
    auto recv_res = socket.recv(buf, zmq::recv_flags::none);
    LOG_F(INFO, "Received Request of Size: %llu", recv_res.value());
    req = reinterpret_cast<const mb_device_request*>(buf.data());

    if(req->req_type == _MB_HS) {
      LOG_F(INFO, "Received handshake request, confirming");
      socket.send(zmq::buffer((void*)&resp, sizeof(resp)), zmq::send_flags::none);
    } else if(req->req_type == _MB_DMA) {
      LOG_F(INFO, "Received DMA request, trying to satisfy");
      LOG_F(INFO, "DST: %lx, LEN: %lx", req->address, req->dma_len);
      LOG_F(INFO, "Payload PTR (should be some random value): %p", req->dma_payload);
      uint32_t* payload = (uint32_t*)&req->dma_payload[0x2000];
      LOG_F(INFO, "memcpy(%p, %p, %lx) %x %x %x %x", req->address, req->dma_payload, req->dma_len, 
      payload[0], payload[1], payload[2], payload[3]);
      memcpy((void*)req->address, (void*)req->dma_payload, req->dma_len);
      LOG_F(INFO, "Done copy to host");
      socket.send(zmq::buffer((void*)&resp, sizeof(resp)), zmq::send_flags::none);
      LOG_F(INFO, "Done responding");
      if(req->address == 0x800000) {
        uint32_t* v = (uint32_t*)(&(req->dma_payload[0x2000]));
        LOG_F(INFO, "Dumping first words for debugging: %x %x %x %x", v[0], v[1], v[2], v[3]);
      }
    } else if(req->req_type == _MB_RD) {
      /** these are normal SLOT requests */
      LOG_F(INFO, "SLOT(CONF) Read Request on %p, sz: %x\n", req->address, req->size);
      mb_device_request rq = *req;
      push_remote_request(rq);
      // blocks
      pop_remote_response(resp);
      socket.send(zmq::buffer((void*)&resp, sizeof(resp)), zmq::send_flags::none);
    } else if(req->req_type == _MB_WR) {
      LOG_F(INFO, "SLOT(CONF) Write Request on %p, sz: %x, data: %lx", 
          req->address, req->size, req->data);
      mb_device_request rq = *req;
      push_remote_request(rq);
      // blocks
      pop_remote_response(resp);
      socket.send(zmq::buffer((void*)&resp, sizeof(resp)), zmq::send_flags::none);
    } else {
      LOG_F(INFO, "Get Request: req_type: %lu addr: %x, data: %x, len: %lu", 
          req->req_type, req->address, req->data, req->dma_len);
      LOG_F(ERROR, "Unsupported Request Type");
      std::abort();
    }
  }

  // remove the buffer
  delete [] req_buffer;
}

std::thread ZMQDaemon::launch() {
  return std::thread([=] { main(); });
}

void ZMQDaemon::stop() {
  LOG_F(INFO, "Set exit signal");
  exit_prom.set_value();
}

bool ZMQDaemon::try_get_remote_request(Command& cmd, bool pop) {
  qmutex.lock();
  if(!remote_to_main.empty()) {
    cmd = remote_to_main.front();
    if(pop) {
      remote_to_main.pop();
    }
    qmutex.unlock();
    return true;
  }
  qmutex.unlock();
  return false;
}

void ZMQDaemon::push_remote_request(mb_device_request& req) {
  qmutex.lock();
  Command cmd;
  cmd.address = req.address;
  cmd.data = req.data;
  cmd.size = req.size;
  cmd.req_type = req.req_type;
  remote_to_main.push(cmd);
  qmutex.unlock();
}

void ZMQDaemon::push_remote_response(Response& resp) {
  std::unique_lock<std::mutex> lk(this->rmutex);
  main_to_remote.push(resp);
  rcv.notify_all();
}

void ZMQDaemon::pop_remote_response(mb_device_response& resp) {
  std::unique_lock<std::mutex> lk(this->rmutex);
  while(main_to_remote.empty()) {
    rcv.wait(lk);
  }
  auto r = main_to_remote.front();
  main_to_remote.pop();
  resp.address = r.address;
  resp.data = r.data;
}