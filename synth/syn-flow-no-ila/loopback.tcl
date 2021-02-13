set core [lindex $argv 0]
set depth [lindex $argv 1]
set protocol [lindex $argv 2]
set bus [lindex $argv 3]
set freq "100mhz"
set confstr "$protocol.${core}c.$depth.$bus"
set vroot "/home/allen/working/scala-deployment/coherence/$confstr/"
set output_root "/home/allen/mount/syn-new/"
set output_folder "$output_root/build.$confstr.$freq/"
exec rm -rf $output_folder
exec mkdir -p $output_folder
puts "VRoot: $vroot"
puts "Output Folder: $output_folder"

create_project -in_memory -part xcvu9p-fsgd2104-2L-e
set_property BOARD_PART xilinx.com:vcu1525:part0:1.3 [current_project]

set_param synth.maxThreads 8
set_param general.maxThreads 8


# read_xdc clock.xdc
read_xdc top.xdc
set_property PROCESSING_ORDER EARLY [ get_files top.xdc ]

exec python scripts/create_debug_xdc.py > ./debug.xdc

# read_xdc debug.xdc
# set_property PROCESSING_ORDER EARLY [ get_files debug.xdc ]


# The top file
read_verilog "$vroot/VU9PTop.v" ./shell.v $vroot/DualPortedRamBB.v
# read_verilog -sv { ./VU9PTop.MemoryControllerCoherenceTable.coherenceMemory.v ./VU9PTop.PrivateCoherenceTable.coherenceMemory.v }

# This should treat the shell as a black-box
# We also prevent the generation of some design

# read_checkpoint -incremental $output_folder/top_synth_ref.dcp

# synth_design -top VU9PTop -flatten_hierarchy none -part xcvu9p-fsgd2104-2L-e -directive RunTimeOptimized
synth_design -top VU9PTop -retiming -part xcvu9p-fsgd2104-2L-e

write_checkpoint -incremental_synth -force $output_folder/top_synth_ref.dcp

read_checkpoint -cell [get_cells sh] build/shell.dcp
set_property HD.PARTITION 1 [get_cells sh]
write_checkpoint -force $output_folder/top_synth.dcp

# exit

# open_checkpoint $output_folder/top_synth.dcp


opt_design -directive Explore

# disable this if it takes too long to route, maybe the design changes are too large to re-use
# read_checkpoint -incremental $output_folder/top_impl_ref.dcp

# place_design
# route_design

place_design -directive SSI_BalanceSLRs
phys_opt_design -slr_crossing_opt -tns_cleanup
route_design -tns_cleanup
phys_opt_design -directive AggressiveExplore

write_checkpoint -force $output_folder/top_impl.dcp
write_edif -force $output_folder/top_impl.edf
write_checkpoint -force $output_folder/top_impl_ref.dcp
write_bitstream -force $output_folder/top.bit
write_debug_probes -force $output_folder/top.ltx

# for incremental flow, if everything just works, use that as our 
# reference point

# Hold
puts [ get_property SLACK [get_timing_paths -hold] ]
# Setup
puts [ get_property SLACK [get_timing_paths -setup] ]
# Total 
puts [ get_property SLACK [get_timing_paths -delay_type min_max] ]
