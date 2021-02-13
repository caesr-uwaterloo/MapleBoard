
################################################################
# This is a generated script based on design: shell
#
# Though there are limitations about the generated script,
# the main purpose of this utility is to make learning
# IP Integrator Tcl commands easier.
################################################################

namespace eval _tcl {
proc get_script_folder {} {
   set script_path [file normalize [info script]]
   set script_folder [file dirname $script_path]
   return $script_folder
}
}
variable script_folder
set script_folder [_tcl::get_script_folder]

################################################################
# Check if script is running in correct Vivado version.
################################################################
set scripts_vivado_version 2018.3
set current_vivado_version [version -short]

if { [string first $scripts_vivado_version $current_vivado_version] == -1 } {
   puts ""
   catch {common::send_msg_id "BD_TCL-109" "ERROR" "This script was generated using Vivado <$scripts_vivado_version> and is being run in <$current_vivado_version> of Vivado. Please run the script in Vivado <$scripts_vivado_version> then open the design in Vivado <$current_vivado_version>. Upgrade the design by running \"Tools => Report => Report IP Status...\", then run write_bd_tcl to create an updated script."}

   return 1
}

################################################################
# START
################################################################

# To test this script, run the following commands from Vivado Tcl console:
# source shell_script.tcl

# If there is no project opened, this script will create a
# project, but make sure you do not have an existing project
# <./myproj/project_1.xpr> in the current working folder.

set list_projs [get_projects -quiet]
if { $list_projs eq "" } {
   create_project project_1 myproj -part xcvu9p-fsgd2104-2L-e
   set_property BOARD_PART xilinx.com:vcu1525:part0:1.3 [current_project]
}


# CHANGE DESIGN NAME HERE
variable design_name
set design_name shell

# If you do not already have an existing IP Integrator design open,
# you can create a design using the following command:
#    create_bd_design $design_name

# Creating design if needed
set errMsg ""
set nRet 0

set cur_design [current_bd_design -quiet]
set list_cells [get_bd_cells -quiet]

if { ${design_name} eq "" } {
   # USE CASES:
   #    1) Design_name not set

   set errMsg "Please set the variable <design_name> to a non-empty value."
   set nRet 1

} elseif { ${cur_design} ne "" && ${list_cells} eq "" } {
   # USE CASES:
   #    2): Current design opened AND is empty AND names same.
   #    3): Current design opened AND is empty AND names diff; design_name NOT in project.
   #    4): Current design opened AND is empty AND names diff; design_name exists in project.

   if { $cur_design ne $design_name } {
      common::send_msg_id "BD_TCL-001" "INFO" "Changing value of <design_name> from <$design_name> to <$cur_design> since current design is empty."
      set design_name [get_property NAME $cur_design]
   }
   common::send_msg_id "BD_TCL-002" "INFO" "Constructing design in IPI design <$cur_design>..."

} elseif { ${cur_design} ne "" && $list_cells ne "" && $cur_design eq $design_name } {
   # USE CASES:
   #    5) Current design opened AND has components AND same names.

   set errMsg "Design <$design_name> already exists in your project, please set the variable <design_name> to another value."
   set nRet 1
} elseif { [get_files -quiet ${design_name}.bd] ne "" } {
   # USE CASES: 
   #    6) Current opened design, has components, but diff names, design_name exists in project.
   #    7) No opened design, design_name exists in project.

   set errMsg "Design <$design_name> already exists in your project, please set the variable <design_name> to another value."
   set nRet 2

} else {
   # USE CASES:
   #    8) No opened design, design_name not in project.
   #    9) Current opened design, has components, but diff names, design_name not in project.

   common::send_msg_id "BD_TCL-003" "INFO" "Currently there is no design <$design_name> in project, so creating one..."

   create_bd_design $design_name

   common::send_msg_id "BD_TCL-004" "INFO" "Making design <$design_name> as current_bd_design."
   current_bd_design $design_name

}

common::send_msg_id "BD_TCL-005" "INFO" "Currently the variable <design_name> is equal to \"$design_name\"."

if { $nRet != 0 } {
   catch {common::send_msg_id "BD_TCL-114" "ERROR" $errMsg}
   return $nRet
}

set bCheckIPsPassed 1
##################################################################
# CHECK IPs
##################################################################
set bCheckIPs 1
if { $bCheckIPs == 1 } {
   set list_check_ips "\ 
xilinx.com:ip:axi_cdma:4.1\
xilinx.com:ip:clk_wiz:6.0\
xilinx.com:ip:ddr4:2.2\
xilinx.com:ip:proc_sys_reset:5.0\
xilinx.com:ip:util_ds_buf:2.1\
xilinx.com:ip:util_vector_logic:2.0\
xilinx.com:ip:smartconnect:1.0\
xilinx.com:ip:xdma:4.1\
"

   set list_ips_missing ""
   common::send_msg_id "BD_TCL-006" "INFO" "Checking if the following IPs exist in the project's IP catalog: $list_check_ips ."

   foreach ip_vlnv $list_check_ips {
      set ip_obj [get_ipdefs -all $ip_vlnv]
      if { $ip_obj eq "" } {
         lappend list_ips_missing $ip_vlnv
      }
   }

   if { $list_ips_missing ne "" } {
      catch {common::send_msg_id "BD_TCL-115" "ERROR" "The following IPs are not found in the IP Catalog:\n  $list_ips_missing\n\nResolution: Please add the repository containing the IP(s) to the project." }
      set bCheckIPsPassed 0
   }

}

if { $bCheckIPsPassed != 1 } {
  common::send_msg_id "BD_TCL-1003" "WARNING" "Will not continue with creation of design due to the error(s) above."
  return 3
}


##################################################################
# DATA FILE TCL PROCs
##################################################################

