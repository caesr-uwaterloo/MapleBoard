#pragma once
#include "context.h"

extern void __idle();
extern int thread_mask[N_CORE];
extern int start_task[N_CORE];
