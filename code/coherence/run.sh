#!/bin/bash
for core in 4; do
  for protocol in pmsi ; do
    for bus in shared ; do
      for line in 32; do
        sbt "runMain utils.VU9PTop --core $core --line $line --protocol $protocol --bus $bus"
      done
    done
  done
done

