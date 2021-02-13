# The MapleBaord Repository
---------------------------

# Project structure

- `code/coherence`: the main chisel repository for coherence, where the DSL compiler is embeded as part of the code generation process
- `code/riscv-new`: the repository of the RISC-V core used in the project
- `code/chisel-testers2`: the repository of a modified chisel-tester2 dependencies to support strong enum, a later official version may be used
- `driver`: includes the Linux driver that we use to move data to and from the host to the FPGA and create interface for accessing the predictable cache on the FPGA. 
- `benchmarks`: includes the benchmarks and utilities that we use. Note that thread management and file IO management is in `benchmarks/riscv-splash2`, and the splash-2 benchmarks are in `benchmarks/parsec-3.0`.
- `runtime`: includes the runtime library for the FPGA board. You can find the runtime library at `runtime/fpga/software/runtime/` where `make compile` should create a binary file in `runtime/fpga/build/vu9p/Vtp`, which is used by `run.sh` to run programs. Note that an emulated design is included `runtime/fpga/software/emulation/`, and it can be used together with qemu.
- `qemu`: includes the patch required to emulate the PCI-e device in qemu as well as useful scripts to launch the VM.
- `synth`: includes scripts and designs used in Vivado to perform synthesis.

# System Requirements

The kernel version we use is 4.19.114 and the the OS version we use is Ubuntu 18.04.5.
The FPGA used is the VCU1525.
