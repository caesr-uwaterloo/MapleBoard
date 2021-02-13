#!/bin/bash
cat ~/harddrive/tmp/log.syscall.return | grep "\[F$1\]\|\[W$1\]\|dCache\|\[CC *$((2*$1+1))\]\|\[CC *$((2*$1))\]\|\[CC *$((2*$1))\]\|\[LLC\|\[BUS\|systemcall"  | grep "\[Core $1\]\|\[M$1\]\|ICacheResp\|\[W$1\]\|dCache\|\[CC *$((2*$1+1))\]\|\[CC *$((2*$1))\]\|\[CC *$((2*$1))\]|\[LLC\|\[BUS\|systemcall" > /tmp/log.$1
