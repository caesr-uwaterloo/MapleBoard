# The new RISC-V implementation

## Target Simulator
- verilator
- xsim (Vivado)

## Target Device
- ZCU102
### Boot command

ZCU102 needs special command to prevent the board from idling using JTAG(jtag)
```
 setenv bootargs 'console=ttyPS0,115200n8 earlycon clk_ignore_unused cpuidle.off=1 root=/dev/mmcblk0p2 rw rootwait'
 setenv bootargs 'console=ttyPS0,115200n8 earlycon clk_ignore_unused cpuidle.off=1 root=/dev/mmcblk0p2 rw earlyprintk rootfstype=ext4 rootwait devtmpfs.mount=1 uio_pdrv_genirq.of_id="generic-uio"'
```

### Programming command
```
open_hw
connect_hw_server
open_hw_target
current_hw_device [get_hw_devices xczu9_0]
refresh_hw_device -update_hw_probes false [lindex [get_hw_devices xczu9_0] 0]
current_hw_device [get_hw_devices arm_dap_1]
refresh_hw_device -update_hw_probes false [lindex [get_hw_devices arm_dap_1] 0]
current_hw_device [get_hw_devices xczu9_0]
set_property PROBES.FILE {} [get_hw_devices xczu9_0]
set_property FULL_PROBES.FILE {} [get_hw_devices xczu9_0]
set_property PROGRAM.FILE {/home/allen/coherence-runs-tcl/output/pmsi.6c.32/pmsi.6c.32_bd.bit} [get_hw_devices xczu9_0]
program_hw_devices [get_hw_devices xczu9_0]
refresh_hw_device [lindex [get_hw_devices xczu9_0] 0]
close_hw_target
close_hw
```

- PYNQ-Z1/2
- F1

## Verilator Guide
`export FPGA_ROOT=`
`export ELF_ROOT=`

## Dependencies
(Maybe useful afterwards for the license issue)
- loguru
- ELFIO

## Progress about the SPLASH-2 benchmarks

| Name           | Status (2-3 cores)*                                                 | Status (4+ cores)                         |
|:--------------:|:------------------------------------------------------------------:|:-----------------------------------------:|
| *Kernels**        |                                                                    |                                           |
|CHOLESKY        | Passed                                                             | Passed (6 cores)                          |
|RADIX           | Passed (2 cores)                                                   | Passed (4 cores)**                          |
|LU (contiguous) | Same as Host                                                       | Same as Host (6 cores)                    |
| FFT            | Same as Host (2 cores)                                             | Same as Host (4 cores)                    |
| **Apps**           |                                                                    |                                           |
| BARNES         | Same as Host                                                       | 6                                         |
| OCEAN          | Taking too long (more than 40 minutes) (2 cores) #                 |                                           |
| RAYTRACE       | Runs (Same output as host)                                         |                                           |
| VOLREND        | Runs                                                               |                                           |
| FMM            | Runs                                                               | Runs (4 cores, Host not working for more) |
| RADIOSITY      | **!!!has a problem running for more than two processors natively**     |                                           |
| WATER-NSQUARED | Same as Host for 64Mol                                             | Same as Host (6 cores)                    |
| WATER-SPATIAL  | Same as Host for 64Mol(20min)                                      | Same as Host (6 cores)                    |

1. * Some benchmarks only work if the number of processor is a power of two, these benchmarks are annotated with `2 cores`
2. \# The program takes too long to run, so not sure whether it works. (more than an hour)
3. ** The program requested a large amount of memory
