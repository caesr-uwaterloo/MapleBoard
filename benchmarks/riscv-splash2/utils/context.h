#pragma once
#define N_CORE 12
#define CACHE_LINE_SIZE 64
#define CACHE_SET_SIZE 32
#define log2_CACHE_LINE_SIZE 32 - __builtin_clz(CACHE_LINE_SIZE) - 1
#define KERNEL_STACK_SIZE 8192

typedef struct __attribute__((aligned(8))){
    unsigned long x[32];
    unsigned long cause;
    unsigned long epc;
    unsigned long stack;
    unsigned long set_child_tid;
    unsigned long clear_child_tid;
    unsigned long tval;
    unsigned char padding[64];
} context_t;

context_t context[N_CORE];

unsigned char kernel_stack[N_CORE][KERNEL_STACK_SIZE] __attribute__((aligned(64)));

context_t* __get_context(int coreid);
unsigned long* __get_register_save(int coreid);
unsigned long* __get_cause(int coreid);
void* __get_kernel_stack_end();
unsigned long __set_tid_address();
