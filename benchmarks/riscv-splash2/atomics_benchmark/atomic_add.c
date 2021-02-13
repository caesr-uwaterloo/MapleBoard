#include <pthread.h>
#include <stdio.h>

//pthread_spinlock_t mutex;
pthread_mutex_t mutex;
long long  shared_variable __attribute__((aligned(64)));
volatile int tmp[1024];
long long per_thread_increment = 10000000;

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

void* threadFunc(void* arg) {

  for (int i = 0; i < per_thread_increment; i++) {
    __atomic_fetch_add(&shared_variable, 1, __ATOMIC_SEQ_CST);
    // lock();
    // int x = shared_variable++;
    // unlock();
    
  }
}

int main() {
  // pthread_spin_init(&lock, PTHREAD_PROCESS_PRIVATE); 
  printf("===== ATOMIC_ADD benchmark =====\n");
  fflush(stdout);
  shared_variable = 0;

  pthread_t thread[7];
  pthread_create(&thread[0], NULL, threadFunc, (void*)1);
  pthread_create(&thread[1], NULL, threadFunc, (void*)2);
  pthread_create(&thread[2], NULL, threadFunc, (void*)1);
  // pthread_create(&thread[3], NULL, threadFunc, (void*)2);
  // pthread_create(&thread[4], NULL, threadFunc, (void*)1);
  // pthread_create(&thread[5], NULL, threadFunc, (void*)2);
  // pthread_create(&thread[6], NULL, threadFunc, (void*)2);

  for (int i = 0; i < per_thread_increment; i++) {
    __atomic_fetch_add(&shared_variable, 1, __ATOMIC_SEQ_CST);
    // lock();
    // shared_variable++;
    // for(int k = 0; k < i % 10; k++) ;
    // unlock();
  }
  
  // manual join
  pthread_join(thread[0], NULL);
  pthread_join(thread[1], NULL);
  pthread_join(thread[2], NULL);
  // pthread_join(thread[3], NULL);
  // pthread_join(thread[4], NULL);
  // pthread_join(thread[5], NULL);
  // pthread_join(thread[6], NULL);

  printf("))))) ATOMIC_ADD benchmark ends, threads joined, checking... (((((\n");
  fflush(stdout);

  if(shared_variable != 8 * per_thread_increment) {
    printf("***** Failed: expected %lld, actual %lld *****\n", 8 * per_thread_increment, shared_variable);
  } else {
    printf("ooooo Passed: expected %lld, actual %lld ooooo\n", 8 * per_thread_increment, shared_variable);
  }
  fflush(stdout);
  return 0;
}
