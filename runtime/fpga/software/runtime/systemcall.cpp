#include "systemcall.h"
#include "common.h"
#include "runtime.h"
#include "loguru.hpp"

#include <iostream>
#include <sys/utsname.h>

#include <unistd.h>
#include <sys/syscall.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/time.h>
#include <sys/resource.h>

extern Runtime* runtime;
extern size_t number_of_cores;

int SystemCall::coreid = -1;

SystemCall::SystemCall() {
    #define INSERT_SYSCALL(name) this->_call_table[word_t::from(this->sysid_##name)] = SystemCall::name
    this->_call_table[word_t::from(this->sysid_check_isa_test)] = SystemCall::check_isa_test;
    this->_call_table[word_t::from(this->sysid_write)] = SystemCall::write;
    INSERT_SYSCALL(read);
    INSERT_SYSCALL(lseek);
    this->_call_table[word_t::from(this->sysid_faccessat)] = SystemCall::faccessat;
    INSERT_SYSCALL(geteuid);
    INSERT_SYSCALL(getuid);
    INSERT_SYSCALL(getgid);
    INSERT_SYSCALL(getegid);
    INSERT_SYSCALL(gettimeofday);
    INSERT_SYSCALL(brk);
    INSERT_SYSCALL(newuname);
    INSERT_SYSCALL(readlinkat);
    INSERT_SYSCALL(fstat);
    INSERT_SYSCALL(exit_group);
    INSERT_SYSCALL(setrobustlist);
    INSERT_SYSCALL(rt_sigaction);
    INSERT_SYSCALL(rt_sigprocmask);
    INSERT_SYSCALL(prlimit64);
    INSERT_SYSCALL(mmap2);
    INSERT_SYSCALL(munmap);
    INSERT_SYSCALL(mprotect);
    INSERT_SYSCALL(madvise);
    INSERT_SYSCALL(futex);
    INSERT_SYSCALL(clone);
    INSERT_SYSCALL(openat);
    INSERT_SYSCALL(writev);
    INSERT_SYSCALL(getpid);
    INSERT_SYSCALL(gettid);
    INSERT_SYSCALL(close);
    INSERT_SYSCALL(tgkill);

    // not really system call but just for handling other exceptions
    INSERT_SYSCALL(illegal_instruction);
}

word_t SystemCall::dispatch(word_t args[8], int coreid) {
    coreid = coreid;
  word_t syscall_id = args[7];
  LOG_F(INFO, "System Call ID: %d (HART %d)", syscall_id, coreid);
  if (this->_call_table.find(syscall_id) == this->_call_table.end()) {
    LOG_F(ERROR, "Unimplemented system call");
    std::abort();
  }
  return this->_call_table[syscall_id](args);
}

address_t SystemCall::allocate_chunk(size_t size) {
    for(auto it: runtime->unmapped_region) {

    }
}

word_t SystemCall::check_isa_test(word_t args[8]) {
  word_t errcode = args[6];
  if (errcode.v == 1) {
    std::cout << "[isa test passed]" << std::endl;
    handlerDoneSignal.set_value();
  } else {
    std::cout << "[isa test failed]" << std::endl;
    if ((errcode.v & 1337) == 1337) {
      std::cout << "Invalid instruction." << std::endl;
    } else {
      std::cout << "FAILED AT 0x" << (errcode.v / 2) << "." << std::endl;
    }
    std::exit(-1);
  }
  /* unreachable */
  return word_t::from(0);
}

word_t SystemCall::write(word_t args[8]) {
    LOG_F(WARNING, "write(fd=%0x, buf=0x%p(%s), count=0x%016x)", args[0], args[1], reinterpret_cast<char*>(args[1].v), args[2]);
    return word_t::from(syscall(SYS_write, args[0], args[1], args[2]));
}

word_t SystemCall::read(word_t args[8]) {
    LOG_F(WARNING, "read(fd=%0x, buf=0x%p, count=0x%016lx)", args[0], args[1], args[2]);
    return word_t::from(syscall(SYS_read, args[0], args[1], args[2]));
}

word_t SystemCall::lseek(word_t args[8]) {
    LOG_F(WARNING, "read(fd=%0x, offset=0x%lx, whence=0x%x)", args[0], args[1], args[2]);
    return word_t::from(syscall(SYS_lseek, args[0], args[1], args[2]));
}

word_t SystemCall::faccessat(word_t args[8]) {
    LOG_F(WARNING, "faccessat(dirfd=%0x, pathname=%p(%s), mode=0x%016x, flags=0x%016x)", args[0], args[1], reinterpret_cast<char*>(args[1].v), args[2], args[3]);
    return word_t::from(syscall(SYS_faccessat, args[0], args[1], args[2], args[3]));
}

