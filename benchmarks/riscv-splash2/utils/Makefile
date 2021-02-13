
MARCH :=rv64ia
MABI  :=lp64

COMMONASM := start.o
COMMONC := trap_vector.o intrinsics.o context.o idle.o clone.o exit.o
COMMON := $(COMMONASM) $(COMMONC)

$(COMMONASM) : %.o : %.S
	$(GCC) -g -c -o $@ $^ -nostartfiles -march=$(MARCH) -mabi=$(MABI)

$(COMMONC) : %.o : %.c
	$(GCC) -g -O2 -c -o $@ $^ -nostartfiles -march=$(MARCH) -mabi=$(MABI)

futex.o : futex.c
	$(GCC) -g -c -o futex.o futex.c -fno-stack-protector -nostartfiles -mabi=$(MABI) -march=$(MARCH)


CROSS:= riscv64-unknown-linux-gnu-
GCC	 := $(CROSS)gcc


all: $(COMMON) $(COMMONC) futex.o

clean::
	rm -rf *.o
