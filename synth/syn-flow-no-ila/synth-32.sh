	for core in 8 ; do \
		for protocol in pmsi pmesi ; do \
		for bus in shared dedicated; do \
      echo make -f Makefile.single all CORES=$core PROTO=$protocol BUS=$bus LINES=32 ; \
		done done done
