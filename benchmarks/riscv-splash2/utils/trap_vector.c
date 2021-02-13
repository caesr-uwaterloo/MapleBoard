#include "context.h"
#include "env.h"
#include "encoding.h"
#include "clone.h"
#include "intrinsics.h"
#include "idle.h"
#include "futex.h"

#include <errno.h>
#include <stddef.h>
#include <sys/types.h>
#include <linux/futex.h>

extern unsigned long __get_idle_core();
/**
 * defined in assembly code, part of the RVTEST_DATA_BEGIN
 * need to be changed as 64-bit syscall returns 64-bit result
 * use volatile to prevent the compiler form optimizing it out
 */
extern volatile unsigned long fromhost[][64 / sizeof(unsigned long)];
extern volatile unsigned long tohost[][64 / sizeof(unsigned long)];

// This might be subject to architectural changes
volatile unsigned long __attribute__((aligned(CACHE_LINE_SIZE), section(".syscallargs"))) syscallargs[N_CORE][8];
volatile unsigned long x;

/**
 * used for flushing the cache
 */
// per cache to avoid sharing
volatile unsigned long __attribute__((aligned(CACHE_LINE_SIZE * CACHE_SET_SIZE))) hole[N_CORE][CACHE_LINE_SIZE / sizeof(unsigned long) * CACHE_SET_SIZE];

// we don't need this now as the core now supports coherent access from the host
int __flush_cache(unsigned long core) {
    // unsigned long size_of_hole = sizeof(hole[core]) / sizeof(unsigned long);
    // unsigned long delta = CACHE_LINE_SIZE / sizeof(unsigned long);
    // register unsigned long x asm("a3");
    // for(int i = 0; i < size_of_hole; i += delta) {
    //     x = hole[core][i];
    // }
    // return x;
}

unsigned long __flush_cache_address(volatile void* addr, unsigned long core) {
     // unsigned int log2CL = log2_CACHE_LINE_SIZE;
     // register unsigned long x asm("a3");
     // unsigned int offset = ((unsigned long)addr >> (log2_CACHE_LINE_SIZE)) & (CACHE_SET_SIZE - 1);
     // x = hole[core][CACHE_LINE_SIZE / sizeof(unsigned long) * offset];
     // return x;
}

void __attribute__ ((noinline)) notify_host(unsigned long core) {
    for (int i = 0; i < 8; i++) {
      syscallargs[core][i] = context[core].x[i + 10];
      // __flush_cache_address(&syscallargs[core][i], core);
    }

    tohost[core][0] = 1;
    // __flush_cache_address(&tohost[core][0], core);
    // wait for the response from host
    while (tohost[core][0] != 0); // __flush_cache_address(&tohost[core][0], core);
}

unsigned long __attribute__ ((noinline)) wait_host(unsigned long core) {
    // the syscall that should rely on the host
    while(fromhost[core][0] == 0); // __flush_cache_address(&fromhost[core][0], core);
    unsigned long res = fromhost[core][1];
    fromhost[core][0] = 0;
    // __flush_cache_address(&fromhost[core][0], core);
    return res;
}

extern unsigned long failing_point[N_CORE * CACHE_LINE_SIZE / sizeof(unsigned long)];