proc write_ddr4_file_shell_ddr4_0_1 { str_filepath } {

   file mkdir [ file dirname "$str_filepath" ]
   set data_file [open $str_filepath  w+]

   puts $data_file {Part type,Part name,Rank,StackHeight,CA Mirror,Data mask,Address width,Row width,Column width,Bank width,Bank group width,CS width,CKE width,ODT width,CK width,Memory speed grade,Memory density,Component density,Memory device width,Memory component width,Data bits per strobe,IO Voltages,Data widths,Min period,Max period,tCKE,tFAW,tFAW_dlr,tMRD,tRAS,tRCD,tREFI,tRFC,tRFC_dlr,tRP,tRRD_S,tRRD_L,tRRD_dlr,tRTP,tWR,tWTR_S,tWTR_L,tXPR,tZQCS,tZQINIT,tCCD_3ds,cas latency,cas write latency,burst length}
   puts $data_file {UDIMMs,BLS4G4D240FSB-2133,1,1,0,1,17,15,10,2,2,1,1,1,1,2133,4GB,4Gb,64,8,8,1.2V,64,938,1600,5000 ps,15000 ps,0,8 tck,33000 ps,14060 ps,7800000 ps,260000 ps,0,14160 ps,3300 ps,4900 ps,0,7500 ps,15000 ps,2500 ps,7500 ps,360 ns,128 tck,1024 tck,0,14,11,8}
   puts $data_file {UDIMMs,BLS4G4D240FSB-2400,1,1,0,1,17,15,10,2,2,1,1,1,1,2400,4GB,4Gb,64,8,8,1.2V,64,833,1600,5000 ps,13000 ps,0,8 tck,32000 ps,14160 ps,7800000 ps,350000 ps,0,14160 ps,3300 ps,4900 ps,0,7500 ps,15000 ps,2500 ps,7500 ps,360 ns,128 tck,1024 tck,0,16,12,8}
   puts $data_file {UDIMMs,BLS4G4D240FSB-2666,1,1,0,1,17,15,10,2,2,1,1,1,1,2666,4GB,4Gb,64,8,8,1.2V,64,750,1600,5000 ps,12000 ps,0,8 tck,32000 ps,12500 ps,7800000 ps,260000 ps,0,12500 ps,3000 ps,4900 ps,0,7500 ps,15000 ps,2500 ps,7500 ps,360 ns,128 tck,1024 tck,0,18,14,8}

   close $data_file
}
# End of write_ddr4_file_shell_ddr4_0_1()



##################################################################
# DESIGN PROCs
##################################################################


