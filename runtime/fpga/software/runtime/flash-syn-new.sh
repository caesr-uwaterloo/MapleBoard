#!/bin/bash
LINES=$4
PROTO=$1
CORES=$2
BUS=$3
vivado -mode batch -source flash-syn-new.tcl -tclargs $CORES $LINES $PROTO $BUS -nojournal -nolog
