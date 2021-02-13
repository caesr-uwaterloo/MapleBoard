#include "context.h"
#include "intrinsics.h"
context_t* __get_context(int coreid) {
    return &context[coreid];
}

unsigned long* __get_register_save(int coreid) {
    return &context[coreid].x[0];
}

unsigned long* __get_cause(int coreid) {
    return &context[coreid].cause;
}

void* __get_kernel_stack_end() {
    unsigned long coreid = __getmhartid();
    return &kernel_stack[coreid][KERNEL_STACK_SIZE];
}

unsigned long __set_tid_address(void* addr) {
    unsigned long coreid = __getmhartid();
    context[coreid].clear_child_tid = (unsigned long)addr;
    return 0;
}

