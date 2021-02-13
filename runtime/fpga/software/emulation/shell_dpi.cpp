#include "VVU9PTop__Dpi.h"

#include "loguru.hpp"

void init() {
    LOG_F(INFO, "SCOPE: %s", svGetNameFromScope(svGetScope()));
}

unsigned long long read64(unsigned long long addr) {
    unsigned long long data = *((unsigned long long *)addr);
    // LOG_F(INFO, "Reading from %lx %lx", addr, data);
    return data;
}

void write64(unsigned long long addr, unsigned long long value) {
    // LOG_F(INFO, "Writing to %lx = %lx", addr, value);
    *((unsigned long long*)addr) = value;
}

