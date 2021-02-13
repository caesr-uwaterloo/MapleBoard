create_project -in_memory -part xcvu9p-fsgd2104-2L-e
set_property BOARD_PART xilinx.com:vcu1525:part0:1.3 [current_project]

# create block design
source shell.tcl

make_wrapper -files [get_files shell.bd] -import -inst_template
# this creates the shell_wrapper.v
# but, the bd wrapper is created as shell.v

read_xdc shell_clock.xdc
read_xdc ddr.xdc


# generate the target for the block design
# otherwise, the synth_design will report error
generate_target all [get_files shell.bd]

# we use this for reference
# read_checkpoint -incremental build/shell_ref.dcp

# this can take a while to generate...
synth_design -mode out_of_context -flatten_hierarchy none -top shell -part xcvu9p-fsgd2104-2L-e

set_property HD.PARTITION 1 [current_design]

# And then write the dcp
write_checkpoint -force build/shell.dcp
write_checkpoint -incremental_synth -force build/shell_ref.dcp

# opt_design
# place_design
# route_design
# 
# write_checkpoint -force build/shell_impl.dcp
