We update the verification infrastructure to utilize virtual machines to simulate the host
The host would be a qemu virtual machine running ubuntu. 
We emulate the PCIe device, whose job is to send and receive signals from our testbench thru IPC (inter process communication).
So, it is basically a stand-alone executable.

The testbench will have infrasture that simulates whatever logic that is after the AXI Bridge (CDMA, cores etc), while the
virtual PCIe device will be responsible for communication between the tb.