# Hierarchical cell: xdma
proc create_hier_cell_xdma { parentCell nameHier } {

  variable script_folder

  if { $parentCell eq "" || $nameHier eq "" } {
     catch {common::send_msg_id "BD_TCL-102" "ERROR" "create_hier_cell_xdma() - Empty argument(s)!"}
     return
  }

  # Get object for parentCell
  set parentObj [get_bd_cells $parentCell]
  if { $parentObj == "" } {
     catch {common::send_msg_id "BD_TCL-100" "ERROR" "Unable to find parent cell <$parentCell>!"}
     return
  }

  # Make sure parentObj is hier blk
  set parentType [get_property TYPE $parentObj]
  if { $parentType ne "hier" } {
     catch {common::send_msg_id "BD_TCL-101" "ERROR" "Parent <$parentObj> has TYPE = <$parentType>. Expected to be <hier>."}
     return
  }

  # Save current instance; Restore later
  set oldCurInst [current_bd_instance .]

  # Set parent object as current
  current_bd_instance $parentObj

  # Create cell and set as current instance
  set hier_obj [create_bd_cell -type hier $nameHier]
  current_bd_instance $hier_obj

  # Create interface pins
  create_bd_intf_pin -mode Master -vlnv xilinx.com:interface:aximm_rtl:1.0 M_AXI_B
  create_bd_intf_pin -mode Slave -vlnv xilinx.com:interface:aximm_rtl:1.0 S_AXI_B1
  create_bd_intf_pin -mode Slave -vlnv xilinx.com:interface:aximm_rtl:1.0 S_AXI_LITE
  create_bd_intf_pin -mode Master -vlnv xilinx.com:interface:pcie_7x_mgt_rtl:1.0 pci_express_x16

  # Create pins
  create_bd_pin -dir O -type clk axi_aclk
  create_bd_pin -dir O -type rst axi_aresetn
  create_bd_pin -dir O -type rst axi_ctl_aresetn
  create_bd_pin -dir I -type rst pcie_perstn
  create_bd_pin -dir I -type clk sys_clk
  create_bd_pin -dir I -type clk sys_clk_gt

  # Create instance: xdma_0, and set properties
  set xdma_0 [ create_bd_cell -type ip -vlnv xilinx.com:ip:xdma:4.1 xdma_0 ]
  set_property -dict [ list \
   CONFIG.PCIE_BOARD_INTERFACE {pci_express_x1} \
   CONFIG.PF0_DEVICE_ID_mqdma {9031} \
   CONFIG.PF2_DEVICE_ID_mqdma {9031} \
   CONFIG.PF3_DEVICE_ID_mqdma {9031} \
   CONFIG.SYS_RST_N_BOARD_INTERFACE {pcie_perstn} \
   CONFIG.axi_addr_width {64} \
   CONFIG.axi_data_width {64_bit} \
   CONFIG.axibar2pciebar_0 {0x0000000060000000} \
   CONFIG.axibar_num {1} \
   CONFIG.axisten_freq {125} \
   CONFIG.bar_indicator {BAR_1:0} \
   CONFIG.c_m_axi_num_write {8} \
   CONFIG.c_s_axi_supports_narrow_burst {false} \
   CONFIG.coreclk_freq {250} \
   CONFIG.en_axi_slave_if {true} \
   CONFIG.en_gt_selection {true} \
   CONFIG.enable_jtag_dbg {false} \
   CONFIG.enable_mark_debug {false} \
   CONFIG.enable_pcie_debug {False} \
   CONFIG.functional_mode {AXI_Bridge} \
   CONFIG.mcap_enablement {None} \
   CONFIG.pciebar2axibar_0 {0x1000000000000000} \
   CONFIG.pciebar2axibar_2 {0x0000000100000000} \
   CONFIG.pciebar2axibar_4 {0x0000000110000000} \
   CONFIG.pf0_bar0_64bit {true} \
   CONFIG.pf0_bar0_scale {Megabytes} \
   CONFIG.pf0_bar0_size {16} \
   CONFIG.pf0_bar2_64bit {true} \
   CONFIG.pf0_bar2_enabled {true} \
   CONFIG.pf0_bar2_scale {Megabytes} \
   CONFIG.pf0_bar2_size {256} \
   CONFIG.pf0_bar4_64bit {true} \
   CONFIG.pf0_bar4_enabled {true} \
   CONFIG.pf0_bar4_scale {Megabytes} \
   CONFIG.pf0_bar4_size {256} \
   CONFIG.pf0_base_class_menu {Processing_accelerators} \
   CONFIG.pf0_class_code {120000} \
   CONFIG.pf0_class_code_base {12} \
   CONFIG.pf0_class_code_interface {00} \
   CONFIG.pf0_class_code_sub {00} \
   CONFIG.pf0_device_id {9031} \
   CONFIG.pf0_msix_cap_pba_bir {BAR_1:0} \
   CONFIG.pf0_msix_cap_table_bir {BAR_1:0} \
   CONFIG.pf0_sub_class_interface_menu {Unknown} \
   CONFIG.pl_link_cap_max_link_speed {8.0_GT/s} \
   CONFIG.pl_link_cap_max_link_width {X1} \
   CONFIG.plltype {QPLL1} \
   CONFIG.xdma_axilite_slave {true} \
 ] $xdma_0

  # Create interface connections
  connect_bd_intf_net -intf_net Conn1 [get_bd_intf_pins pci_express_x16] [get_bd_intf_pins xdma_0/pcie_mgt]
  connect_bd_intf_net -intf_net S_AXI_B1_1 [get_bd_intf_pins S_AXI_B1] [get_bd_intf_pins xdma_0/S_AXI_B]
  connect_bd_intf_net -intf_net S_AXI_LITE_1 [get_bd_intf_pins S_AXI_LITE] [get_bd_intf_pins xdma_0/S_AXI_LITE]
  connect_bd_intf_net -intf_net xdma_0_M_AXI_B [get_bd_intf_pins M_AXI_B] [get_bd_intf_pins xdma_0/M_AXI_B]

  # Create port connections
  connect_bd_net -net pcie_perstn_1 [get_bd_pins pcie_perstn] [get_bd_pins xdma_0/sys_rst_n]
  connect_bd_net -net sys_clk_1 [get_bd_pins sys_clk] [get_bd_pins xdma_0/sys_clk]
  connect_bd_net -net sys_clk_gt_1 [get_bd_pins sys_clk_gt] [get_bd_pins xdma_0/sys_clk_gt]
  connect_bd_net -net xdma_0_axi_aclk [get_bd_pins axi_aclk] [get_bd_pins xdma_0/axi_aclk]
  connect_bd_net -net xdma_0_axi_aresetn [get_bd_pins axi_aresetn] [get_bd_pins xdma_0/axi_aresetn]
  connect_bd_net -net xdma_0_axi_ctl_aresetn [get_bd_pins axi_ctl_aresetn] [get_bd_pins xdma_0/axi_ctl_aresetn]

  # Restore current instance
  current_bd_instance $oldCurInst
}

