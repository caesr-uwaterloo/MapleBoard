TARGET=$1
if [[ $# -ne 1 ]] ; then
  echo Should input report or all
  exit
fi
for core in 2 4 8 ; do
  for protocol in carp-no-e ; do
    for bus in shared  ; do
      for line in 32 ; do
        if [[ $(jobs -r -p | wc -l) -gt 4 ]] ; then
          wait -n
        fi
        make -f Makefile.single $TARGET CORES=$core PROTO=$protocol BUS=$bus LINES=$line &
      done
    done
  done
done
wait
