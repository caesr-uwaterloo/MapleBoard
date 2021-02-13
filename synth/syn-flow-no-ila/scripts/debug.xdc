
# This file is generated automatically by Python script
# create the debug core (and dbg_hub automatically)
create_debug_core u_ila_0 ila

# some fixed parameter that is used in the dialog
set_property ALL_PROBE_SAME_MU true [get_debug_cores u_ila_0]
set_property ALL_PROBE_SAME_MU_CNT 1 [get_debug_cores u_ila_0]
set_property C_ADV_TRIGGER false [get_debug_cores u_ila_0]
set_property C_DATA_DEPTH 16384 [get_debug_cores u_ila_0]
set_property C_EN_STRG_QUAL false [get_debug_cores u_ila_0]
set_property C_INPUT_PIPE_STAGES 1 [get_debug_cores u_ila_0]
set_property C_TRIGIN_EN false [get_debug_cores u_ila_0]
set_property C_TRIGOUT_EN false [get_debug_cores u_ila_0]

# Setup the clock for u_ila_0
set_property port_width 1 [get_debug_ports u_ila_0/clk]
connect_debug_port u_ila_0/clk [get_nets [list sh_root_clk]]


set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe0]
set_property port_width 64 [get_debug_ports u_ila_0/probe0]
connect_debug_port u_ila_0/probe0 [ get_nets [list {Confreg/reg_initPC[0]} {Confreg/reg_initPC[1]} {Confreg/reg_initPC[2]} {Confreg/reg_initPC[3]} {Confreg/reg_initPC[4]} {Confreg/reg_initPC[5]} {Confreg/reg_initPC[6]} {Confreg/reg_initPC[7]} {Confreg/reg_initPC[8]} {Confreg/reg_initPC[9]} {Confreg/reg_initPC[10]} {Confreg/reg_initPC[11]} {Confreg/reg_initPC[12]} {Confreg/reg_initPC[13]} {Confreg/reg_initPC[14]} {Confreg/reg_initPC[15]} {Confreg/reg_initPC[16]} {Confreg/reg_initPC[17]} {Confreg/reg_initPC[18]} {Confreg/reg_initPC[19]} {Confreg/reg_initPC[20]} {Confreg/reg_initPC[21]} {Confreg/reg_initPC[22]} {Confreg/reg_initPC[23]} {Confreg/reg_initPC[24]} {Confreg/reg_initPC[25]} {Confreg/reg_initPC[26]} {Confreg/reg_initPC[27]} {Confreg/reg_initPC[28]} {Confreg/reg_initPC[29]} {Confreg/reg_initPC[30]} {Confreg/reg_initPC[31]} {Confreg/reg_initPC[32]} {Confreg/reg_initPC[33]} {Confreg/reg_initPC[34]} {Confreg/reg_initPC[35]} {Confreg/reg_initPC[36]} {Confreg/reg_initPC[37]} {Confreg/reg_initPC[38]} {Confreg/reg_initPC[39]} {Confreg/reg_initPC[40]} {Confreg/reg_initPC[41]} {Confreg/reg_initPC[42]} {Confreg/reg_initPC[43]} {Confreg/reg_initPC[44]} {Confreg/reg_initPC[45]} {Confreg/reg_initPC[46]} {Confreg/reg_initPC[47]} {Confreg/reg_initPC[48]} {Confreg/reg_initPC[49]} {Confreg/reg_initPC[50]} {Confreg/reg_initPC[51]} {Confreg/reg_initPC[52]} {Confreg/reg_initPC[53]} {Confreg/reg_initPC[54]} {Confreg/reg_initPC[55]} {Confreg/reg_initPC[56]} {Confreg/reg_initPC[57]} {Confreg/reg_initPC[58]} {Confreg/reg_initPC[59]} {Confreg/reg_initPC[60]} {Confreg/reg_initPC[61]} {Confreg/reg_initPC[62]} {Confreg/reg_initPC[63]} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe1]
set_property port_width 64 [get_debug_ports u_ila_0/probe1]
connect_debug_port u_ila_0/probe1 [ get_nets [list {Confreg/reg_resetReg[0]} {Confreg/reg_resetReg[1]} {Confreg/reg_resetReg[2]} {Confreg/reg_resetReg[3]} {Confreg/reg_resetReg[4]} {Confreg/reg_resetReg[5]} {Confreg/reg_resetReg[6]} {Confreg/reg_resetReg[7]} {Confreg/reg_resetReg[8]} {Confreg/reg_resetReg[9]} {Confreg/reg_resetReg[10]} {Confreg/reg_resetReg[11]} {Confreg/reg_resetReg[12]} {Confreg/reg_resetReg[13]} {Confreg/reg_resetReg[14]} {Confreg/reg_resetReg[15]} {Confreg/reg_resetReg[16]} {Confreg/reg_resetReg[17]} {Confreg/reg_resetReg[18]} {Confreg/reg_resetReg[19]} {Confreg/reg_resetReg[20]} {Confreg/reg_resetReg[21]} {Confreg/reg_resetReg[22]} {Confreg/reg_resetReg[23]} {Confreg/reg_resetReg[24]} {Confreg/reg_resetReg[25]} {Confreg/reg_resetReg[26]} {Confreg/reg_resetReg[27]} {Confreg/reg_resetReg[28]} {Confreg/reg_resetReg[29]} {Confreg/reg_resetReg[30]} {Confreg/reg_resetReg[31]} {Confreg/reg_resetReg[32]} {Confreg/reg_resetReg[33]} {Confreg/reg_resetReg[34]} {Confreg/reg_resetReg[35]} {Confreg/reg_resetReg[36]} {Confreg/reg_resetReg[37]} {Confreg/reg_resetReg[38]} {Confreg/reg_resetReg[39]} {Confreg/reg_resetReg[40]} {Confreg/reg_resetReg[41]} {Confreg/reg_resetReg[42]} {Confreg/reg_resetReg[43]} {Confreg/reg_resetReg[44]} {Confreg/reg_resetReg[45]} {Confreg/reg_resetReg[46]} {Confreg/reg_resetReg[47]} {Confreg/reg_resetReg[48]} {Confreg/reg_resetReg[49]} {Confreg/reg_resetReg[50]} {Confreg/reg_resetReg[51]} {Confreg/reg_resetReg[52]} {Confreg/reg_resetReg[53]} {Confreg/reg_resetReg[54]} {Confreg/reg_resetReg[55]} {Confreg/reg_resetReg[56]} {Confreg/reg_resetReg[57]} {Confreg/reg_resetReg[58]} {Confreg/reg_resetReg[59]} {Confreg/reg_resetReg[60]} {Confreg/reg_resetReg[61]} {Confreg/reg_resetReg[62]} {Confreg/reg_resetReg[63]} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe2]
set_property port_width 64 [get_debug_ports u_ila_0/probe2]
connect_debug_port u_ila_0/probe2 [ get_nets [list {Confreg/s_axi_awaddr[0]} {Confreg/s_axi_awaddr[1]} {Confreg/s_axi_awaddr[2]} {Confreg/s_axi_awaddr[3]} {Confreg/s_axi_awaddr[4]} {Confreg/s_axi_awaddr[5]} {Confreg/s_axi_awaddr[6]} {Confreg/s_axi_awaddr[7]} {Confreg/s_axi_awaddr[8]} {Confreg/s_axi_awaddr[9]} {Confreg/s_axi_awaddr[10]} {Confreg/s_axi_awaddr[11]} {Confreg/s_axi_awaddr[12]} {Confreg/s_axi_awaddr[13]} {Confreg/s_axi_awaddr[14]} {Confreg/s_axi_awaddr[15]} {Confreg/s_axi_awaddr[16]} {Confreg/s_axi_awaddr[17]} {Confreg/s_axi_awaddr[18]} {Confreg/s_axi_awaddr[19]} {Confreg/s_axi_awaddr[20]} {Confreg/s_axi_awaddr[21]} {Confreg/s_axi_awaddr[22]} {Confreg/s_axi_awaddr[23]} {Confreg/s_axi_awaddr[24]} {Confreg/s_axi_awaddr[25]} {Confreg/s_axi_awaddr[26]} {Confreg/s_axi_awaddr[27]} {Confreg/s_axi_awaddr[28]} {Confreg/s_axi_awaddr[29]} {Confreg/s_axi_awaddr[30]} {Confreg/s_axi_awaddr[31]} {Confreg/s_axi_awaddr[32]} {Confreg/s_axi_awaddr[33]} {Confreg/s_axi_awaddr[34]} {Confreg/s_axi_awaddr[35]} {Confreg/s_axi_awaddr[36]} {Confreg/s_axi_awaddr[37]} {Confreg/s_axi_awaddr[38]} {Confreg/s_axi_awaddr[39]} {Confreg/s_axi_awaddr[40]} {Confreg/s_axi_awaddr[41]} {Confreg/s_axi_awaddr[42]} {Confreg/s_axi_awaddr[43]} {Confreg/s_axi_awaddr[44]} {Confreg/s_axi_awaddr[45]} {Confreg/s_axi_awaddr[46]} {Confreg/s_axi_awaddr[47]} {Confreg/s_axi_awaddr[48]} {Confreg/s_axi_awaddr[49]} {Confreg/s_axi_awaddr[50]} {Confreg/s_axi_awaddr[51]} {Confreg/s_axi_awaddr[52]} {Confreg/s_axi_awaddr[53]} {Confreg/s_axi_awaddr[54]} {Confreg/s_axi_awaddr[55]} {Confreg/s_axi_awaddr[56]} {Confreg/s_axi_awaddr[57]} {Confreg/s_axi_awaddr[58]} {Confreg/s_axi_awaddr[59]} {Confreg/s_axi_awaddr[60]} {Confreg/s_axi_awaddr[61]} {Confreg/s_axi_awaddr[62]} {Confreg/s_axi_awaddr[63]} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe3]
set_property port_width 1 [get_debug_ports u_ila_0/probe3]
connect_debug_port u_ila_0/probe3 [ get_nets [list {Confreg/s_axi_awvalid} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe4]
set_property port_width 1 [get_debug_ports u_ila_0/probe4]
connect_debug_port u_ila_0/probe4 [ get_nets [list {Confreg/s_axi_awready} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe5]
set_property port_width 64 [get_debug_ports u_ila_0/probe5]
connect_debug_port u_ila_0/probe5 [ get_nets [list {Confreg/s_axi_wdata[0]} {Confreg/s_axi_wdata[1]} {Confreg/s_axi_wdata[2]} {Confreg/s_axi_wdata[3]} {Confreg/s_axi_wdata[4]} {Confreg/s_axi_wdata[5]} {Confreg/s_axi_wdata[6]} {Confreg/s_axi_wdata[7]} {Confreg/s_axi_wdata[8]} {Confreg/s_axi_wdata[9]} {Confreg/s_axi_wdata[10]} {Confreg/s_axi_wdata[11]} {Confreg/s_axi_wdata[12]} {Confreg/s_axi_wdata[13]} {Confreg/s_axi_wdata[14]} {Confreg/s_axi_wdata[15]} {Confreg/s_axi_wdata[16]} {Confreg/s_axi_wdata[17]} {Confreg/s_axi_wdata[18]} {Confreg/s_axi_wdata[19]} {Confreg/s_axi_wdata[20]} {Confreg/s_axi_wdata[21]} {Confreg/s_axi_wdata[22]} {Confreg/s_axi_wdata[23]} {Confreg/s_axi_wdata[24]} {Confreg/s_axi_wdata[25]} {Confreg/s_axi_wdata[26]} {Confreg/s_axi_wdata[27]} {Confreg/s_axi_wdata[28]} {Confreg/s_axi_wdata[29]} {Confreg/s_axi_wdata[30]} {Confreg/s_axi_wdata[31]} {Confreg/s_axi_wdata[32]} {Confreg/s_axi_wdata[33]} {Confreg/s_axi_wdata[34]} {Confreg/s_axi_wdata[35]} {Confreg/s_axi_wdata[36]} {Confreg/s_axi_wdata[37]} {Confreg/s_axi_wdata[38]} {Confreg/s_axi_wdata[39]} {Confreg/s_axi_wdata[40]} {Confreg/s_axi_wdata[41]} {Confreg/s_axi_wdata[42]} {Confreg/s_axi_wdata[43]} {Confreg/s_axi_wdata[44]} {Confreg/s_axi_wdata[45]} {Confreg/s_axi_wdata[46]} {Confreg/s_axi_wdata[47]} {Confreg/s_axi_wdata[48]} {Confreg/s_axi_wdata[49]} {Confreg/s_axi_wdata[50]} {Confreg/s_axi_wdata[51]} {Confreg/s_axi_wdata[52]} {Confreg/s_axi_wdata[53]} {Confreg/s_axi_wdata[54]} {Confreg/s_axi_wdata[55]} {Confreg/s_axi_wdata[56]} {Confreg/s_axi_wdata[57]} {Confreg/s_axi_wdata[58]} {Confreg/s_axi_wdata[59]} {Confreg/s_axi_wdata[60]} {Confreg/s_axi_wdata[61]} {Confreg/s_axi_wdata[62]} {Confreg/s_axi_wdata[63]} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe6]
set_property port_width 1 [get_debug_ports u_ila_0/probe6]
connect_debug_port u_ila_0/probe6 [ get_nets [list {Confreg/s_axi_wvalid} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe7]
set_property port_width 1 [get_debug_ports u_ila_0/probe7]
connect_debug_port u_ila_0/probe7 [ get_nets [list {Confreg/s_axi_wready} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe8]
set_property port_width 1 [get_debug_ports u_ila_0/probe8]
connect_debug_port u_ila_0/probe8 [ get_nets [list {Confreg/s_axi_bvalid} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe9]
set_property port_width 1 [get_debug_ports u_ila_0/probe9]
connect_debug_port u_ila_0/probe9 [ get_nets [list {Confreg/s_axi_bready} ] ]


# Something for the debug hub
# when you create u_ila_0, the dbg_hub is created as well
# these parts should be constant
set_property C_CLK_INPUT_FREQ_HZ 100000000 [get_debug_cores dbg_hub]
set_property C_ENABLE_CLK_DIVIDER true [get_debug_cores dbg_hub]
set_property C_USER_SCAN_CHAIN 1 [get_debug_cores dbg_hub]
connect_debug_port dbg_hub/clk [get_nets sh_root_clk]