# Hierarchical cell: interconnect
proc create_hier_cell_interconnect { parentCell nameHier } {

  variable script_folder

  if { $parentCell eq "" || $nameHier eq "" } {
     catch {common::send_msg_id "BD_TCL-102" "ERROR" "create_hier_cell_interconnect() - Empty argument(s)!"}
     return
  }

  # Get object for parentCell
  set parentObj [get_bd_cells $parentCell]
  if { $parentObj == "" } {
     catch {common::send_msg_id "BD_TCL-100" "ERROR" "Unable to find parent cell <$parentCell>!"}
     return
  }

  # Make sure parentObj is hier blk
  set parentType [get_property TYPE $parentObj]
  if { $parentType ne "hier" } {
     catch {common::send_msg_id "BD_TCL-101" "ERROR" "Parent <$parentObj> has TYPE = <$parentType>. Expected to be <hier>."}
     return
  }

  # Save current instance; Restore later
  set oldCurInst [current_bd_instance .]

  # Set parent object as current
  current_bd_instance $parentObj

  # Create cell and set as current instance
  set hier_obj [create_bd_cell -type hier $nameHier]
  current_bd_instance $hier_obj

  # Create interface pins
  create_bd_intf_pin -mode Master -vlnv xilinx.com:interface:aximm_rtl:1.0 CONF
  create_bd_intf_pin -mode Master -vlnv xilinx.com:interface:aximm_rtl:1.0 DDR
  create_bd_intf_pin -mode Master -vlnv xilinx.com:interface:aximm_rtl:1.0 M01_AXI
  create_bd_intf_pin -mode Master -vlnv xilinx.com:interface:aximm_rtl:1.0 M04_AXI
  create_bd_intf_pin -mode Master -vlnv xilinx.com:interface:aximm_rtl:1.0 M05_AXI
  create_bd_intf_pin -mode Slave -vlnv xilinx.com:interface:aximm_rtl:1.0 S03_AXI
  create_bd_intf_pin -mode Master -vlnv xilinx.com:interface:aximm_rtl:1.0 SLOT
  create_bd_intf_pin -mode Slave -vlnv xilinx.com:interface:aximm_rtl:1.0 SMEM
  create_bd_intf_pin -mode Slave -vlnv xilinx.com:interface:aximm_rtl:1.0 S_AXI

  # Create pins
  create_bd_pin -dir I -type clk M00_ACLK
  create_bd_pin -dir I -type rst M00_ARESETN
  create_bd_pin -dir I -type clk aclk2
  create_bd_pin -dir I -type clk root_clk

  # Create instance: smartconnect_0, and set properties
  set smartconnect_0 [ create_bd_cell -type ip -vlnv xilinx.com:ip:smartconnect:1.0 smartconnect_0 ]
  set_property -dict [ list \
   CONFIG.ADVANCED_PROPERTIES { __view__ { timing { S00_Buffer { AR_SLR_PIPE 1 AW_SLR_PIPE 1 B_SLR_PIPE 1 R_SLR_PIPE 1 W_SLR_PIPE 1 } M01_Buffer { AR_SLR_PIPE 1 AW_SLR_PIPE 1 B_SLR_PIPE 1 R_SLR_PIPE 1 W_SLR_PIPE 1 } S01_Buffer { AR_SLR_PIPE 1 AW_SLR_PIPE 1 B_SLR_PIPE 1 R_SLR_PIPE 1 W_SLR_PIPE 1 } S02_Buffer { AR_SLR_PIPE 1 AW_SLR_PIPE 1 B_SLR_PIPE 1 R_SLR_PIPE 1 W_SLR_PIPE 1 } M00_Buffer { AR_SLR_PIPE 1 AW_SLR_PIPE 1 B_SLR_PIPE 1 R_SLR_PIPE 1 W_SLR_PIPE 1 } M02_Buffer { AR_SLR_PIPE 1 AW_SLR_PIPE 1 B_SLR_PIPE 1 R_SLR_PIPE 1 W_SLR_PIPE 1 } M03_Buffer { AR_SLR_PIPE 1 AW_SLR_PIPE 1 B_SLR_PIPE 1 R_SLR_PIPE 1 W_SLR_PIPE 1 } M04_Buffer { AR_SLR_PIPE 1 AW_SLR_PIPE 1 B_SLR_PIPE 1 R_SLR_PIPE 1 W_SLR_PIPE 1 } M05_Buffer { AR_SLR_PIPE 1 AW_SLR_PIPE 1 B_SLR_PIPE 1 R_SLR_PIPE 1 W_SLR_PIPE 1 } } }} \
   CONFIG.NUM_CLKS {3} \
   CONFIG.NUM_MI {6} \
   CONFIG.NUM_SI {3} \
 ] $smartconnect_0

  # Create interface connections
  connect_bd_intf_net -intf_net Conn1 [get_bd_intf_pins DDR] [get_bd_intf_pins smartconnect_0/M03_AXI]
  connect_bd_intf_net -intf_net Conn2 [get_bd_intf_pins SMEM] [get_bd_intf_pins smartconnect_0/S01_AXI]
  connect_bd_intf_net -intf_net Conn4 [get_bd_intf_pins M04_AXI] [get_bd_intf_pins smartconnect_0/M04_AXI]
  connect_bd_intf_net -intf_net S03_AXI_1 [get_bd_intf_pins S03_AXI] [get_bd_intf_pins smartconnect_0/S02_AXI]
  connect_bd_intf_net -intf_net S_AXI_1 [get_bd_intf_pins S_AXI] [get_bd_intf_pins smartconnect_0/S00_AXI]
  connect_bd_intf_net -intf_net smartconnect_0_M00_AXI [get_bd_intf_pins M01_AXI] [get_bd_intf_pins smartconnect_0/M00_AXI]
  connect_bd_intf_net -intf_net smartconnect_0_M01_AXI [get_bd_intf_pins CONF] [get_bd_intf_pins smartconnect_0/M01_AXI]
  connect_bd_intf_net -intf_net smartconnect_0_M02_AXI [get_bd_intf_pins SLOT] [get_bd_intf_pins smartconnect_0/M02_AXI]
  connect_bd_intf_net -intf_net smartconnect_0_M05_AXI [get_bd_intf_pins M05_AXI] [get_bd_intf_pins smartconnect_0/M05_AXI]

  # Create port connections
  connect_bd_net -net M00_ACLK_1 [get_bd_pins M00_ACLK] [get_bd_pins smartconnect_0/aclk1]
  connect_bd_net -net M00_ARESETN_1 [get_bd_pins M00_ARESETN] [get_bd_pins smartconnect_0/aresetn]
  connect_bd_net -net aclk2_1 [get_bd_pins aclk2] [get_bd_pins smartconnect_0/aclk2]
  connect_bd_net -net root_clk_1 [get_bd_pins root_clk] [get_bd_pins smartconnect_0/aclk]

  # Restore current instance
  current_bd_instance $oldCurInst
}


