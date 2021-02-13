set core [lindex $argv 0]
set depth [lindex $argv 1]
set protocol [lindex $argv 2]
set bus [lindex $argv 3]
set freq "100mhz"
set confstr "$protocol.${core}c.$depth.$bus"
set output_root "/home/allen/mount/syn-new/"
set output_folder "$output_root/build.$confstr.$freq/"

puts "Bitstream Folder (Output Folder): $output_folder"
open_hw
connect_hw_server
open_hw_target

set_property PROGRAM.FILE      "$output_folder/top.bit"  [get_hw_devices xcvu9p_0]
set_property PROBES.FILE       "$output_folder/top.ltx"  [get_hw_devices xcvu9p_0]
set_property FULL_PROBES.FILE  "$output_folder/top.ltx"  [get_hw_devices xcvu9p_0]
current_hw_device  [get_hw_devices xcvu9p_0]
program_hw_devices [get_hw_devices xcvu9p_0]

refresh_hw_device [lindex [get_hw_devices xcvu9p_0] 0]
after 2000
report_property [ lindex [get_hw_migs] 0] -regexp .*CAL_ERROR_MSG.*