word_t SystemCall::geteuid(word_t args[8]) {
    LOG_F(WARNING, "getueid()");
    return word_t::from(syscall(SYS_geteuid));
}

word_t SystemCall::getgid(word_t args[8]) {
    LOG_F(WARNING, "getgid()");
    return word_t::from(syscall(SYS_getgid));
}
word_t SystemCall::getegid(word_t args[8]) {
    LOG_F(WARNING, "getegid()");
    return word_t::from(syscall(SYS_getegid));
}

word_t SystemCall::getuid(word_t args[8]) {
    LOG_F(WARNING, "getuid()");
    return word_t::from(syscall(SYS_getuid));
}

word_t SystemCall::brk(word_t args[8]) {
    LOG_F(WARNING, "brk(addr=0x%x)", args[0]);
    if(args[0].v >= runtime->curbrk && args[0].v < runtime->brk_end) {
        // zero's out the allocated memory
        // memset(reinterpret_cast<void*>(runtime->curbrk), 0, args[0].v - runtime->curbrk);
        return args[0];
    } else if(args[0].v == 0) {
        return word_t::from(runtime->curbrk);
    } else {
        return word_t::from(-1);
    }
}

word_t SystemCall ::mmap2(word_t args[8]) {
  LOG_F(WARNING,
        "mmap2(addr=0x%x, length=0x%x, prot=0x%x, flags=0x%x, fd=0x%x, pgoffset=0x%x)", 
        args[0], args[1], args[2], 
        args[3], args[4], args[5]);
  return word_t::from(runtime->create_mmap2(
      args[0].v,
      args[1].v,
      args[2].v,
      args[3].v,
      args[4].v,
      args[5].v
  ));
}

word_t SystemCall::newuname(word_t args[8]) {
  LOG_F(WARNING, "newuname(buf=0x%0x)", args[0]);
  auto res = word_t::from(uname(reinterpret_cast<utsname*>(args[0].v)));
  utsname* x = reinterpret_cast<utsname*>(args[0].v);
  x->release[3] = '5'; // fake to be 4.15
  // maybe we need to fake the system version to prevent it from crashing
  LOG_F(WARNING, "%s/%s/%s/%s/%s", x->sysname, x->nodename, x->release,
        x->version, x->machine);
  return res;
}

word_t SystemCall::readlinkat(word_t args[8]) {
  LOG_F(WARNING,
        "readlinkat(dir=0x%x, pathname=0x%x(%s), buf=0x%x, bufsize=0x%x)",
        args[0], args[1], reinterpret_cast<char*>(args[1].v), args[2], args[3]);
  char* pathname = reinterpret_cast<char*>(args[1].v);
  if(strcmp(pathname, "/proc/self/exe") == 0) {
      auto len = strlen(runtime->elf_path.c_str());
      auto bytes_to_copy = std::min(args[3].v, (uint64_t)len);
      memcpy((void*)args[2].v, runtime->elf_path.c_str(), bytes_to_copy);
      return word_t::from(bytes_to_copy);
  } else {
    return word_t::from(
        syscall(SYS_readlinkat, args[0], args[1], args[2], args[3]));
  }
}

word_t SystemCall::fstat(word_t args[8]) {
  // 32-bit platform might be complicated
  LOG_F(WARNING, "fstat(fd=0x%x, statbuf=0x%x)", args[0], args[1]);
  // return word_t::from(syscall(syscall(SYS_fstat, args[0], args[1])));
  return word_t::from((int)::fstat(args[0].v, reinterpret_cast<struct stat*>(args[1].v)));
}

word_t SystemCall::exit_group(word_t args[8]) {
  LOG_F(WARNING, "exit_group(status=%d)", args[0]);
  LOG_F(WARNING, "----- recording WCL -----");
  for(size_t i = 0; i < number_of_cores; i++) {
    LOG_F(INFO, "the WCL[%d]: %lld", 2 * i, runtime->get_stats(2 * i));
    LOG_F(INFO, "the WCL[%d]: %lld", 2 * i + 1, runtime->get_stats(2 * i + 1));
  }
  LOG_F(ERROR, "exiting the simulator...");
  // we currently cannot stop other threads, one way would be to send a signal on time interrupt.
  // so now we simply exit the handler thread gracefully...
  handlerDoneSignal.set_value();
  std::terminate();
}

word_t SystemCall::setrobustlist(word_t args[8]) {
  LOG_F(WARNING, "set_robust_list(head=0x%x, len=%d) [skipped]", args[0], args[1]);
  return word_t::from(0);
}
word_t SystemCall::rt_sigaction(word_t args[8]) {
  LOG_F(WARNING, "rt_sigaction(signum=0x%x, act=0x%x, oldact=0x%x) [skipped]", args[0], args[1], args[2]);
  return word_t::from(0);
}
word_t SystemCall::rt_sigprocmask(word_t args[8]) {
  LOG_F(WARNING, "rt_sigprocmask(...) [skipped]");
  return word_t::from(0);
}

