# SIC implementation
Based on existing RISC-V core project

Progress: basic 5-stage pipeline completes

TODO:
1. More testing: currently the memory DPI module is implemented in Chisel, which use a register array to simulate the memory.
   Current approach cannot create a big enough memory to hold many test programs, otherwise stackoverflow happens.
   Need to consider alternate way to do this.
2. Change memory layer to support SIC implementation: add one bit in fetch request indicating
if we want to cancel the access if it is a cache miss, and the response indicates whether the request is cancelled or not.
