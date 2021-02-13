#include "context.h"
#include "idle.h"
#include "intrinsics.h"

#include <sys/types.h>

// copied from kernel code
#define CLONE_PARENT_SETTID	 0x00100000	/* set the TID in the parent */
#define CLONE_CHILD_CLEARTID 0x00200000	/* clear the TID in the child */
#define CLONE_CHILD_SETTID   0x01000000	/* set the TID in the child */
/**
 * a simple implementation of the clone system call
 */
unsigned long __get_idle_core() {
    for(unsigned int i = 0; i < N_CORE; i++) {
        int val = __atomic_load_n(&thread_mask[i], __ATOMIC_SEQ_CST);
        if(val == 0) {
            return i;
        }
    }
    return (unsigned long)-1;
}
/**
 * NOTE: the clone function has different signatures than
 * the linux system call
 */
unsigned long __clone_rt(
    int   flags,
    void* child_stack,
    void* parent_tidptr,
    void* tls,
    void* child_tidptr
) {
    unsigned long idle_core = __get_idle_core();
    // busy loop if no idle core found
    while(idle_core == -1);

    unsigned long current_core = __getmhartid();

    
    /* copy registers */
    for(int i = 0; i < 32; i++) {
        context[idle_core].x[i] = context[current_core].x[i];
    }

    context[idle_core].x[2] = (unsigned long)child_stack;
    context[idle_core].x[4] = (unsigned long)tls;
    context[idle_core].epc  = __getmepc();

    context[idle_core].clear_child_tid = 0;
    if((flags & CLONE_CHILD_SETTID) != 0) {
        context[idle_core].set_child_tid = (unsigned long)child_tidptr;
        *(pid_t*)child_tidptr = idle_core + 1;
    }
    if((flags & CLONE_PARENT_SETTID) != 0) {
        *(pid_t*)parent_tidptr = idle_core + 1;
    }
    if((flags & CLONE_CHILD_CLEARTID) != 0) {
        context[idle_core].clear_child_tid = (unsigned long)child_tidptr;
    } else {
        context[idle_core].clear_child_tid = 0;
    }

    __atomic_exchange_n(&start_task[idle_core], 1, __ATOMIC_ACQ_REL);

    return idle_core + 1;
}
