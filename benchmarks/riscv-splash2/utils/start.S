#include "riscv_test.h"
#include "macros.h"

// TODO: the macro is defined also in context.h
// should be factored out
#define KERNEL_STACK_SIZE 8192

// the following lines will be moved to env.h
// also defined in asm/asm.h
#if __riscv_xlen == 64
# define REG_S sd
# define REG_L ld
#else
# define REG_S sw
# define REG_L lw
#endif

#if __riscv_xlen == 64
# define REG_OFFSET(regid) regid * 8
# define KERNEL_OFFSET (8 * 32)
# define CAUSE_OFFSET 8 * 32
# define EPC_OFFSET 8 * 33
# define STACK_OFFSET 8 * 34
#else
# define REG_OFFSET(regid) regid * 4
# define KERNEL_OFFSET (4 * 32)
# define CAUSE_OFFSET 4 * 32
# define EPC_OFFSET 4 * 33
# define STACK_OFFSET 4 * 34
#endif

/**
 * the register saving routine
 */
#define ECALL_SAVE_REGS \
  REG_S x0,  REG_OFFSET(0)(tp);  \
  REG_S x1,  REG_OFFSET(1)(tp);  \
  REG_S x2,  REG_OFFSET(2)(tp);  \
  REG_S x3,  REG_OFFSET(3)(tp);  \
  REG_S x5,  REG_OFFSET(5)(tp);  \
  REG_S x6,  REG_OFFSET(6)(tp);  \
  REG_S x7,  REG_OFFSET(7)(tp);  \
  REG_S x8,  REG_OFFSET(8)(tp);  \
  REG_S x9,  REG_OFFSET(9)(tp);  \
  REG_S x10, REG_OFFSET(10)(tp); \
  REG_S x11, REG_OFFSET(11)(tp); \
  REG_S x12, REG_OFFSET(12)(tp); \
  REG_S x13, REG_OFFSET(13)(tp); \
  REG_S x14, REG_OFFSET(14)(tp); \
  REG_S x15, REG_OFFSET(15)(tp); \
  REG_S x16, REG_OFFSET(16)(tp); \
  REG_S x17, REG_OFFSET(17)(tp); \
  REG_S x18, REG_OFFSET(18)(tp); \
  REG_S x19, REG_OFFSET(19)(tp); \
  REG_S x20, REG_OFFSET(20)(tp); \
  REG_S x21, REG_OFFSET(21)(tp); \
  REG_S x22, REG_OFFSET(22)(tp); \
  REG_S x23, REG_OFFSET(23)(tp); \
  REG_S x24, REG_OFFSET(24)(tp); \
  REG_S x25, REG_OFFSET(25)(tp); \
  REG_S x26, REG_OFFSET(26)(tp); \
  REG_S x27, REG_OFFSET(27)(tp); \
  REG_S x28, REG_OFFSET(28)(tp); \
  REG_S x29, REG_OFFSET(29)(tp); \
  REG_S x30, REG_OFFSET(30)(tp); \
  REG_S x31, REG_OFFSET(31)(tp)
  
#define ECALL_RESTORE_REGS \
  REG_L x0,  REG_OFFSET(0)(tp);  \
  REG_L x1,  REG_OFFSET(1)(tp);  \
  REG_L x2,  REG_OFFSET(2)(tp);  \
  REG_L x3,  REG_OFFSET(3)(tp);  \
  REG_L x5,  REG_OFFSET(5)(tp);  \
  REG_L x6,  REG_OFFSET(6)(tp);  \
  REG_L x7,  REG_OFFSET(7)(tp);  \
  REG_L x8,  REG_OFFSET(8)(tp);  \
  REG_L x9,  REG_OFFSET(9)(tp);  \
  REG_L x10, REG_OFFSET(10)(tp); \
  REG_L x11, REG_OFFSET(11)(tp); \
  REG_L x12, REG_OFFSET(12)(tp); \
  REG_L x13, REG_OFFSET(13)(tp); \
  REG_L x14, REG_OFFSET(14)(tp); \
  REG_L x15, REG_OFFSET(15)(tp); \
  REG_L x16, REG_OFFSET(16)(tp); \
  REG_L x17, REG_OFFSET(17)(tp); \
  REG_L x18, REG_OFFSET(18)(tp); \
  REG_L x19, REG_OFFSET(19)(tp); \
  REG_L x20, REG_OFFSET(20)(tp); \
  REG_L x21, REG_OFFSET(21)(tp); \
  REG_L x22, REG_OFFSET(22)(tp); \
  REG_L x23, REG_OFFSET(23)(tp); \
  REG_L x24, REG_OFFSET(24)(tp); \
  REG_L x25, REG_OFFSET(25)(tp); \
  REG_L x26, REG_OFFSET(26)(tp); \
  REG_L x27, REG_OFFSET(27)(tp); \
  REG_L x28, REG_OFFSET(28)(tp); \
  REG_L x29, REG_OFFSET(29)(tp); \
  REG_L x30, REG_OFFSET(30)(tp); \
  REG_L x31, REG_OFFSET(31)(tp)

