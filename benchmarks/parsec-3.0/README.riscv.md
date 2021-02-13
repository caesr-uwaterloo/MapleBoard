# Modifications
- Add the ostype in the gcc.bldconf
- Add necessary CLFAGS/LDFLAGS for the runtime to simulate kernel and communicate with the host
- Add modify the `CFLAGS` and `LDFLAGS` in `makefile`s so that they can take the arguments from the gcc.bldcon
- `gets()`: on 64-bit system, should not use the return value directly, might cause segmentation fault because of the pointer...


# Usage
An example:
`HOSTTYPE=riscv OSTYPE=rv64linux parsecmgmt -a build -p splash2.fft`
