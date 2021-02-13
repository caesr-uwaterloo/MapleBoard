#include "intrinsics.h"
#include "context.h"
#include "idle.h"

extern void _restore_reg_return;
int thread_mask[N_CORE];
int start_task[N_CORE];

void __set_thread_mask() {
    unsigned long hartid = __getmhartid();
    __atomic_store_n(&thread_mask[hartid], 1, __ATOMIC_SEQ_CST);
}

void __reset_thread_mask() {
    unsigned long hartid = __getmhartid();
    __atomic_store_n(&thread_mask[hartid], 0, __ATOMIC_SEQ_CST);
}
// whether there is a monitor core (non-crit)
extern volatile unsigned long has_mon;
// the id of the monitor core
extern volatile unsigned long mon_core;
extern void monitor();



/**
 * __idle could be called from reset_vector or trap_vector
 */
void __idle() {
    // divert to the monitor process
    unsigned long thishartid = __getmhartid();
    if(has_mon && thishartid == mon_core) {
      monitor();
    }
    __reset_thread_mask();
    __atomic_exchange_n(&start_task[thishartid], 0, __ATOMIC_SEQ_CST);


    int res;
    while((res = __atomic_exchange_n(&start_task[thishartid], 0, __ATOMIC_ACQ_REL)) == 0) {
        /**
         * empty loop, wait for new task
         */
    }
    __set_thread_mask();
    // offset to return address
    // no need to do this
    // context[thishartid].epc += 4;
    context[thishartid].x[10] = 0;

    unsigned long context_addr;
    context_addr = (unsigned long)&context[thishartid]; // restore the context overwritten
    __setmepc(context[thishartid].epc);
    __asm__ volatile("mv tp, %0" :: "r"(context_addr));
    __asm__ volatile("j _restore_reg_return\n\t");
}
// should jump to the _restore_reg_return label
