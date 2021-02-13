	for core in 8 ; do \
		for protocol in pmsi pmesi ; do \
		for bus in shared dedicated; do \
      make -f Makefile.single all CORES=$$core PROTO=$$protocol BUS=$$bus LINES=64 \
		done done done