/**
 * the propose of the entry point is to set up the environment for glibc,
 * for example, the trap_vector handler etc
 */
.globl __start;
.globl trap_vector;
.globl main;
.globl thread_mask;
.globl start_task;
.globl context;
.globl _restore_reg_return;
.globl __get_kernel_stack_end;
.globl __idle;
.globl kernel_stack;
.globl __global_pointer$;

__start:
  j reset_vector;
  .align 2;

/**
 * save the registers and call trap vector
 * swap out the mscratch register
 * we also assume that all syscalls comming from user space so tp will always be non-zero
 */
__trap_vector:
1:
  csrrw tp, mscratch, tp; // should not be 0
  // nested system call not supported
  beqz tp, 1b; // tp is p_context

_save_context:
  ECALL_SAVE_REGS;
  // save tp
  csrr t0, mscratch;
  REG_S t0, REG_OFFSET(4)(tp);
  csrw mscratch, zero;
  csrr t0, mcause;
  REG_S t0, CAUSE_OFFSET(tp);
  csrr t0, mepc;
  REG_S t0, EPC_OFFSET(tp);
  /* setup kernel stack */
  REG_L sp, STACK_OFFSET(tp);
  call trap_vector;
_restore_reg_return:
  REG_L t0, EPC_OFFSET(tp);
  addi t0, t0, 4;
  csrw mepc, t0;
  REG_L t0, REG_OFFSET(4)(tp);
  csrw mscratch, t0;
  ECALL_RESTORE_REGS;
  csrrw tp, mscratch, tp; # swap in the context
  mret

reset_vector:
  DELEGATE_NO_TRAPS;
  la t0, __trap_vector;
  csrw  mtvec, t0;
  // setup the global pointer, glibc will not do this for us
  // the .option here prevents __global_pointer$ to be optimized to gp (relocation)
  // since we need global pointers even before the program start, so we
  // initialize it here redundantly
.option push
.option norelax
  lla gp, __global_pointer$;
.option pop
  // setup the pointer for saving registers
  csrr a0, mhartid;
  /* reset the start mask */
  la t0, start_task;
  slli t1, a0, 2;
  add t0, t0, t1;
  // we don't need this, C should always set this as zero
  // sw zero, (t0);
  /* set up kernel stack */
  la t0, kernel_stack;
1:
  li t1, KERNEL_STACK_SIZE;
  add  t0, t0, t1;
  beqz a0, 1f;
  addi a0, a0, -1;
  j 1b;
1:
  mv sp, t0;
  csrr a0, mhartid; // __get_context argument
  call __get_context;
  csrw  mscratch, a0;
  REG_S sp, STACK_OFFSET(a0);
  csrwi mstatus, 0;

  csrr t0, mhartid;
  beqz t0, 1f;
  call __idle;
  /* idle core is started by the main core */
  j _restore_reg_return;
  // RISCV_MULTICORE_ENABLE;
  /**
   * this will lead the core to run into the idle
   * function
   */
1:
  la t0, thread_mask;
  la t1, 1;
  REG_S t1, (t0);
  la t0, _start;
  csrw mepc, t0;
  /**
   * load initial stack pointer and clear the location
   * according to https://code.woboq.org/userspace/glibc/sysdeps/riscv/start.S.html
   * sp should point to argc
   */
  la sp, fromhost;
  REG_L sp, 0(sp);
  la t0, fromhost;
  REG_S zero, 0(t0);
  /* setup the tls address */
  la tp, __tdata_start;
  li a0, 0; // rtld_init, we don't need this, setting it wrong might cause problems
  mret;

/**
 * for tohost and fromhost
 */
RVTEST_DATA_BEGIN
RVTEST_DATA_END

ELFSPACE

