#include <pthread.h>
#include <stdio.h>

//pthread_spinlock_t mutex;
pthread_mutex_t mutex;
int shared_variable __attribute__((aligned(64)));
volatile unsigned long __attribute__((aligned(64))) lk;
volatile int flock __attribute__((aligned(64)));
volatile int tmp[1024];
int flock_pre;
int INC_BASE = 0;

static inline void lock() {
    // register int tmp;
    // register int k = 1;
    // asm volatile("1: lr.d %0, (%1)\n\t"
    // "bnez %2, 1b\n\t"
    // "sc.d %0, %3, (%1)\n\t"
    // "bnez %2, 1b\n\t"
    // :"=r"(tmp):"r"(&lk), "r"(tmp), "r"(k));
    // return;
  pthread_mutex_lock(&mutex);
  // unsigned long res = 0;
  // while((res = __atomic_exchange_n(&lk, 1, __ATOMIC_SEQ_CST)) != 0) ;
}

static inline void unlock() {
  // __atomic_exchange_n(&lk, 0, __ATOMIC_SEQ_CST);
  pthread_mutex_unlock(&mutex);
  // pthread_spin_unlock(&mutex);
  // __atomic_exchange_n(&lk, 0, __ATOMIC_SEQ_CST);
}

static inline void testlrsc() {
    register int upb = 2 * INC_BASE;
    register int* p_shared = &shared_variable;
    register int i, k;
    for(i = 0; i < upb; i++) {
      register int tmp;
      register int k = 0;
      asm volatile(
          "1: lr.w %0, (%1)\n\t"
          "addi %0, %0,1\n\t"
          "nop\n\t"
          "nop\n\t"
          //"sw %0, (%4)\n\t"
          "sc.w %3, %0, (%4)\n\t"
          "bnez %3, 1b\n\t"
          :"=r"(tmp):"r"(p_shared), "r"(tmp), "r"(k), "r"(p_shared));
      for(k = 0; k < (i & 3); k++);
    }
}

void* threadFunc(void* arg) {

  for (int i = 0; i < 2 * INC_BASE; i++) {
    // __atomic_fetch_add(&shared_variable, 1, __ATOMIC_SEQ_CST);
    lock();

    for(int j = 0; j < (i & 15); j++) tmp[j] = i;
    int x = shared_variable++;
    unlock();
    
  }
  printf("..... hello from thread %lld .....\n", (unsigned long)arg);
  fflush(stdout);
  printf("xxxxx hello from thread %lld xxxxx\n", (unsigned long)arg);
  fflush(stdout);
   __atomic_fetch_add(&flock, 1, __ATOMIC_SEQ_CST);
}

int main() {
  INC_BASE = 5000;
  // pthread_spin_init(&lock, PTHREAD_PROCESS_PRIVATE); 
  printf("===== ATOMIC_ADD benchmark =====\n");
  fflush(stdout);
  shared_variable = 0;
  __atomic_store_n(&flock, 0, __ATOMIC_SEQ_CST);

  pthread_t thread[2];
  pthread_create(&thread[0], NULL, threadFunc, (void*)1);
  pthread_create(&thread[1], NULL, threadFunc, (void*)2);


  for (int i = 0; i < 2 * INC_BASE; i++) {
    // __atomic_fetch_add(&shared_variable, 1, __ATOMIC_SEQ_CST);
    lock();
    shared_variable++;
    // for(int k = 0; k < i % 10; k++) ;
    unlock();
  }
  
  // manual join
  int k;
  while((k = __atomic_load_n(&flock, __ATOMIC_SEQ_CST)) != 2) ;

  // pthread_join(thread[0], NULL);
  // pthread_join(thread[1], NULL);

  printf("))))) ATOMIC_ADD benchmark ends, threads joined, checking... (((((\n");
  fflush(stdout);

  if(shared_variable != 6 * INC_BASE) {
    printf("***** Failed: expected %d, actual %d *****\n", 6 * INC_BASE, shared_variable);
  } else {
    printf("ooooo Passed: expected %d, actual %d ooooo\n", 6 * INC_BASE, shared_variable);
  }
  fflush(stdout);
  return 0;
}