unsigned long do_syscall() {
  unsigned long core = __getmhartid();
  if (context[core].x[17] == NR_futex) {
    // do the futex call
    // we need to print the calling arguments and we don't really need the host
    // for(int i = 0; i < 2048; i++) x = i;
    // int uaddr_val = *(int*)context[core].x[10];
    // failing_point[core] = 0;
    // syscallargs[core][0] = context[core].x[2];
    // notify_host(core);
    // wait_host(core);
    int res = __futex((int*)context[core].x[10], (int)context[core].x[11],
                   (int)context[core].x[12], (uint32_t)context[core].x[13],
                   (int*)context[core].x[14], (int)context[core].x[15]);
    // unsigned long tmp16 = context[core].x[16];
    // unsigned long tmp15 = context[core].x[15];
    // unsigned long tmp14 = context[core].x[14];
    // unsigned long tmp13 = context[core].x[13];
    // context[core].x[16] = res;
    // context[core].x[15] = uaddr_val;
    // context[core].x[14] = errno;
    // context[core].x[13] = failing_point[core * CACHE_LINE_SIZE / sizeof(unsigned long)];

    // context[core].x[16] = tmp16;
    // context[core].x[15] = tmp15;
    // context[core].x[14] = tmp14;
    // context[core].x[13] = tmp13;
    return res;
  } else if (context[core].x[17] == NR_clone) {
    // do the clone
    // we need to print the calling arguments and we don't really need the host

    // unsigned long x10 = context[core].x[10];
    // context[core].x[10] = __get_idle_core();
    notify_host(core);
    // context[core].x[10] = x10;
    wait_host(core);
    return __clone_rt(context[core].x[10], (void*)context[core].x[11],
                   (void*)context[core].x[12], (void*)context[core].x[13],
                   (void*)context[core].x[14]);
  } /* else if (context[core].x[17] == NR_exit) {
    // jump to the idle
  } else if(context[core].x[17] == NR_exit_group) {
    // return from exit_group
  } */ else if(context[core].x[17] == NR_set_tid_address) {
    return __set_tid_address(context[core].x[10]);
  } else if(context[core].x[17] == NR_exit) {
      // the exiting thread must clear the tid address
      pid_t* ctid = (pid_t*)context[core].clear_child_tid;
      if(ctid != (void*)0) {
        *ctid = 0;
      }
      // and call futex on this note that we are in kernel space so no need to save...
      __futex(ctid, FUTEX_WAKE, 1, 0, NULL, 0);
      // the thread will never return from this function
      __idle();
      while(1) ;
  } else {
    notify_host(core);
    return wait_host(core);
  }

  return 0;
}

void trap_vector() {
  int core = __getmhartid();
  if (context[core].cause == CAUSE_MACHINE_ECALL) {
    context[core].x[10] = do_syscall(core);
    return;
  } else if(context[core].cause == CAUSE_ILLEGAL_INSTRUCTION) {
    // this is a fatal error so there is no chance to go back
    // and we can use syscall arguments to pass extra information
    context[core].x[10] = context[core].epc;
    context[core].x[11] = context[core].cause;
    context[core].x[12] = __getmtval();
    context[core].x[17] = 0xfff;
    do_syscall(core);
  } else {
    // for these system calls, it will simply report and won't be able to recover
    // but this allows us to show what is going on
    context[core].x[10] = context[core].epc;
    context[core].x[11] = context[core].cause;
    context[core].x[12] = __getmtval();
    context[core].x[17] = 0xfff;
    do_syscall(core);
  }
/* in case that trap_vector cannot handle, simple do an empty loop */
loopback:
  goto loopback;
}

// These utility functions are for CARP

// should be set to zero at the beginning
unsigned long __attribute__((section(".monaddr"))) monaddr[N_CORE] __attribute__((aligned(64)));
// char __attribute__((section(".monaddr"))) padding_0[CACHE_LINE_SIZE];
// whether there is a monitor core (non-crit)
unsigned long __attribute__((section(".has_mon"))) has_mon __attribute__((aligned(64)));
// char __attribute__((section(".has_mon"))) padding_1[CACHE_LINE_SIZE];
// the id of the monitor core
unsigned long __attribute__((section(".mon_core"))) mon_core __attribute__((aligned(64)));
// char __attribute__((section(".mon_core"))) padding_2[CACHE_LINE_SIZE];

// this is the daemon for the non-critical core
void monitor() {
  volatile unsigned long read_value[N_CORE];
  volatile unsigned long* addr;
  while(1) {
    for(int i = 0; i < N_CORE; i++) {
      // simply read this value
      addr = (volatile unsigned long*)monaddr[i];
      if(addr == NULL) continue;
      read_value[i] = *addr;
    }
  }
}
