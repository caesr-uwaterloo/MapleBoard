// this defines the top of the execution stack in user space
// by default this value corresponds to a 32MB memory
// this address will be loaded into sp on power up
#include "macros.h"
#define STACK_START (0x00FFFFFC + 0x10000)


#define DELEGATE_NO_TRAPS                                               \
  la t0, 1f;                                                            \
  csrw mtvec, t0;                                                       \
  csrwi medeleg, 0;                                                     \
  csrwi mideleg, 0;                                                     \
  csrwi mie, 0;                                                         \
  .align 2;                                                             \
1:

#define RVTEST_ENABLE_MACHINE                                           \
  li a0, MSTATUS_MPP;                                                   \
  csrs mstatus, a0;                                                     \

#define CAUSE_USER_ECALL       0x8
#define CAUSE_SUPERVISOR_ECALL 0x9
#define CAUSE_MACHINE_ECALL    0xb
#define MSTATUS_MPP            0x00001800

#define RISCV_MULTICORE_DISABLE \
  csrr a0, mhartid;             \
  1: bnez a0, 1b

#define RISCV_MULTICORE_ENABLE \
  la   a0, ncore;              \
  li   t1, 1;                  \
  amoadd.w t1, t1, 0(a0);      \
  csrr a0, mhartid;             \
  bnez a0, __idle_rt;

#define RESTORE_REG_T0 \
  lw x0, 0(t0); \
  lw x1, 4(t0);  /*lw x2, 8(t0);*/ \
  lw x3, 12(t0); /*lw x4, 16(t0);lw x5, 20(t0);*/ \
  lw x6, 24(t0); \
  lw x7, 28(t0); \
  lw x8, 32(t0); \
  lw x9, 36(t0); \
  lw x10, 40(t0); /* lw x11, 44(t0); */ \
  lw x12, 48(t0); \
  lw x13, 52(t0); \
  lw x14, 56(t0); \
  lw x15, 60(t0); \
  lw x16, 64(t0); \
  lw x17, 68(t0); \
  lw x18, 72(t0); \
  lw x19, 76(t0); \
  lw x20, 80(t0); \
  lw x21, 84(t0); \
  lw x22, 88(t0); \
  lw x23, 92(t0); \
  lw x24, 96(t0); \
  lw x25, 100(t0); \
  lw x26, 104(t0); \
  lw x27, 108(t0); \
  lw x28, 112(t0); \
  lw x29, 116(t0); \
  lw x30, 120(t0); \
  lw x31, 124(t0); \
  lw t0, 20(t0)

#define SAVE_REG_T0 \
  sw x0, 0(t0); \
  sw x1, 4(t0); \
  sw x2, 8(t0); \
  sw x3, 12(t0); \
  sw x4, 16(t0); \
  sw x5, 20(t0); \
  sw x6, 24(t0); \
  sw x7, 28(t0); \
  sw x8, 32(t0); \
  sw x9, 36(t0); \
  sw x10, 40(t0); \
  sw x11, 44(t0); \
  sw x12, 48(t0); \
  sw x13, 52(t0); \
  sw x14, 56(t0); \
  sw x15, 60(t0); \
  sw x16, 64(t0); \
  sw x17, 68(t0); \
  sw x18, 72(t0); \
  sw x19, 76(t0); \
  sw x20, 80(t0); \
  sw x21, 84(t0); \
  sw x22, 88(t0); \
  sw x23, 92(t0); \
  sw x24, 96(t0); \
  sw x25, 100(t0); \
  sw x26, 104(t0); \
  sw x27, 108(t0); \
  sw x28, 112(t0); \
  sw x29, 116(t0); \
  sw x30, 120(t0); \
  sw x31, 124(t0)

#define FUNC_CALL_SAVE_REGS_T0_NO_A0 \
  sw ra, 0(t0); \
  sw a1, 8(t0); \
  sw a2, 12(t0); \
  sw a3, 16(t0); \
  sw a4, 20(t0); \
  sw a5, 24(t0); \
  sw a6, 28(t0); \
  sw a7, 32(t0)

#define FUNC_CALL_RESTORE_REGS_T0_NO_A0 \
  lw ra, 0(t0); \
  lw a1, 8(t0); \
  lw a2, 12(t0); \
  lw a3, 16(t0); \
  lw a4, 20(t0); \
  lw a5, 24(t0); \
  lw a6, 28(t0); \
  lw a7, 32(t0)

// SYSTEM CALLs
#define NR_clone 220
#define NR_set_tid_address 96
#define NR_futex 98
#define NR_exit 93
#define NR_exit_group 94
