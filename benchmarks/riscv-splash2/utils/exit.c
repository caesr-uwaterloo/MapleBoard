#include "context.h"
#include "intrinsics.h"

void __exit() {
    unsigned long coreid = __getmhartid();
    unsigned long* clear_child_tid = (unsigned long*)context[coreid].clear_child_tid;
    *clear_child_tid = 0;
}