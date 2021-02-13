#pragma once
#include "types.h"
#include <map>
/**
 * the class holding all system calls
 * different platform might have different implementation of system calls
 */
class SystemCall {
 public:
  using systemcall_t = auto (*)(word_t args[8]) -> word_t;

  SystemCall();
  word_t dispatch(word_t args[8], int coreid);

  static word_t check_isa_test(word_t args[8]);
  static word_t mmap(word_t args[8]);
  static word_t mmap2(word_t args[8]);
  static word_t munmap(word_t args[8]);
  static word_t write(word_t args[8]);
  static word_t fstat(word_t args[8]);
  static word_t fcntl(word_t args[8]);
  static word_t gettimeofday(word_t args[8]);
  static word_t openat(word_t args[8]);
  static word_t close(word_t args[8]);
  static word_t writev(word_t args[8]);
  static word_t faccessat(word_t args[8]);
  static word_t geteuid(word_t args[8]);
  static word_t getuid(word_t args[8]);
  static word_t getegid(word_t args[8]);
  static word_t getgid(word_t args[8]);
  static word_t brk(word_t args[8]);
  static word_t getpid(word_t args[8]);
  // static word_t getppid(word_t args[8]);
  static word_t gettid(word_t args[8]);
  static word_t newuname(word_t args[8]);
  static word_t readlinkat(word_t args[8]);
  static word_t lseek(word_t args[8]);
  static word_t setrobustlist(word_t args[8]);
  static word_t rt_sigaction(word_t args[8]);
  static word_t rt_sigprocmask(word_t args[8]);
  static word_t getrlimit(word_t args[8]);
  static word_t madvise(word_t args[8]);
  static word_t read(word_t args[8]);
  static word_t mprotect(word_t args[8]);
  static word_t exit_group(word_t args[8]);
  static word_t prlimit64(word_t args[8]);
  static word_t futex(word_t args[8]);
  static word_t clone(word_t args[8]);
  static word_t illegal_instruction(word_t args[8]);
  static word_t tgkill(word_t args[8]);
  static word_t default_impl(word_t args[8]);

  /**
   * these values might be changed on different host platforms
   */
  int sysid_check_isa_test = 5;
  int sysid_fstat = 80;
  int sysid_fcntl = 25;
  int sysid_gettimeofday = 169;
  int sysid_openat = 56;
  int sysid_close = 57;
  int sysid_writev = 66;
  int sysid_faccessat = 48;
  int sysid_geteuid = 175;
  int sysid_getuid = 174;
  int sysid_getegid = 177;
  int sysid_getgid = 176;
  int sysid_brk = 214;
  int sysid_getpid = 172;
  int sysid_gettid = 178;
  int sysid_newuname = 160;
  int sysid_readlinkat = 78;
  int sysid_write = 64;
  int sysid_lseek = 62;
  int sysid_setrobustlist = 99;
  int sysid_tgkill = 131;
  int sysid_rt_sigaction = 134;
  int sysid_rt_sigprocmask = 135;
  int sysid_getrlimit = 163;
  int sysid_mmap2 = 222;
  int sysid_madvise = 233;
  int sysid_read = 63;
  int sysid_mprotect = 226;
  int sysid_munmap = 215;
  int sysid_exit_group = 94;
  int sysid_exit = 93;
  int sysid_prlimit64 = 261;
  int sysid_futex = 98;
  int sysid_clone = 220;
  int sysid_illegal_instruction = 0xfff;

  static int coreid;

 protected:
  std::map<word_t, SystemCall::systemcall_t, word_comparator_t> _call_table;

  bool register_syscall(word_t& syscall_id, SystemCall::systemcall_t& imp);
  bool unregister_syscall(word_t& syscall_id);

  static address_t allocate_chunk(size_t size);
};  // class SystemCall