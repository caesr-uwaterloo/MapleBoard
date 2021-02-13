make -f Makefile.single report CORES=8 PROTO=msi BUS=shared LINES=32
make -f Makefile.single report CORES=4 PROTO=msi BUS=shared LINES=32
make -f Makefile.single report CORES=2 PROTO=msi BUS=shared LINES=32

make -f Makefile.single report CORES=8 PROTO=mesi BUS=shared LINES=32
make -f Makefile.single report CORES=4 PROTO=mesi BUS=shared LINES=32
make -f Makefile.single report CORES=2 PROTO=mesi BUS=shared LINES=32

make -f Makefile.single report CORES=8 PROTO=pmsi BUS=atomicModified LINES=32
make -f Makefile.single report CORES=4 PROTO=pmsi BUS=atomicModified LINES=32
make -f Makefile.single report CORES=2 PROTO=pmsi BUS=atomicModified LINES=32

make -f Makefile.single report CORES=8 PROTO=pmesi BUS=atomicModified LINES=32
make -f Makefile.single report CORES=4 PROTO=pmesi BUS=atomicModified LINES=32
make -f Makefile.single report CORES=2 PROTO=pmesi BUS=atomicModified LINES=32
