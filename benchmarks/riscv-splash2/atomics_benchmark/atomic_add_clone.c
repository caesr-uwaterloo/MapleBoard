#include <pthread.h>
#include <stdio.h>
#include <sched.h>

void* run(void* args) {
  for(int i = 0; i < 20; i++) {
    __atomic_fetch_add(&x, 1, __ATOMIC_CST_SEQ);
  }

  printf("thread %lld done.\n", (unsigned long)args); 
  fflush(stdout);

loopback:
  goto loopback;
}

int main() {
  x = 0;
  printf("===== ATOMIC_ADD_CLONE =====\n");

  pthread_t thread[2];
  pthread_create(&thread[0], NULL, run, (void*)1);
  pthread_create(&thread[1], NULL, run, (void*)2);



  return 0;
}
