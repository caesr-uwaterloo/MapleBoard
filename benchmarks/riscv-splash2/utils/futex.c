/**
 * Temporarily remove the futex system call.
 * As the correct implementation should still be correct if the futex calls are reomved.
 */
#include <stdint.h>
#include <linux/futex.h>
#include <sys/time.h>
#include <errno.h>
#include "context.h"

extern unsigned long do_syscall();

#define LOCKED   1
#define UNLOCKED 0

extern int errno;

#ifdef __x86_64__
#include <pthread.h>
#include <stdio.h>
#include <string.h>
#else
#include "intrinsics.h"
#endif
// This macro is adapted from glibc header file atomic-machines
//#define asm_amo(which, mem, value) ({ 		\
//  typeof(*mem) __tmp; 						\
//    asm volatile (which ".w" "\t%0, %z2, %1"		\
//		  : "=r"(__tmp), "+A"(*(mem))			\
//		  : "rJ"(value));				\
//  __tmp; })

// the return value indicates whether the value is 0 or not
//#define test_and_set(mem) asm_amo("amoswap", mem, 1)
//#define atomic_reset(mem) asm_amo("amoswap", mem, 0)
// N_BUCKET must be the power of 2
#define N_BUCKET 256

// each hash_bucket will only place one `futex_q` like object.
// and thus the hash_buckets is actually similar to futex_queues in kernel
// currently, all of the cores share the same data structure...
int biglock = 0;
int guard = 0;

volatile int simple_counter = 0;
volatile unsigned long failing_point[N_CORE * CACHE_LINE_SIZE / sizeof(unsigned long)];
// used for synchronizing
struct __attribute__((aligned(64))) atom_t {
    unsigned long waiting;
    unsigned long waiting_on;
    char padding[64 - sizeof(unsigned long) - sizeof(unsigned long)];
} atoms[N_CORE];

struct hash_bucket {
  unsigned long addr;
  unsigned long valid;
  unsigned long waiters;
} hash_buckets[N_BUCKET];




void __lock_futex() {
  int res = 0;
  while((res = __sync_lock_test_and_set(&biglock, 1)) != 0) ;
  return ;
}

void __release_futex() {
  __sync_lock_release(&biglock);
}

int __get_bucket_idx(void* addr) {
    for(int i = 0; i < N_BUCKET; i++) {
        if(hash_buckets[i].valid == 1 && hash_buckets[i].addr == (unsigned long)addr) {
            return i;
        }
    }
    return -1;
}

