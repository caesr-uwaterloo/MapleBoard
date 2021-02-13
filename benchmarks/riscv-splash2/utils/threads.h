#ifndef _RV_THREADS_H
#define _RV_THREADS_H

// stores all the general purpose register values
typedef struct {

} register_t;

typedef struct {
  long       clear_child_tid;
  long       set_child_tid;
  register_t registers;
} thread_context_t;

#endif
