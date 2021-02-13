#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>

//pthread_spinlock_t mutex;
#define N_CORE 12
pthread_mutex_t mutex;
long S;

struct SharedVariable {
  char padding_0[64]; // padding with 64 bytes
  long long shared_variable[32 * 10][8];
  char padding_1[64];
} shared_variables[N_CORE];

long long per_thread_increment = 2000000;


extern unsigned long has_mon;
extern unsigned long monaddr[N_CORE];

void* threadFunc(void* arg) {
  int core_id = (long)arg;
  int idx = core_id % S;
  for (int i = 0; i < per_thread_increment; i++) {
    // if(i %10000 == 0) {
    //   printf("Core %d - %d\n", core_id, i);
    //   fflush(stdout);
    // }
    if(core_id == 0) {
      for(int j = 0; j < 5; j++) {
        for(int k = 0; k < 4; k++) {
          volatile long long* s = &shared_variables[idx].shared_variable[j * 32 + k][0];
          s[0] = 1;
        }
      }
    } else {

      for(int j = 0; j < 2; j++) {
        volatile long long* s = &shared_variables[idx].shared_variable[j * 32][0];
        s[0] = 1;
      }
      //   volatile long long* s = &shared_variables[idx].shared_variable[0][0];
      //   s[0] = 1;
    }

    // for(int j = 0 ; j < 5; j++) {
    //   volatile long long* s = &shared_variables[idx].shared_variable[j * 32][0];
    //   s[0] = 1;
    // }
    //
    // if((i & (1 << 12) == 0)) {
    //   printf("Core %d at %d\n", core_id, i);
    //   fflush(stdout);
    // }
    // __atomic_fetch_add(s, 1, __ATOMIC_SEQ_CST);
    // Don't use atomic, but simple writes
  }
}

pthread_t thread[8];

int main(int argc, char** argv) {
  // do not count the monitor thread. for carp, if there are 8 cores, then it's 7
  int i = 0;
  int n_thread; 

  if(argc < 3) {
    printf("Usage: ./a.out N S [per thread increment]\n");
    printf("  N: number of threads\n");
    printf("  S: Diverse factor (larger S represents larger less sharing)\n");
    fflush(stdout);
    return -1;
  }
  n_thread = atoi(argv[1]);
  S = atol(argv[2]);
  if(n_thread > N_CORE) {
    printf("N should not be greater than 12\n");
    fflush(stdout);
    return -1;
  }
  if(S > n_thread) {
    printf("S should not be greater than N\n");
    fflush(stdout);
    return -1;
  }

  if(argc == 4) {
    per_thread_increment = atol(argv[3]);
  }

  printf("===== ATOMIC_ADD benchmark (%d threads) =====\n", n_thread);
  fflush(stdout);
  for(int i = 0; i < N_CORE; i++) {
    // shared_variables[i].shared_variable = 0;
  }

  for(i = 0; i < n_thread; i++) {
    monaddr[i] = (unsigned long)&shared_variables[i % S].shared_variable;
  }

  for(i = 0; i < n_thread - 1; i++) {
    pthread_create(&thread[i], NULL, threadFunc, (void*)(i + 1ll));
  }

  // thread 0 is also doing something
  threadFunc((void*)0);

  // manual join
  for(i = 0; i < n_thread - 1; i++) {
    pthread_join(thread[i], NULL);
  }

  printf("))))) ATOMIC_ADD benchmark ends, threads joined, checking... (((((\n");
  fflush(stdout);

  // for(int i = 0; i < S; i++) {
  //   printf("Checking %d: Core ", i);
  //   int t = 0;
  //   for(int j = 0; j < n_thread; j++) {
  //     if(j % S == i) {
  //       printf("%d ", j);
  //       t ++;
  //     }
  //   }
  //   printf("\n");
  //   long long val = shared_variables[i].shared_variable;
  //   if(val != t * per_thread_increment) {
  //     printf("***** Failed: expected %lld, actual %lld *****\n", t * per_thread_increment, val);
  //   } else {
  //     printf("ooooo Passed: expected %lld, actual %lld ooooo\n", t * per_thread_increment, val);
  //   }
  //   fflush(stdout);
  // }
  return 0;
}
