/* this process is the initiating process
 * it is responsible for storing the data in the memory for another processor to continue
*/

#include "env.h"

#define CLONE_CHILD_CLEARTID	0x00200000

.global __clone_rt;
__clone_rt:
  // get_idle_core to t0
  la t3, ncore;
  lw t3, (t3);
  addi t3, t3, 1;
L3:
  li t0, 0;
  la t5, thread_mask;
  j L4;
L8:
  addi t0, t0, 1;
  beq t3, t0, L3;
L4:
  lw t4, 0(t5);
  addi t5, t5, 4;
  bnez t4, L8;
  slli t1, t0, 5;

  // check for the clear child tid tag
  srli t3, a0, 20;
  andi t3, t3, 1;
  beqz t3, 1f;
  // set the clear child tid tag
  la t3, clear_child_tid;
  srli t4, t1, 3;
  add t3, t3, t4;
  sw  a4, 0(t3);

1:
  // save registers
  slli t3, t1, 2; // 128
  la t0, task_struct;
  add t0, t0, t3;
  SAVE_REG_T0;

  la t0, idle_address;
  add t0, t0, t1;
  sw a0, 0(t0);
  sw a1, 4(t0);
  sw a2, 8(t0);
  sw a3, 12(t0);
  sw a4, 16(t0);
  csrr t1, mepc; // set up the epc
  sw t1, 20(t0);
  addi t0, t0, 24;
  li t1, 1;
  amoswap.w t1, t1, 0(t0);
  csrr t1, mhartid
  addi t1, t1, 16; // we offset thread id by 0x10, or 16
  mv a0, t1; // thread id
  ret;

# thread mask is used to annotate which thread is idle
.global thread_mask;
thread_mask:
  .dword 0;
  .dword 0;
  .dword 0;
  .dword 0;

// currently this struct only stores registers
.pushsection .task_struct, "aw"
.global task_struct
task_struct:
  .skip 128
  .skip 128
  .skip 128
  .skip 128
set_child_tid:
  .skip 4
  .skip 4
  .skip 4
  .skip 4