# Procedure to create entire design; Provide argument to make
# procedure reusable. If parentCell is "", will use root.
proc create_root_design { parentCell } {

  variable script_folder
  variable design_name

  if { $parentCell eq "" } {
     set parentCell [get_bd_cells /]
  }

  # Get object for parentCell
  set parentObj [get_bd_cells $parentCell]
  if { $parentObj == "" } {
     catch {common::send_msg_id "BD_TCL-100" "ERROR" "Unable to find parent cell <$parentCell>!"}
     return
  }

  # Make sure parentObj is hier blk
  set parentType [get_property TYPE $parentObj]
  if { $parentType ne "hier" } {
     catch {common::send_msg_id "BD_TCL-101" "ERROR" "Parent <$parentObj> has TYPE = <$parentType>. Expected to be <hier>."}
     return
  }

  # Save current instance; Restore later
  set oldCurInst [current_bd_instance .]

  # Set parent object as current
  current_bd_instance $parentObj


  # Create interface ports
  set C1_DDR4_0 [ create_bd_intf_port -mode Master -vlnv xilinx.com:interface:ddr4_rtl:1.0 C1_DDR4_0 ]
  set CONF [ create_bd_intf_port -mode Master -vlnv xilinx.com:interface:aximm_rtl:1.0 CONF ]
  set_property -dict [ list \
   CONFIG.ADDR_WIDTH {64} \
   CONFIG.DATA_WIDTH {64} \
   CONFIG.HAS_BURST {1} \
   CONFIG.HAS_CACHE {1} \
   CONFIG.HAS_LOCK {1} \
   CONFIG.HAS_PROT {1} \
   CONFIG.HAS_QOS {0} \
   CONFIG.HAS_REGION {0} \
   CONFIG.HAS_WSTRB {1} \
   CONFIG.NUM_READ_OUTSTANDING {1} \
   CONFIG.NUM_WRITE_OUTSTANDING {1} \
   CONFIG.PROTOCOL {AXI4} \
   ] $CONF
  set SLOT [ create_bd_intf_port -mode Master -vlnv xilinx.com:interface:aximm_rtl:1.0 SLOT ]
  set_property -dict [ list \
   CONFIG.ADDR_WIDTH {64} \
   CONFIG.DATA_WIDTH {64} \
   CONFIG.HAS_BURST {1} \
   CONFIG.HAS_CACHE {1} \
   CONFIG.HAS_LOCK {1} \
   CONFIG.HAS_PROT {1} \
   CONFIG.HAS_QOS {0} \
   CONFIG.HAS_REGION {0} \
   CONFIG.HAS_WSTRB {0} \
   CONFIG.NUM_READ_OUTSTANDING {1} \
   CONFIG.NUM_WRITE_OUTSTANDING {1} \
   CONFIG.PROTOCOL {AXI4LITE} \
   ] $SLOT
  set SMEM [ create_bd_intf_port -mode Slave -vlnv xilinx.com:interface:aximm_rtl:1.0 SMEM ]
  set_property -dict [ list \
   CONFIG.ADDR_WIDTH {64} \
   CONFIG.ARUSER_WIDTH {0} \
   CONFIG.AWUSER_WIDTH {0} \
   CONFIG.BUSER_WIDTH {0} \
   CONFIG.DATA_WIDTH {64} \
   CONFIG.HAS_BRESP {1} \
   CONFIG.HAS_BURST {1} \
   CONFIG.HAS_CACHE {1} \
   CONFIG.HAS_LOCK {1} \
   CONFIG.HAS_PROT {1} \
   CONFIG.HAS_QOS {0} \
   CONFIG.HAS_REGION {0} \
   CONFIG.HAS_RRESP {1} \
   CONFIG.HAS_WSTRB {1} \
   CONFIG.ID_WIDTH {1} \
   CONFIG.MAX_BURST_LENGTH {256} \
   CONFIG.NUM_READ_OUTSTANDING {1} \
   CONFIG.NUM_READ_THREADS {1} \
   CONFIG.NUM_WRITE_OUTSTANDING {1} \
   CONFIG.NUM_WRITE_THREADS {1} \
   CONFIG.PROTOCOL {AXI4} \
   CONFIG.READ_WRITE_MODE {READ_WRITE} \
   CONFIG.RUSER_BITS_PER_BYTE {0} \
   CONFIG.RUSER_WIDTH {0} \
   CONFIG.SUPPORTS_NARROW_BURST {0} \
   CONFIG.WUSER_BITS_PER_BYTE {0} \
   CONFIG.WUSER_WIDTH {0} \
   ] $SMEM
  set dimm1_refclk [ create_bd_intf_port -mode Slave -vlnv xilinx.com:interface:diff_clock_rtl:1.0 dimm1_refclk ]
  set_property -dict [ list \
   CONFIG.FREQ_HZ {300000000} \
   ] $dimm1_refclk
  set pci_express_x16 [ create_bd_intf_port -mode Master -vlnv xilinx.com:interface:pcie_7x_mgt_rtl:1.0 pci_express_x16 ]
  set pcie_refclk [ create_bd_intf_port -mode Slave -vlnv xilinx.com:interface:diff_clock_rtl:1.0 pcie_refclk ]
  set_property -dict [ list \
   CONFIG.FREQ_HZ {100000000} \
   ] $pcie_refclk
  set sys_clk [ create_bd_intf_port -mode Slave -vlnv xilinx.com:interface:diff_clock_rtl:1.0 sys_clk ]
  set_property -dict [ list \
   CONFIG.FREQ_HZ {300000000} \
   ] $sys_clk

  # Create ports
  set pcie_perstn [ create_bd_port -dir I -type rst pcie_perstn ]
  set_property -dict [ list \
   CONFIG.POLARITY {ACTIVE_LOW} \
 ] $pcie_perstn
  set peripheral_reset_0 [ create_bd_port -dir O -from 0 -to 0 -type rst peripheral_reset_0 ]
  set root_clk [ create_bd_port -dir O -type clk root_clk ]
  set_property -dict [ list \
   CONFIG.ASSOCIATED_BUSIF {SMEM:CONF:SLOT} \
 ] $root_clk

  # Create instance: axi_cdma_0, and set properties
  set axi_cdma_0 [ create_bd_cell -type ip -vlnv xilinx.com:ip:axi_cdma:4.1 axi_cdma_0 ]
  set_property -dict [ list \
   CONFIG.C_ADDR_WIDTH {64} \
   CONFIG.C_INCLUDE_SG {0} \
   CONFIG.C_M_AXI_DATA_WIDTH {64} \
   CONFIG.C_M_AXI_MAX_BURST_LEN {8} \
 ] $axi_cdma_0

  # Create instance: clk_wiz, and set properties
  set clk_wiz [ create_bd_cell -type ip -vlnv xilinx.com:ip:clk_wiz:6.0 clk_wiz ]
  set_property -dict [ list \
   CONFIG.CLKOUT1_DRIVES {BUFGCE} \
   CONFIG.CLKOUT1_JITTER {101.475} \
   CONFIG.CLKOUT1_PHASE_ERROR {77.836} \
   CONFIG.CLKOUT1_REQUESTED_OUT_FREQ {100} \
   CONFIG.CLKOUT2_DRIVES {BUFGCE} \
   CONFIG.CLKOUT3_DRIVES {BUFGCE} \
   CONFIG.CLKOUT4_DRIVES {BUFGCE} \
   CONFIG.CLKOUT5_DRIVES {BUFGCE} \
   CONFIG.CLKOUT6_DRIVES {BUFGCE} \
   CONFIG.CLKOUT7_DRIVES {BUFGCE} \
   CONFIG.FEEDBACK_SOURCE {FDBK_AUTO} \
   CONFIG.MMCM_CLKFBOUT_MULT_F {4.000} \
   CONFIG.MMCM_CLKOUT0_DIVIDE_F {12.000} \
   CONFIG.MMCM_DIVCLK_DIVIDE {1} \
   CONFIG.USE_RESET {false} \
   CONFIG.USE_SAFE_CLOCK_STARTUP {true} \
 ] $clk_wiz

  # Create instance: ddr4_1, and set properties
  set ddr4_1 [ create_bd_cell -type ip -vlnv xilinx.com:ip:ddr4:2.2 ddr4_1 ]

   # Generate the DDR4 Custom Parts File
   set str_ddr4_folder [get_property IP_DIR [ get_ips [ get_property CONFIG.Component_Name $ddr4_1 ] ] ]
   set str_ddr4_file_name BLS4G4D240FSB.csv
   set str_ddr4_file_path ${str_ddr4_folder}/${str_ddr4_file_name}

   write_ddr4_file_shell_ddr4_0_1 $str_ddr4_file_path

  set_property -dict [ list \
   CONFIG.ADDN_UI_CLKOUT1_FREQ_HZ {100} \
   CONFIG.C0.DDR4_AxiAddressWidth {32} \
   CONFIG.C0.DDR4_CLKOUT0_DIVIDE {5} \
   CONFIG.C0.DDR4_CasLatency {16} \
   CONFIG.C0.DDR4_CustomParts {BLS4G4D240FSB.csv} \
   CONFIG.C0.DDR4_DataMask {DM_NO_DBI} \
   CONFIG.C0.DDR4_DataWidth {64} \
   CONFIG.C0.DDR4_Ecc {false} \
   CONFIG.C0.DDR4_InputClockPeriod {3332} \
   CONFIG.C0.DDR4_MemoryPart {BLS4G4D240FSB-2400} \
   CONFIG.C0.DDR4_MemoryType {UDIMMs} \
   CONFIG.C0.DDR4_TimePeriod {833} \
   CONFIG.C0.DDR4_isCustom {true} \
   CONFIG.C0_DDR4_BOARD_INTERFACE {Custom} \
 ] $ddr4_1

  # Create instance: interconnect
  create_hier_cell_interconnect [current_bd_instance .] interconnect

  # Create instance: proc_sys_reset_0, and set properties
  set proc_sys_reset_0 [ create_bd_cell -type ip -vlnv xilinx.com:ip:proc_sys_reset:5.0 proc_sys_reset_0 ]

  # Create instance: proc_sys_reset_1, and set properties
  set proc_sys_reset_1 [ create_bd_cell -type ip -vlnv xilinx.com:ip:proc_sys_reset:5.0 proc_sys_reset_1 ]

  # Create instance: util_ds_buf, and set properties
  set util_ds_buf [ create_bd_cell -type ip -vlnv xilinx.com:ip:util_ds_buf:2.1 util_ds_buf ]
  set_property -dict [ list \
   CONFIG.C_BUF_TYPE {IBUFDSGTE} \
   CONFIG.DIFF_CLK_IN_BOARD_INTERFACE {pcie_refclk} \
   CONFIG.USE_BOARD_FLOW {true} \
 ] $util_ds_buf

  # Create instance: util_ds_buf_0, and set properties
  set util_ds_buf_0 [ create_bd_cell -type ip -vlnv xilinx.com:ip:util_ds_buf:2.1 util_ds_buf_0 ]

  # Create instance: util_vector_logic_0, and set properties
  set util_vector_logic_0 [ create_bd_cell -type ip -vlnv xilinx.com:ip:util_vector_logic:2.0 util_vector_logic_0 ]
  set_property -dict [ list \
   CONFIG.C_OPERATION {not} \
   CONFIG.C_SIZE {1} \
   CONFIG.LOGO_FILE {data/sym_notgate.png} \
 ] $util_vector_logic_0

  # Create instance: xdma
  create_hier_cell_xdma [current_bd_instance .] xdma

  # Create interface connections
  connect_bd_intf_net -intf_net CLK_IN_D_0_1 [get_bd_intf_ports sys_clk] [get_bd_intf_pins util_ds_buf_0/CLK_IN_D]
  connect_bd_intf_net -intf_net S01_AXI_0_1 [get_bd_intf_ports SMEM] [get_bd_intf_pins interconnect/SMEM]
  connect_bd_intf_net -intf_net axi_cdma_0_M_AXI [get_bd_intf_pins axi_cdma_0/M_AXI] [get_bd_intf_pins interconnect/S03_AXI]
  connect_bd_intf_net -intf_net axi_interconnect_0_M01_AXI [get_bd_intf_pins interconnect/M01_AXI] [get_bd_intf_pins xdma/S_AXI_LITE]
  connect_bd_intf_net -intf_net axi_interconnect_0_M02_AXI [get_bd_intf_ports CONF] [get_bd_intf_pins interconnect/CONF]
  connect_bd_intf_net -intf_net axi_interconnect_0_M03_AXI [get_bd_intf_ports SLOT] [get_bd_intf_pins interconnect/SLOT]
  connect_bd_intf_net -intf_net ddr4_1_C0_DDR4 [get_bd_intf_ports C1_DDR4_0] [get_bd_intf_pins ddr4_1/C0_DDR4]
  connect_bd_intf_net -intf_net dimm1_refclk_1 [get_bd_intf_ports dimm1_refclk] [get_bd_intf_pins ddr4_1/C0_SYS_CLK]
  connect_bd_intf_net -intf_net interconnect_M03_AXI [get_bd_intf_pins ddr4_1/C0_DDR4_S_AXI] [get_bd_intf_pins interconnect/DDR]
  connect_bd_intf_net -intf_net interconnect_M04_AXI [get_bd_intf_pins axi_cdma_0/S_AXI_LITE] [get_bd_intf_pins interconnect/M04_AXI]
  connect_bd_intf_net -intf_net interconnect_M05_AXI [get_bd_intf_pins interconnect/M05_AXI] [get_bd_intf_pins xdma/S_AXI_B1]
  connect_bd_intf_net -intf_net pcie_refclk_1 [get_bd_intf_ports pcie_refclk] [get_bd_intf_pins util_ds_buf/CLK_IN_D]
  connect_bd_intf_net -intf_net xdma_M_AXI_B [get_bd_intf_pins interconnect/S_AXI] [get_bd_intf_pins xdma/M_AXI_B]
  connect_bd_intf_net -intf_net xdma_pci_express_x16 [get_bd_intf_ports pci_express_x16] [get_bd_intf_pins xdma/pci_express_x16]

  # Create port connections
  connect_bd_net -net clk_wiz_clk_out1 [get_bd_ports root_clk] [get_bd_pins clk_wiz/clk_out1] [get_bd_pins interconnect/root_clk] [get_bd_pins proc_sys_reset_0/slowest_sync_clk]
  connect_bd_net -net clk_wiz_locked [get_bd_pins clk_wiz/locked] [get_bd_pins proc_sys_reset_0/dcm_locked]
  connect_bd_net -net ddr4_1_c0_ddr4_ui_clk [get_bd_pins ddr4_1/c0_ddr4_ui_clk] [get_bd_pins interconnect/aclk2] [get_bd_pins proc_sys_reset_1/slowest_sync_clk]
  connect_bd_net -net ddr4_1_c0_ddr4_ui_clk_sync_rst [get_bd_pins ddr4_1/c0_ddr4_ui_clk_sync_rst] [get_bd_pins proc_sys_reset_1/ext_reset_in]
  connect_bd_net -net pcie_perstn_1 [get_bd_ports pcie_perstn] [get_bd_pins util_vector_logic_0/Op1] [get_bd_pins xdma/pcie_perstn]
  connect_bd_net -net proc_sys_reset_0_peripheral_reset [get_bd_ports peripheral_reset_0] [get_bd_pins proc_sys_reset_0/peripheral_reset]
  connect_bd_net -net proc_sys_reset_1_peripheral_aresetn [get_bd_pins ddr4_1/c0_ddr4_aresetn] [get_bd_pins proc_sys_reset_1/peripheral_aresetn]
  connect_bd_net -net util_ds_buf_0_IBUF_OUT [get_bd_pins clk_wiz/clk_in1] [get_bd_pins util_ds_buf_0/IBUF_OUT]
  connect_bd_net -net util_ds_buf_IBUF_DS_ODIV2 [get_bd_pins util_ds_buf/IBUF_DS_ODIV2] [get_bd_pins xdma/sys_clk]
  connect_bd_net -net util_ds_buf_IBUF_OUT [get_bd_pins util_ds_buf/IBUF_OUT] [get_bd_pins xdma/sys_clk_gt]
  connect_bd_net -net util_vector_logic_0_Res [get_bd_pins ddr4_1/sys_rst] [get_bd_pins util_vector_logic_0/Res]
  connect_bd_net -net xdma_0_axi_aclk [get_bd_pins axi_cdma_0/m_axi_aclk] [get_bd_pins axi_cdma_0/s_axi_lite_aclk] [get_bd_pins interconnect/M00_ACLK] [get_bd_pins xdma/axi_aclk]
  connect_bd_net -net xdma_0_axi_aresetn [get_bd_pins axi_cdma_0/s_axi_lite_aresetn] [get_bd_pins interconnect/M00_ARESETN] [get_bd_pins proc_sys_reset_0/ext_reset_in] [get_bd_pins xdma/axi_aresetn]

  # Create address segments
  create_bd_addr_seg -range 0x20000000 -offset 0x000120000000 [get_bd_addr_spaces axi_cdma_0/Data] [get_bd_addr_segs ddr4_1/C0_DDR4_MEMORY_MAP/C0_DDR4_ADDRESS_BLOCK] SEG_ddr4_1_C0_DDR4_ADDRESS_BLOCK
  create_bd_addr_seg -range 0x20000000 -offset 0x00000000 [get_bd_addr_spaces axi_cdma_0/Data] [get_bd_addr_segs xdma/xdma_0/S_AXI_B/BAR0] SEG_xdma_0_BAR0
  create_bd_addr_seg -range 0x00100000 -offset 0x1000000000100000 [get_bd_addr_spaces xdma/xdma_0/M_AXI_B] [get_bd_addr_segs CONF/Reg] SEG_CONF_Reg
  create_bd_addr_seg -range 0x20000000 -offset 0x000100000000 [get_bd_addr_spaces xdma/xdma_0/M_AXI_B] [get_bd_addr_segs SLOT/Reg] SEG_SLOT_Reg
  create_bd_addr_seg -range 0x00100000 -offset 0x1000000000200000 [get_bd_addr_spaces xdma/xdma_0/M_AXI_B] [get_bd_addr_segs axi_cdma_0/S_AXI_LITE/Reg] SEG_axi_cdma_0_Reg
  create_bd_addr_seg -range 0x00100000 -offset 0x1000000000000000 [get_bd_addr_spaces xdma/xdma_0/M_AXI_B] [get_bd_addr_segs xdma/xdma_0/S_AXI_LITE/CTL0] SEG_xdma_0_CTL0
  create_bd_addr_seg -range 0x20000000 -offset 0x000120000000 [get_bd_addr_spaces SMEM] [get_bd_addr_segs ddr4_1/C0_DDR4_MEMORY_MAP/C0_DDR4_ADDRESS_BLOCK] SEG_ddr4_1_C0_DDR4_ADDRESS_BLOCK

  # Exclude Address Segments
  create_bd_addr_seg -range 0x00100000 -offset 0x1000000000100000 [get_bd_addr_spaces SMEM] [get_bd_addr_segs CONF/Reg] SEG_CONF_Reg
  exclude_bd_addr_seg [get_bd_addr_segs SMEM/SEG_CONF_Reg]

  create_bd_addr_seg -range 0x20000000 -offset 0x000100000000 [get_bd_addr_spaces SMEM] [get_bd_addr_segs SLOT/Reg] SEG_SLOT_Reg
  exclude_bd_addr_seg [get_bd_addr_segs SMEM/SEG_SLOT_Reg]

  create_bd_addr_seg -range 0x00100000 -offset 0x1000000000200000 [get_bd_addr_spaces SMEM] [get_bd_addr_segs axi_cdma_0/S_AXI_LITE/Reg] SEG_axi_cdma_0_Reg
  exclude_bd_addr_seg [get_bd_addr_segs SMEM/SEG_axi_cdma_0_Reg]

  create_bd_addr_seg -range 0x20000000 -offset 0x00000000 [get_bd_addr_spaces SMEM] [get_bd_addr_segs xdma/xdma_0/S_AXI_B/BAR0] SEG_xdma_0_BAR0
  exclude_bd_addr_seg [get_bd_addr_segs SMEM/SEG_xdma_0_BAR0]

  create_bd_addr_seg -range 0x00100000 -offset 0x1000000000000000 [get_bd_addr_spaces SMEM] [get_bd_addr_segs xdma/xdma_0/S_AXI_LITE/CTL0] SEG_xdma_0_CTL0
  exclude_bd_addr_seg [get_bd_addr_segs SMEM/SEG_xdma_0_CTL0]

  create_bd_addr_seg -range 0x00100000 -offset 0x1000000000100000 [get_bd_addr_spaces axi_cdma_0/Data] [get_bd_addr_segs CONF/Reg] SEG_CONF_Reg
  exclude_bd_addr_seg [get_bd_addr_segs axi_cdma_0/Data/SEG_CONF_Reg]

  create_bd_addr_seg -range 0x20000000 -offset 0x000100000000 [get_bd_addr_spaces axi_cdma_0/Data] [get_bd_addr_segs SLOT/Reg] SEG_SLOT_Reg
  exclude_bd_addr_seg [get_bd_addr_segs axi_cdma_0/Data/SEG_SLOT_Reg]

  create_bd_addr_seg -range 0x00100000 -offset 0x1000000000200000 [get_bd_addr_spaces axi_cdma_0/Data] [get_bd_addr_segs axi_cdma_0/S_AXI_LITE/Reg] SEG_axi_cdma_0_Reg
  exclude_bd_addr_seg [get_bd_addr_segs axi_cdma_0/Data/SEG_axi_cdma_0_Reg]

  create_bd_addr_seg -range 0x00100000 -offset 0x1000000000000000 [get_bd_addr_spaces axi_cdma_0/Data] [get_bd_addr_segs xdma/xdma_0/S_AXI_LITE/CTL0] SEG_xdma_0_CTL0
  exclude_bd_addr_seg [get_bd_addr_segs axi_cdma_0/Data/SEG_xdma_0_CTL0]

  create_bd_addr_seg -range 0x20000000 -offset 0x000120000000 [get_bd_addr_spaces xdma/xdma_0/M_AXI_B] [get_bd_addr_segs ddr4_1/C0_DDR4_MEMORY_MAP/C0_DDR4_ADDRESS_BLOCK] SEG_ddr4_1_C0_DDR4_ADDRESS_BLOCK
  exclude_bd_addr_seg [get_bd_addr_segs xdma/xdma_0/M_AXI_B/SEG_ddr4_1_C0_DDR4_ADDRESS_BLOCK]

  create_bd_addr_seg -range 0x20000000 -offset 0x00000000 [get_bd_addr_spaces xdma/xdma_0/M_AXI_B] [get_bd_addr_segs xdma/xdma_0/S_AXI_B/BAR0] SEG_xdma_0_BAR0
  exclude_bd_addr_seg [get_bd_addr_segs xdma/xdma_0/M_AXI_B/SEG_xdma_0_BAR0]



  # Restore current instance
  current_bd_instance $oldCurInst

  save_bd_design
}
# End of create_root_design()


##################################################################
# MAIN FLOW
##################################################################

create_root_design ""


common::send_msg_id "BD_TCL-1000" "WARNING" "This Tcl script was generated from a block design that has not been validated. It is possible that design <$design_name> may result in errors during validation."

