export FPGA_ROOT=~/Dropbox/Caesr/riscv-new/fpga/
export ELF_ROOT=~/Dropbox/Caesr/riscv/tests/isa/output/
export SPLASH2_ROOT=/home/allen/Dropbox/Caesr/riscv-splash2/splash2_benchmark

# sadly, 64 lines won't be possible for all 8 core configurations...probably solved with tandem configuration
LINES=$4
PROTO=$1
CORES=$2
BUS=$3
CONFSTR=${PROTO}.${CORES}c.${LINES}.${BUS}
echo $CONFSTR

if [ $# -ne '4' ]; then
  echo "There must be protocol, cores and bus go $# arguments"
  exit
fi

sudo -u allen mkdir -p ./results/${CONFSTR}

benchmarks="lu_cb ocean_cp radix raytrace water_spatial water_nsquared cholesky volrend fmm fft barnes"
for benchmark in $benchmarks; do
  echo $benchmark
  make $benchmark CORE=${CORES} >results/${CONFSTR}/$benchmark.out 2>results/${CONFSTR}/$benchmark.err
done
