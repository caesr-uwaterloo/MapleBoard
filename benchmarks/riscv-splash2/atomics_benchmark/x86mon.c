#define N_CORE 12
#define CACHE_LINE_SIZE 64
// should be set to zero at the beginning
volatile unsigned long __attribute__((aligned(CACHE_LINE_SIZE), section(".monaddr"))) monaddr[N_CORE];
// whether there is a monitor core (non-crit)
volatile unsigned long __attribute__((alinged(CACHE_LINE_SIZE), section(".has_mon"))) has_mon;
// the id of the monitor core
volatile unsigned long __attribute__((alinged(CACHE_LINE_SIZE), section(".mon_core"))) mon_core;