word_t SystemCall::prlimit64(word_t args[8]) {
  LOG_F(WARNING, "prlimit(pid=0x%d, resource=0x%x, new_limit=0x%x, old_limit=0x%x)", args[0], args[1], args[2], args[3]);
  rlimit* rlim = reinterpret_cast<rlimit*>(args[3].v);

  auto res = word_t::from(syscall(__NR_prlimit64, args[0], args[1], args[2], args[3]));
  if(args[3].v != 0) {
    LOG_F(WARNING, "Old limit: limit_cur: 0x%lx, limit_max: 0x%lx", rlim->rlim_cur, rlim->rlim_max);
  }
  switch (args[1].v) {
  case RLIMIT_STACK:
    LOG_F(WARNING, "RLIMIT_STACK");
    rlim->rlim_cur = 0x100000; // Limit the stack size, so it won't allocate too much using malloc
    rlim->rlim_max = 0x200000;
    break;
  case RLIMIT_DATA:
    LOG_F(WARNING, "RLIMIT_DATA");
    break;
  }
  return res;
}

word_t SystemCall::mprotect(word_t args[8]) {
  LOG_F(WARNING, "mprotect(addr=0x%0x, len=0x%0x, prot=0x%0x) [skipped]", args[0], args[1], args[2]);
  return word_t::from(0);
}
word_t SystemCall::illegal_instruction(word_t args[8]) {
  ABORT_F("Illegal Instruction at: 0x%0x, Cause: 0x%0x, TVAL: 0x%lx", args[0], args[1], args[2]);
  std::abort();
  return word_t::from(-1);
}

word_t SystemCall::madvise(word_t args[8]) {
  LOG_F(WARNING, "madvise(addr=0x%x, length=0x%x, advice=0x%x) [skipped]", args[0], args[1], args[2]);
  return word_t::from(0);
}

word_t SystemCall::futex(word_t args[8]) {
  LOG_F(WARNING, "futex(uaddr=0x%x, futex_op=0x%x, val=0x%x, timeout=?, uaddr2=?, val=?) fp=%x, errno=%d, *uaddr=%d",
    args[0], args[1], args[2], args[3], args[4], args[5]);
    // NOTE: args[6] is the real return value, just for sanity check
  return args[6];
}

word_t SystemCall::clone(word_t args[8]) {
  LOG_F(WARNING, "clone(flags=0x%x, child_stack=0x%x, parent_tidptr=0x%x, tls=0x%x, child_tidptr=0x%x)",
    args[0], args[1], args[2], args[3], args[4]);
  return word_t::from(0);
}

word_t SystemCall::munmap(word_t args[8]) {
  LOG_F(WARNING, "munmap(addr=0x%lx, len=0x%lx)", args[0], args[1]);
  auto res = runtime->remove_mmap(args[0].v, args[1].v);
  if(res == ~0ull) {
    return word_t::from(-1);
  } else {
    return word_t::from(res);
  }
}

word_t SystemCall::gettimeofday(word_t args[8]) {
  LOG_F(WARNING, "gettimeofday(tv=0x%x, tz=0x%x)", args[0], args[1]);
  return word_t::from(syscall(SYS_gettimeofday, args[0], args[1]));
}

word_t SystemCall::openat(word_t args[8]) {
    LOG_F(WARNING, "openat(dir=0x%lx, pathname=0x%lx (%s0), flags=0x%x, mode=0x%x)", args[0], args[1], reinterpret_cast<char*>(args[1].v), args[2], args[3]);
    return word_t::from(syscall(SYS_openat, args[0], args[1], args[2], args[3]));
}

word_t SystemCall::close(word_t args[8]) {
    LOG_F(WARNING, "close(fd=0x%lx)", args[0]);
    return word_t::from(syscall(SYS_close, args[0]));
}

word_t SystemCall::writev(word_t args[8]) {
    LOG_F(WARNING, "writev(fd=0x%lx, iov=0x%lx, iovcnt=0x%x)", args[0], args[1], args[2]);
    return word_t::from(syscall(SYS_writev, args[0], args[1], args[2]));
}
word_t SystemCall::getpid(word_t args[8]) {
    LOG_F(WARNING, "getpid()");
    return word_t::from(syscall(SYS_getpid));
}

word_t SystemCall::gettid(word_t args[8]) {
    LOG_F(WARNING, "gettid()");
    return word_t::from(coreid + 1);
}
word_t SystemCall::tgkill(word_t args[8]) {
    LOG_F(ERROR, "tgkill()");
    ABORT_F("Encountered tgkill() failed");
    std::abort();
    return word_t::from(0);
}
