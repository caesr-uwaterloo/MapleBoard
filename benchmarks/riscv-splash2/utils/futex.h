#pragma once
#include <stdint.h>

int __futex(int *uaddr, int futex_op, int val,
                 uint32_t val2,
                 int *uaddr2, int val3); 