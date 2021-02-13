TARGET  ?=verilator
SIM_DIR = $(FPGA_ROOT)/verif/sim/$(TARGET)/
INC_DIR = $(FPGA_ROOT)/software/include/
iNC_DIR = $(FPGA_ROOT)/software/include/elfio/
SRC_DIR = $(FPGA_ROOT)/software/runtime/
MKF_DIR = $(FPGA_ROOT)/verif/scripts/
V_DIR   = $(FPGA_ROOT)/design/#chisel3-verilog/
_CXX_SRC = run_elf.cpp run_elf_$(TARGET).cpp sim_dpi.cpp \
					 runtime.cpp $(TARGET)_runtime.cpp utility.cpp \
					 handler.cpp systemcall.cpp verilator_systemcall.cpp \
					 loguru.cpp
CXX_SRC = $(addprefix $(SRC_DIR), $(_CXX_SRC))
# V_SRC   = $(shell find $(V_DIR)/chisel3-verilog/ -name '*.v')
# V_SRC   += $(shell find $(V_DIR)/chisel3-verilog/ -name '*.sv')
V_SRC   =  $(V_DIR)/chisel3-verilog/BRAMVerilog.v $(V_DIR)/chisel3-verilog/CoreGroupAXIWithMemory.v $(V_DIR)/chisel3-verilog/ByteEnableBRAMVerilog.v $(V_DIR)/chisel3-verilog/coreparam.sv
V_SRC   += $(V_DIR)/tb.sv $(V_DIR)/ICacheMemory.sv $(V_DIR)/DCacheMemory.sv $(V_DIR)/MemoryDPI.sv $(V_DIR)/fifo_v2.v \
					 $(V_DIR)/cache_tag_array.v $(V_DIR)/cache_data_array.v $(V_DIR)/two_bit_rr_arbiter.v \
					 $(V_DIR)/ram_v2.v
CXXFLAGS = -std=c++17 -lpthread -I$(INC_DIR) -iquote $(iNC_DIR)

# allocated memory size in MB
# 384MB
MEM_SIZE = 260046848 # 134217728
# ELF root
ELF_ROOT ?= .
# ELF to be launched
ELF_PATH ?= $(ELF_ROOT)/rv64ui-p-add

include $(MKF_DIR)/Makefrag.isa.mk
include $(MKF_DIR)/Makefrag.splash2.mk
include $(MKF_DIR)/Makefrag.atomics.mk
ifeq ($(TARGET), verilator)
include $(MKF_DIR)/Makefile.$(TARGET)
endif
clean::
	rm -rf $(SIM_DIR)

all: compile

.PHONY: clean all
