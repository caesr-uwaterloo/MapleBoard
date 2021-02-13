LINE?=32
CORE?=4
BUS?=shared
PROTO?=carp
THIS_FOLDER := $(FPGA_ROOT)/software/emulation
TARGET_DIR := ../../build/pcie_verilator/
V_SRC := ../../design/shell.sv ../../design/vfolder/${PROTO}.${CORE}c.${LINE}.${BUS}/VU9PTop.v \
  ../../design/vfolder/${PROTO}.${CORE}c.${LINE}.${BUS}/DualPortedRamBB.v  \
  ../../design/slot.sv ../../design/conf.sv
CXX_SRC := $(THIS_FOLDER)/pcie_verilator.cpp $(THIS_FOLDER)/zmq_daemon.cpp $(THIS_FOLDER)/loguru.cpp $(THIS_FOLDER)/shell_dpi.cpp

VERILATOR_CFLAGS:=-DVERILATOR -g -std=c++14

# We use multi-threaded simulation ,wehre all dpi's are thread save and we use up to 8 threads...
compile:
	mkdir -p $(TARGET_DIR)
	verilator --Mdir $(TARGET_DIR) \
		+1800-2017ext+sv  \
		-CFLAGS "$(VERILATOR_CFLAGS)"  \
		+define+PRINTF_COND=0 \
		-LDFLAGS "-lpthread -ldl -lzmq" \
		--top-module VU9PTop \
		-sv \
		--cc \
		--exe $(V_SRC) $(CXX_SRC) \
		--trace \
		--trace-depth 99
	make -C $(TARGET_DIR) -j -f VVU9PTop.mk VVU9PTop

launch_daemon: compile
	$(TARGET_DIR)/VVU9PTop

sender:
	g++ -std=c++14 command_sender.cpp -o sender -lzmq
