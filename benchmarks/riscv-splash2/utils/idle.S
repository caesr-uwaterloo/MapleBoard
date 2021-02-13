/* idle process */
// When multi-core is enabled, this process is activated
// The process spin and check for possible start point

#include "env.h"

.global __idle_rt;
__idle_rt:
  // clear the thread_mask, indicating the thread is idle;
  la t0, thread_mask;
  csrr t1, mhartid;
  slli t1, t1, 2;
  add t0, t0, t1;
  sw zero, 0(t0);
1:
  csrr t0, mhartid;
  // we need 32 bytes to bootstrap
  slli t0, t0, 5; // mhartid * 32
  la t1, idle_address;
  add t1, t1, t0;
  addi t2, t1, 24;
  amoswap.w a5, zero, 0(t2); // signal indicating the context is ready
  li t0, 1;
  bne a5, t0, 1b; // retry until successfully load new thread


  la t0, thread_mask;
  csrr t1, mhartid;
  slli t1, t1, 2;
  add t0, t0, t1;
  addi t1, zero, 1;
  sw t1, 0(t0);



  csrr t0, mhartid;
  slli t0, t0, 5;
  la t1, idle_address;
  add t1, t1, t0;


  lw a0, 0(t1); // flags
  lw sp, 4(t1); // void *child_stack
  lw a1, 8(t1); // pid_t *ptid;
  lw tp, 12(t1); // tls;
  lw a3, 16(t1); // pid_t *ctid;
  lw a4, 20(t1); // pc;


  addi a4, a4, 4; // pc = pc + 4
  csrw mepc, a4;
  
  // after this point, all register information will be lost
  la t0, task_struct;
  csrr t1, mhartid;
  slli t1, t1, 7; // mhartid * 128
  add t0, t0, t1;
  RESTORE_REG_T0;
  la a0, 0;
  mret;

// we currently only support up to 4 cores
// idle address is used to pass thread arguments
.pushsection .idle_address, "aw"
.global idle_address;
idle_address:
  .skip 32, 0; // 8 32-bit integers, should be enough for now
  .skip 32, 0;
  .skip 32, 0;
  .skip 32, 0;
