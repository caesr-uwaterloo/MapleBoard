for core in 2 4 8 ; do
  for proto in msi mesi ; do
    make -f Makefile.single report CORES=$core PROTO=$proto BUS=shared LINES=32
  done
done

for core in 2 4 8 ; do
  for proto in pmsi pmesi ; do
    make -f Makefile.single report CORES=$core PROTO=$proto BUS=atomicModified LINES=32
  done
done
