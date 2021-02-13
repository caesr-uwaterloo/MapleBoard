# A list of splash2 programs

ifeq ($(TARGET), verilator)
lu_cb: compile
	sudo env -i $(SIM_DIR)/$(RT) -m $(MEM_SIZE) $(FPGA_ROOT)/software/runtime/benchmarks/lu_cb -v 2 -a "-n128 -p8 -b8 -t -s"
# other benchmarks take a long time to run
LU-contiguous: compile
	$(SIM_DIR)/$(RT) -m $(MEM_SIZE) $(FPGA_ROOT)/software/runtime/benchmarks/lu_cb -v 2 -a "-n8 -p4 -b2"

RADIX: compile
	env -i $(SIM_DIR)/$(RT) -m $(MEM_SIZE) $(FPGA_ROOT)/software/runtime/benchmarks/radix -v 2 -a "-r256 -p4 -n256 -m256 -t -s"

FMM: compile
	env -i $(SIM_DIR)/$(RT) -m $(MEM_SIZE) $(FPGA_ROOT)/software/runtime/benchmarks/fmm -v 2 -a "-s"  <  $(FPGA_ROOT)/software/runtime/benchmarks/fmm-input/input.256

WATER_SPATIAL: compile
	cp $(FPGA_ROOT)/software/runtime/benchmarks/water_spatial-input/random.in $(FPGA_ROOT)/software/runtime/benchmarks/
	env -i $(SIM_DIR)/$(RT) -m $(MEM_SIZE) $(FPGA_ROOT)/software/runtime/benchmarks/water_spatial -v 2 -a " 4 " < $(FPGA_ROOT)/software/runtime/benchmarks/water_spatial-input/input
else
cholesky: compile
	sudo env -i $(SIM_DIR)/$(RT) -m $(MEM_SIZE) $(FPGA_ROOT)/software/runtime/benchmarks/cholesky -v 2 -a " -p${CORE} -t -C16 $(FPGA_ROOT)/software/runtime/benchmarks/cholesky-input/lshp.O"
lu_cb: compile
	sudo env -i $(SIM_DIR)/$(RT) -m $(MEM_SIZE) $(FPGA_ROOT)/software/runtime/benchmarks/lu_cb -v 2 -a "-n512 -p${CORE} -b16 -t -s"

radiosity: compile
	sudo env -i $(SIM_DIR)/$(RT) -m $(MEM_SIZE) $(FPGA_ROOT)/software/runtime/benchmarks/radiosity -v 2 -a "-batch -room -p ${CORE}"

fft: compile
	sudo env -i $(SIM_DIR)/$(RT) -m $(MEM_SIZE) $(FPGA_ROOT)/software/runtime/benchmarks/fft -v 2 -a "-m18 -p${CORE} -n64 -l6 -t -s"
radix: compile
	# Note RAIDX only works for 2^n proc
	sudo env -i $(SIM_DIR)/$(RT) -m $(MEM_SIZE) $(FPGA_ROOT)/software/runtime/benchmarks/radix -v 2 -a "-r4096 -p${CORE} -n262144 -m524288"

barnes: compile
	sed -i "12s/.*/${CORE}/" $(FPGA_ROOT)/software/runtime/benchmarks/barnes-input/input.large
	sudo env -i  $(SIM_DIR)/$(RT) -m $(MEM_SIZE) $(FPGA_ROOT)/software/runtime/benchmarks/barnes -v 2 < $(FPGA_ROOT)/software/runtime/benchmarks/barnes-input/input.large

ocean_cp: compile
	sudo env -i $(SIM_DIR)/$(RT) -m $(MEM_SIZE) $(FPGA_ROOT)/software/runtime/benchmarks/ocean_cp -v 2 -a "-n258 -p${CORE} -e1e-07 -r20000 -t28800 -o"

fmm: compile
	sed -i "5s/.*/${CORE}/" $(FPGA_ROOT)/software/runtime/benchmarks/fmm-input/input.256
	sudo env -i $(SIM_DIR)/$(RT) -m $(MEM_SIZE) $(FPGA_ROOT)/software/runtime/benchmarks/fmm -v 2 -a "${CORE}"  <  $(FPGA_ROOT)/software/runtime/benchmarks/fmm-input/input.256

