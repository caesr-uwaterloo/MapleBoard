# A list of splash2 programs
pthread_lock: compile
	$(SIM_DIR)/$(RT) -m $(MEM_SIZE) $(SPLASH2_ROOT)/../atomics_benchmark/pthread_lock -v 2
ifeq ($(TARGET), verilator)
atomic_add: compile
	env -i $(SIM_DIR)/$(RT) -m $(MEM_SIZE) $(SPLASH2_ROOT)/../atomics_benchmark/atomic_add -v 2
else
atomic_add: compile
	sudo $(SIM_DIR)/$(RT) -m $(MEM_SIZE) $(SPLASH2_ROOT)/../atomics_benchmark/atomic_add -v 3
endif

