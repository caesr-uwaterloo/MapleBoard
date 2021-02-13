shell.dcp: shell.tcl
	vivado -mode batch -source synthesize-shell.tcl

shell.v::
	rm -rf build/tmp/*
	cp build/shell.dcp build/tmp/
	cd build/tmp && unzip shell.dcp && cp shell_stub.v ../../shell.v


shell_wrapper.v:
	echo NOTE: if the file is not found, try generate shell.dcp first, after that the shell_wrapper.v will be generated
	cp .srcs/sources_1/bd/shell/hdl/shell_wrapper.v .

configs= pmsi.2c.4way.32.dedicated.70mhz \
				 pmsi.4c.4way.32.dedicated.70mhz \
				 pmsi.8c.4way.32.dedicated.70mhz \
				 pmsi.2c.4way.32.shared.70mhz \
				 pmsi.4c.4way.32.shared.70mhz \
				 pmsi.8c.4way.32.shared.70mhz

utilization-targets=$(addsuffix .rpt,$(addprefix build/, $(configs)))

build/%.rpt:
	cd build.$* && vivado -mode tcl -source ../report_utilization.tcl

report: $(utilization-targets)

cores = 2 4 8
protocols = pmsi pmesi
buses = shared dedicated

synthesize-all:
	for core in $(cores) ; do \
		for protocol in $(protocols); do \
		for bus in $(buses); do \
		make -f Makefile.single all CORES=$$core PROTO=$$protocol BUS=$$bus;  \
		done done done

report-all:
	for core in $(cores) ; do \
		for protocol in $(protocols); do \
		for bus in $(buses); do \
		make -f Makefile.single report CORES=$$core PROTO=$$protocol BUS=$$bus;  \
		done done done
