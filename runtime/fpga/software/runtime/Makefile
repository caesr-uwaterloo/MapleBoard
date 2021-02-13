# TARGET:=zcu102
TARGET:=vu9p
include ../../verif/scripts/Makefile

BUILD_DIR=$(FPGA_ROOT)/build/$(TARGET)/
MC=$(shell echo $(TARGET) | tr  '[:lower:]' '[:upper:]')
CXXFLAGS:=$(CXXFLAGS) -D$(MC) -I$(V_DIR)/chisel3-verilog/ -I/usr/include/ -DXLEN=64 -g
_OBJS=${_CXX_SRC:.cpp=.o}
OBJS=$(addprefix $(BUILD_DIR), $(_OBJS))
RT=Vtb
# overwrite the SIM_DIR as we no longer needs simulation
SIM_DIR=$(BUILD_DIR)

ifeq ($(TARGET), zcu102)
LDFLAGS=-lcma -I/usr/include -lpthread -ldl -lsds_lib
endif

ifeq ($(TARGET), vu9p)
LDFLAGS=-I/usr/include -lpthread -ldl
endif

$(OBJS): $(BUILD_DIR)%.o: $(SRC_DIR)/%.cpp
	# compiles only
	@mkdir -p $(BUILD_DIR)
	g++ -c $(CXXFLAGS) $^ -o $@

# Simply use this name because this is the tb name used in verilator
$(BUILD_DIR)/Vtb: $(OBJS)
	g++ -o $@ $^ $(CXXFLAGS) $(LDFLAGS)

compile:  $(OBJS) $(BUILD_DIR)/Vtb
	echo $(OBJS)

# collect the benchmarks from parsec benchmark directory
PARSECS2DIR=/home/allen/parsec/parsec-3.0/ext/splash2/
SP2KERNELS=cholesky fft lu_cb lu_ncb radix
SP2KERNELS:=$(addprefix ./benchmarks/, $(SP2KERNELS))
SP2APPS=barnes ocean_cp radiosity volrend water_spatial fmm ocean_ncp raytrace water_nsquared
SP2APPS:=$(addprefix ./benchmarks/, $(SP2APPS))
$(SP2KERNELS):./benchmarks/%:
	cp $(PARSECS2DIR)/kernels/$*/inst/riscv-linux.gcc/bin/$* ./benchmarks/
	mkdir -p benchmarks/$*-input/
	cp $(PARSECS2DIR)/kernels/$*/inputs/* ./benchmarks/$*-input/ | true
$(SP2APPS):./benchmarks/%:
	cp $(PARSECS2DIR)/apps/$*/inst/riscv-linux.gcc/bin/$* ./benchmarks/
	# also copy the input
	mkdir -p benchmarks/$*-input/
	cp $(PARSECS2DIR)/apps/$*/inputs/* ./benchmarks/$*-input/ | true


collect: $(SP2KERNELS) $(SP2APPS)
clean-benchmarks:
	rm -rf benchmarks/*
	
clean::
	rm -rf $(OBJS) $(BUILD_DIR)/Vtb
