/* the exit function will clear the clear_child_tid 
 * and call the futex wake before returning to the 
 * idle process
 */

#include "env.h"

.global __exit_rt;
__exit_rt:
  csrr t0, mhartid;
  la t1, clear_child_tid; // void* clear_child_tid
  slli t0, t0, 2;
  add t1, t1, t0;# t1 = clear_child_tid + mhartid * 4;
  lw t0, 0(t1); # t0 = *t1
  beqz t0, return_to_idle;
  sw zero, 0(t0); # *t0 = 0;
  # here we might loose the track of a0 because __futex_rt might rewrite
  # according to man page, the exit should call
  # futex(clear_child_tid, FUTEX_WAIT, 1, NULL, NULL, 0);
  # but we use FUTEX_WAIT_PRIVATE instead because of the limited support of the library
  mv a0, t0;
  li a1, 129; # FUTEX_WAKE_PRIVATE
  li a2, 1;
  li a3, 0;
  li a4, 0;
  li a5, 0;
  # no need to save ra since the function will not return
  jal __futex_rt;

return_to_idle:
  # this syscall will not return
  j __idle_rt;

  