int __allocate_bucket(void* addr) {
    unsigned long hash = (((unsigned long)addr) >> 3) % N_BUCKET;
    for(int i = 0; i < N_BUCKET; i++, hash++ ) {
        if(hash >= N_BUCKET) hash = 0;
        if(hash_buckets[i].valid == 0) {
            hash_buckets[i].valid = 1;
            hash_buckets[i].waiters = 0;
            hash_buckets[i].addr = (unsigned long)addr;
            return i;
        }
    }
    return -1;
}
void __spin_wait() {
    unsigned long core = __getmhartid();
    unsigned long res;
    while((res = __atomic_load_n(&atoms[core].waiting, __ATOMIC_SEQ_CST)) != 0) {
        ;
    }
    // __atomic_store_n(&atoms[core].waiting, 0, __ATOMIC_SEQ_CST);
    __atomic_store_n(&atoms[core].waiting_on, 0, __ATOMIC_SEQ_CST);
    return;
}
/* this is the implementation of the futex system call */
//__attribute__((stdcall))
int __futex(int *uaddr, int futex_op, int val,
                 uint32_t val2,
                 int *uaddr2, int val3) {
  if(!(
        (futex_op == FUTEX_WAIT_PRIVATE) ||
        (futex_op == FUTEX_WAKE_PRIVATE) ||
        (futex_op == FUTEX_WAKE) || 
        (futex_op == FUTEX_WAIT)
      )) {
    // errno = ENOSYS;
    failing_point[__getmhartid() * CACHE_LINE_SIZE / sizeof(unsigned long)] = 8888888ul;
    return -ENOSYS;
  }

  // serialization point
  __lock_futex();

  int idx = -1;

  if(futex_op == FUTEX_WAIT_PRIVATE || futex_op == FUTEX_WAIT) {
    int i = __get_bucket_idx(uaddr);
    if(i == -1) { i = __allocate_bucket(uaddr); }
    if(i == -1) { while(1); } // no space then simply spin
    // 32bit value so...
    int v = *(int*)uaddr;
    if(v == val) {
        __atomic_fetch_add(&hash_buckets[i].waiters, 1, __ATOMIC_SEQ_CST);
        /** note this is incompatible with x86 */
        unsigned long core = __getmhartid();
        __atomic_store_n(&atoms[core].waiting, 1, __ATOMIC_SEQ_CST);
        __atomic_store_n(&atoms[core].waiting_on, uaddr, __ATOMIC_SEQ_CST);
        __release_futex();
        __spin_wait();
    } else {
        __release_futex();
        // errno = EAGAIN;
        failing_point[__getmhartid() * CACHE_LINE_SIZE / sizeof(unsigned long)] = v;
        return -EAGAIN;
    }
    return 0;
  } else if(futex_op == FUTEX_WAKE_PRIVATE || futex_op == FUTEX_WAKE) {
    int i = __get_bucket_idx(uaddr);
    if(i == -1) { i = __allocate_bucket(uaddr); }
    if(i == -1) { while(1); } // no space then simply spin
    int towake = __atomic_load_n(&hash_buckets[i].waiters, __ATOMIC_SEQ_CST);
    int res = 0;
    if(val > 0) towake = val > towake ? towake : val;
    for(int c = 0; c < 8 && towake > 0; c++) {
        unsigned long waiting = __atomic_load_n(&atoms[c].waiting, __ATOMIC_SEQ_CST);
        unsigned long waiting_on = __atomic_load_n(&atoms[c].waiting_on, __ATOMIC_SEQ_CST);
        if(waiting == 1 && waiting_on == (unsigned long)uaddr) {
            __atomic_fetch_add(&hash_buckets[i].waiters, -1, __ATOMIC_SEQ_CST);
            __atomic_store_n(&atoms[c].waiting, 0, __ATOMIC_SEQ_CST);
            towake--;
            res++;
        }
    }
    towake = __atomic_load_n(&hash_buckets[i].waiters, __ATOMIC_SEQ_CST);
    if(towake == 0) {
        __atomic_store_n(&hash_buckets[i].valid, 0, __ATOMIC_SEQ_CST);
        __atomic_store_n(&hash_buckets[i].addr, 0, __ATOMIC_SEQ_CST);
        __atomic_store_n(&hash_buckets[i].waiters, 0, __ATOMIC_SEQ_CST);
    } // release the resource
    __release_futex();
    return res;
  }
  __release_futex();
  return 0;
}

// These programs are for test purpose on x86 arch
#ifdef __x86_64__

int pseudo_mutex_lock(int* lock, int idx) {
  int c;
  while( (c = __sync_lock_test_and_set(lock, 1)) != 0) {
    futex(lock, FUTEX_WAIT_PRIVATE, 1, 0, 0, 0);
  }
}

int pseudo_mutex_unlock(int* lock, int idx) {
  // caller must own the lock
  __sync_lock_release(lock);
  futex(lock, FUTEX_WAKE_PRIVATE, -1, 0, 0, 0);
  return 0;
}

int lock = 0, lock1 = 0;
int val = 0, val1 = 0;

void* threads(void* idx) {
  long tid = (long)idx;
  int a;
  double q = 2.0;
  for(int i = 0; i < 10000; i++) {
    // RMW
    pseudo_mutex_lock(&lock, tid);
    a = val;
    a += 1;
    val = a;
    q *= val;
    for(int i = 0; i < 10000; i++);
    // printf("Thread %ld, %d\n", tid, val);
    pseudo_mutex_unlock(&lock, tid);

    /*
    pseudo_mutex_lock(&lock1, tid);
    a = val1;
    for(int j = 0; j < (idx ==0 ? 100 : 500); j++);
    a += 1;
    val1 = a;
    // printf("Thread %ld, %d\n", tid, val);
    pseudo_mutex_unlock(&lock1, tid);
    */

  }
  printf("%lf\n", q);
  return 0;
}

/*
 * this is the reference program for using test_and_set, which works fine
void* threads(void* idx) {
  int res;
  for(int i = 0; i < 100000; i++) {
    do {
      res = test_and_set(&lock);
    } while(res != 1);
    int* a = idx;
    *a = *a + 1;
    atomic_reset(&lock);
  }
}
*/

#define N_THREAD 4
pthread_t pid[N_THREAD];

int main() {
  printf("Starting...\n");


  for(long i = 0; i < N_THREAD; i++) {
    pthread_create(&pid[i], NULL, &threads, (void*)i);
    printf("%lx -> %ld\n", pid[i], i);
  }


  for(int i = 0; i < N_THREAD; i++) {
    pthread_join(pid[i], NULL);
  }

  printf("Whole program done. \n");
  printf("The final value of val: %d, %d\n", val, val1);
  return 0;
}
#endif
