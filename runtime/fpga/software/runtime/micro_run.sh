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

benchmarks="atomic_add_micro"
# benchmarks="non_atomic_add_micro"
echo The benchmarks to run: $benchmarks
for benchmark in $benchmarks; do
  for sf in $(seq 1 $(($CORES)) ); do
    for time in 1 2 3 4 5; do
      echo $time Round
      echo $benchmark
      echo $sf
       make $benchmark CORE=${CORES} SF=$sf INCR=40000000 >results/${CONFSTR}/$benchmark.$sf.out 2>>results/${CONFSTR}/$benchmark.$sf.err
       # make $benchmark CORE=${CORES} >results/${CONFSTR}/$benchmark.$sf.out 2>results/${CONFSTR}/$benchmark.$sf.err
    done
  done
done