raytrace: compile
	cp $(FPGA_ROOT)/software/runtime/benchmarks/raytrace-input/teapot.geo $(FPGA_ROOT)/software/runtime/benchmarks/
	cp $(FPGA_ROOT)/software/runtime/benchmarks/raytrace-input/teapot.env $(FPGA_ROOT)/software/runtime/benchmarks/
	sudo $(SIM_DIR)/$(RT) -m $(MEM_SIZE) $(FPGA_ROOT)/software/runtime/benchmarks/raytrace -v 2 -a "-p${CORE} -m128 $(FPGA_ROOT)/software/runtime/benchmarks/teapot.env"

volrend: compile
	sudo $(SIM_DIR)/$(RT) -m $(MEM_SIZE) $(FPGA_ROOT)/software/runtime/benchmarks/volrend -v 2 -a  "${CORE}  $(FPGA_ROOT)/software/runtime/benchmarks/volrend-input/head-scaleddown4"

water_nsquared: compile
	cp $(FPGA_ROOT)/software/runtime/benchmarks/water_nsquared-input/random.in $(FPGA_ROOT)/software/runtime/benchmarks/
	sudo env -i $(SIM_DIR)/$(RT) -m $(MEM_SIZE) $(FPGA_ROOT)/software/runtime/benchmarks/water_nsquared -v 2 -a " ${CORE} " < $(FPGA_ROOT)/software/runtime/benchmarks/water_nsquared-input/input.${CORE}

water_spatial: compile
	cp $(FPGA_ROOT)/software/runtime/benchmarks/water_spatial-input/random.in $(FPGA_ROOT)/software/runtime/benchmarks/
	sudo env -i $(SIM_DIR)/$(RT) -m $(MEM_SIZE) $(FPGA_ROOT)/software/runtime/benchmarks/water_spatial -v 2 -a " ${CORE} " < $(FPGA_ROOT)/software/runtime/benchmarks/water_spatial-input/input.${CORE}

atomic_add: compile
	sudo env -i $(SIM_DIR)/$(RT) -m $(MEM_SIZE) $(FPGA_ROOT)/../../riscv-splash2/atomics_benchmark/atomic_add -v 2 -a " 0 "
atomic_add_hard: compile
	sudo env -i $(SIM_DIR)/$(RT) -m $(MEM_SIZE) $(FPGA_ROOT)/../../riscv-splash2/atomics_benchmark/atomic_add_hard -v 2 -a " 0 "
INCR ?= 2000000
atomic_add_carp: compile
	if [ -z ${CORE} ]; then echo "Must provide number of COREs!"; exit 1; fi
	sudo env -i $(SIM_DIR)/$(RT) -n  $$((${CORE} - 1)) -m $(MEM_SIZE) $(FPGA_ROOT)/../../riscv-splash2/atomics_benchmark/atomic_add_carp -v 2 -a " $$((${CORE} - 1)) ${SF} ${INCR}"

atomic_add_micro: compile
	if [ -z ${CORE} ]; then echo "Must provide number of COREs!"; exit 1; fi
	if [ -z ${SF} ]; then echo "Must provide share factor!"; exit 1; fi
	sudo env -i $(SIM_DIR)/$(RT) -n  $$((${CORE} - 1)) -m $(MEM_SIZE) $(FPGA_ROOT)/../../riscv-splash2/atomics_benchmark/atomic_add_micro -v 2 -a " $$((${CORE} - 1)) ${SF} ${INCR}"
# non_atomic_add_micro: compile
# 	if [ -z ${CORE} ]; then echo "Must provide number of COREs!"; exit 1; fi
# 	if [ -z ${SF} ]; then echo "Must provide share factor!"; exit 1; fi
# 	sudo env -i $(SIM_DIR)/$(RT) -n  $$((${CORE} - 1)) -m $(MEM_SIZE) $(FPGA_ROOT)/../../riscv-splash2/atomics_benchmark/non_atomic_add_micro -v 2 -a " $$((${CORE} - 1)) ${SF} ${INCR}"
#

# Suppose we don't do monitoring
non_atomic_add_micro: compile
	if [ -z ${CORE} ]; then echo "Must provide number of COREs!"; exit 1; fi
	if [ -z ${SF} ]; then echo "Must provide share factor!"; exit 1; fi
	sudo env -i $(SIM_DIR)/$(RT) -m $(MEM_SIZE) $(FPGA_ROOT)/../../riscv-splash2/atomics_benchmark/non_atomic_add_micro -v 2 -a " $$((${CORE})) ${SF} ${INCR}"



endif

