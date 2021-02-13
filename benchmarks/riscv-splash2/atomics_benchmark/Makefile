TARGET = host
MABI := lp64
MARCH := rv64ia
ifeq ($(TARGET), host)
	CC=gcc
else
	CC=riscv64-unknown-linux-gnu-gcc
endif

ifeq ($(TARGET), host)
	CFLAGS=
else
	CFLAGS=-fno-stack-protector -static -Wl,-z,norelro -mabi=$(MABI) -march=$(MARCH) -Wl,-e__start -lpthread -Wl,-T../utils/link.ld -g
	COMMON:= clone.o context.o exit.o futex.o idle.o intrinsics.o start.o trap_vector.o
	COMMON:= $(addprefix ../utils/, $(COMMON))
endif


# these are common functions in the utility folder, for interfacing with the host
common:
	make -C ../utils all

pthread_lock: pthread_lock.c $(COMMON)
	$(CC) -o $@ $^ -pthread

atomic_add: atomic_add.c $(COMMON)
	$(CC) -o $@ $^ $(CFLAGS) -pthread

atomic_add_new: atomic_add_new.c $(COMMON)
	$(CC) -o $@ $^ $(CFLAGS) -pthread

atomic_add_hard: atomic_add_hard.c $(COMMON)
	$(CC) -o $@ $^ $(CFLAGS) -pthread

atomic_add_carp: atomic_add_carp.c $(COMMON)
	$(CC) -o $@ $^ $(CFLAGS) -pthread

atomic_add_micro: atomic_add_micro.c $(COMMON)
	$(CC) -o $@ $^ $(CFLAGS) -pthread

non_atomic_add_micro: non_atomic_add_micro.c $(COMMON)
	$(CC) -o $@ $^ $(CFLAGS) -O3 -pthread

atomic_add_clone: atomic_add_clone.c $(COMMON)
	$(CC) -o $@ $^ $(CFLAGS) -pthread

all: pthread_lock atomic_add
