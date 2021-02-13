
# This file is generated automatically by Python script
# create the debug core (and dbg_hub automatically)
create_debug_core u_ila_0 ila

# some fixed parameter that is used in the dialog
set_property ALL_PROBE_SAME_MU true [get_debug_cores u_ila_0]
set_property ALL_PROBE_SAME_MU_CNT 4 [get_debug_cores u_ila_0]
set_property C_ADV_TRIGGER true [get_debug_cores u_ila_0]
set_property C_DATA_DEPTH 16384 [get_debug_cores u_ila_0]
set_property C_EN_STRG_QUAL true [get_debug_cores u_ila_0]
set_property C_INPUT_PIPE_STAGES 3 [get_debug_cores u_ila_0]
set_property C_TRIGIN_EN false [get_debug_cores u_ila_0]
set_property C_TRIGOUT_EN false [get_debug_cores u_ila_0]

# Setup the clock for u_ila_0
set_property port_width 1 [get_debug_ports u_ila_0/clk]
connect_debug_port u_ila_0/clk [get_nets [list sh_root_clk]]


set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe0]
set_property port_width 64 [get_debug_ports u_ila_0/probe0]
set_property MARK_DEBUG TRUE [ get_nets Confreg/reg_initPC* ]
connect_debug_port u_ila_0/probe0 [ get_nets [list {Confreg/reg_initPC[0]} {Confreg/reg_initPC[1]} {Confreg/reg_initPC[2]} {Confreg/reg_initPC[3]} {Confreg/reg_initPC[4]} {Confreg/reg_initPC[5]} {Confreg/reg_initPC[6]} {Confreg/reg_initPC[7]} {Confreg/reg_initPC[8]} {Confreg/reg_initPC[9]} {Confreg/reg_initPC[10]} {Confreg/reg_initPC[11]} {Confreg/reg_initPC[12]} {Confreg/reg_initPC[13]} {Confreg/reg_initPC[14]} {Confreg/reg_initPC[15]} {Confreg/reg_initPC[16]} {Confreg/reg_initPC[17]} {Confreg/reg_initPC[18]} {Confreg/reg_initPC[19]} {Confreg/reg_initPC[20]} {Confreg/reg_initPC[21]} {Confreg/reg_initPC[22]} {Confreg/reg_initPC[23]} {Confreg/reg_initPC[24]} {Confreg/reg_initPC[25]} {Confreg/reg_initPC[26]} {Confreg/reg_initPC[27]} {Confreg/reg_initPC[28]} {Confreg/reg_initPC[29]} {Confreg/reg_initPC[30]} {Confreg/reg_initPC[31]} {Confreg/reg_initPC[32]} {Confreg/reg_initPC[33]} {Confreg/reg_initPC[34]} {Confreg/reg_initPC[35]} {Confreg/reg_initPC[36]} {Confreg/reg_initPC[37]} {Confreg/reg_initPC[38]} {Confreg/reg_initPC[39]} {Confreg/reg_initPC[40]} {Confreg/reg_initPC[41]} {Confreg/reg_initPC[42]} {Confreg/reg_initPC[43]} {Confreg/reg_initPC[44]} {Confreg/reg_initPC[45]} {Confreg/reg_initPC[46]} {Confreg/reg_initPC[47]} {Confreg/reg_initPC[48]} {Confreg/reg_initPC[49]} {Confreg/reg_initPC[50]} {Confreg/reg_initPC[51]} {Confreg/reg_initPC[52]} {Confreg/reg_initPC[53]} {Confreg/reg_initPC[54]} {Confreg/reg_initPC[55]} {Confreg/reg_initPC[56]} {Confreg/reg_initPC[57]} {Confreg/reg_initPC[58]} {Confreg/reg_initPC[59]} {Confreg/reg_initPC[60]} {Confreg/reg_initPC[61]} {Confreg/reg_initPC[62]} {Confreg/reg_initPC[63]} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe1]
set_property port_width 64 [get_debug_ports u_ila_0/probe1]
set_property MARK_DEBUG TRUE [ get_nets Confreg/reg_resetReg* ]
connect_debug_port u_ila_0/probe1 [ get_nets [list {Confreg/reg_resetReg[0]} {Confreg/reg_resetReg[1]} {Confreg/reg_resetReg[2]} {Confreg/reg_resetReg[3]} {Confreg/reg_resetReg[4]} {Confreg/reg_resetReg[5]} {Confreg/reg_resetReg[6]} {Confreg/reg_resetReg[7]} {Confreg/reg_resetReg[8]} {Confreg/reg_resetReg[9]} {Confreg/reg_resetReg[10]} {Confreg/reg_resetReg[11]} {Confreg/reg_resetReg[12]} {Confreg/reg_resetReg[13]} {Confreg/reg_resetReg[14]} {Confreg/reg_resetReg[15]} {Confreg/reg_resetReg[16]} {Confreg/reg_resetReg[17]} {Confreg/reg_resetReg[18]} {Confreg/reg_resetReg[19]} {Confreg/reg_resetReg[20]} {Confreg/reg_resetReg[21]} {Confreg/reg_resetReg[22]} {Confreg/reg_resetReg[23]} {Confreg/reg_resetReg[24]} {Confreg/reg_resetReg[25]} {Confreg/reg_resetReg[26]} {Confreg/reg_resetReg[27]} {Confreg/reg_resetReg[28]} {Confreg/reg_resetReg[29]} {Confreg/reg_resetReg[30]} {Confreg/reg_resetReg[31]} {Confreg/reg_resetReg[32]} {Confreg/reg_resetReg[33]} {Confreg/reg_resetReg[34]} {Confreg/reg_resetReg[35]} {Confreg/reg_resetReg[36]} {Confreg/reg_resetReg[37]} {Confreg/reg_resetReg[38]} {Confreg/reg_resetReg[39]} {Confreg/reg_resetReg[40]} {Confreg/reg_resetReg[41]} {Confreg/reg_resetReg[42]} {Confreg/reg_resetReg[43]} {Confreg/reg_resetReg[44]} {Confreg/reg_resetReg[45]} {Confreg/reg_resetReg[46]} {Confreg/reg_resetReg[47]} {Confreg/reg_resetReg[48]} {Confreg/reg_resetReg[49]} {Confreg/reg_resetReg[50]} {Confreg/reg_resetReg[51]} {Confreg/reg_resetReg[52]} {Confreg/reg_resetReg[53]} {Confreg/reg_resetReg[54]} {Confreg/reg_resetReg[55]} {Confreg/reg_resetReg[56]} {Confreg/reg_resetReg[57]} {Confreg/reg_resetReg[58]} {Confreg/reg_resetReg[59]} {Confreg/reg_resetReg[60]} {Confreg/reg_resetReg[61]} {Confreg/reg_resetReg[62]} {Confreg/reg_resetReg[63]} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe2]
set_property port_width 2 [get_debug_ports u_ila_0/probe2]
set_property MARK_DEBUG TRUE [ get_nets Confreg/reg_st* ]
connect_debug_port u_ila_0/probe2 [ get_nets [list {Confreg/reg_st[0]} {Confreg/reg_st[1]} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe3]
set_property port_width 64 [get_debug_ports u_ila_0/probe3]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address* ]
connect_debug_port u_ila_0/probe3 [ get_nets [list {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[0]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[1]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[2]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[3]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[4]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[5]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[6]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[7]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[8]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[9]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[10]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[11]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[12]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[13]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[14]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[15]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[16]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[17]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[18]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[19]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[20]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[21]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[22]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[23]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[24]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[25]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[26]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[27]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[28]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[29]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[30]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[31]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[32]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[33]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[34]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[35]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[36]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[37]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[38]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[39]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[40]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[41]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[42]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[43]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[44]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[45]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[46]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[47]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[48]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[49]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[50]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[51]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[52]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[53]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[54]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[55]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[56]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[57]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[58]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[59]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[60]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[61]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[62]} {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address[63]} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe4]
set_property port_width 1 [get_debug_ports u_ila_0/probe4]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_valid* ]
connect_debug_port u_ila_0/probe4 [ get_nets [list {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_valid} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe5]
set_property port_width 1 [get_debug_ports u_ila_0/probe5]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_ready* ]
connect_debug_port u_ila_0/probe5 [ get_nets [list {PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_ready} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe6]
set_property port_width 64 [get_debug_ports u_ila_0/probe6]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data* ]
connect_debug_port u_ila_0/probe6 [ get_nets [list {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[0]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[1]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[2]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[3]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[4]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[5]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[6]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[7]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[8]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[9]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[10]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[11]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[12]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[13]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[14]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[15]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[16]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[17]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[18]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[19]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[20]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[21]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[22]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[23]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[24]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[25]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[26]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[27]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[28]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[29]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[30]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[31]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[32]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[33]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[34]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[35]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[36]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[37]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[38]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[39]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[40]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[41]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[42]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[43]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[44]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[45]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[46]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[47]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[48]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[49]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[50]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[51]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[52]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[53]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[54]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[55]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[56]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[57]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[58]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[59]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[60]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[61]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[62]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data[63]} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe7]
set_property port_width 64 [get_debug_ports u_ila_0/probe7]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency* ]
connect_debug_port u_ila_0/probe7 [ get_nets [list {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[0]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[1]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[2]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[3]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[4]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[5]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[6]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[7]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[8]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[9]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[10]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[11]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[12]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[13]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[14]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[15]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[16]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[17]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[18]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[19]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[20]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[21]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[22]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[23]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[24]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[25]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[26]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[27]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[28]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[29]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[30]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[31]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[32]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[33]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[34]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[35]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[36]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[37]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[38]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[39]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[40]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[41]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[42]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[43]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[44]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[45]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[46]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[47]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[48]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[49]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[50]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[51]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[52]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[53]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[54]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[55]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[56]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[57]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[58]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[59]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[60]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[61]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[62]} {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency[63]} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe8]
set_property port_width 1 [get_debug_ports u_ila_0/probe8]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_valid* ]
connect_debug_port u_ila_0/probe8 [ get_nets [list {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_valid} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe9]
set_property port_width 1 [get_debug_ports u_ila_0/probe9]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_ready* ]
connect_debug_port u_ila_0/probe9 [ get_nets [list {PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_ready} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe10]
set_property port_width 64 [get_debug_ports u_ila_0/probe10]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address* ]
connect_debug_port u_ila_0/probe10 [ get_nets [list {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[0]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[1]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[2]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[3]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[4]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[5]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[6]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[7]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[8]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[9]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[10]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[11]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[12]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[13]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[14]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[15]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[16]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[17]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[18]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[19]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[20]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[21]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[22]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[23]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[24]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[25]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[26]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[27]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[28]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[29]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[30]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[31]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[32]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[33]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[34]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[35]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[36]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[37]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[38]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[39]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[40]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[41]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[42]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[43]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[44]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[45]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[46]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[47]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[48]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[49]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[50]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[51]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[52]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[53]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[54]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[55]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[56]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[57]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[58]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[59]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[60]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[61]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[62]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address[63]} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe11]
set_property port_width 1 [get_debug_ports u_ila_0/probe11]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_valid* ]
connect_debug_port u_ila_0/probe11 [ get_nets [list {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_valid} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe12]
set_property port_width 1 [get_debug_ports u_ila_0/probe12]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_ready* ]
connect_debug_port u_ila_0/probe12 [ get_nets [list {PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_ready} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe13]
set_property port_width 64 [get_debug_ports u_ila_0/probe13]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data* ]
connect_debug_port u_ila_0/probe13 [ get_nets [list {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[0]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[1]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[2]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[3]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[4]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[5]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[6]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[7]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[8]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[9]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[10]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[11]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[12]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[13]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[14]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[15]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[16]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[17]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[18]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[19]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[20]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[21]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[22]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[23]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[24]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[25]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[26]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[27]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[28]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[29]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[30]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[31]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[32]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[33]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[34]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[35]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[36]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[37]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[38]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[39]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[40]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[41]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[42]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[43]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[44]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[45]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[46]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[47]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[48]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[49]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[50]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[51]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[52]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[53]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[54]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[55]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[56]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[57]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[58]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[59]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[60]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[61]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[62]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data[63]} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe14]
set_property port_width 64 [get_debug_ports u_ila_0/probe14]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency* ]
connect_debug_port u_ila_0/probe14 [ get_nets [list {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[0]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[1]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[2]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[3]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[4]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[5]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[6]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[7]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[8]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[9]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[10]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[11]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[12]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[13]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[14]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[15]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[16]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[17]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[18]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[19]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[20]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[21]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[22]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[23]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[24]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[25]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[26]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[27]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[28]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[29]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[30]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[31]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[32]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[33]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[34]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[35]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[36]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[37]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[38]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[39]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[40]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[41]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[42]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[43]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[44]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[45]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[46]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[47]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[48]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[49]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[50]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[51]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[52]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[53]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[54]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[55]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[56]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[57]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[58]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[59]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[60]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[61]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[62]} {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency[63]} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe15]
set_property port_width 1 [get_debug_ports u_ila_0/probe15]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_valid* ]
connect_debug_port u_ila_0/probe15 [ get_nets [list {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_valid} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe16]
set_property port_width 1 [get_debug_ports u_ila_0/probe16]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_ready* ]
connect_debug_port u_ila_0/probe16 [ get_nets [list {PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_ready} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe17]
set_property port_width 64 [get_debug_ports u_ila_0/probe17]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address* ]
connect_debug_port u_ila_0/probe17 [ get_nets [list {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[0]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[1]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[2]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[3]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[4]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[5]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[6]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[7]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[8]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[9]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[10]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[11]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[12]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[13]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[14]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[15]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[16]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[17]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[18]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[19]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[20]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[21]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[22]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[23]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[24]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[25]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[26]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[27]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[28]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[29]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[30]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[31]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[32]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[33]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[34]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[35]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[36]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[37]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[38]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[39]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[40]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[41]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[42]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[43]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[44]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[45]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[46]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[47]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[48]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[49]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[50]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[51]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[52]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[53]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[54]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[55]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[56]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[57]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[58]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[59]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[60]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[61]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[62]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address[63]} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe18]
set_property port_width 1 [get_debug_ports u_ila_0/probe18]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_valid* ]
connect_debug_port u_ila_0/probe18 [ get_nets [list {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_valid} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe19]
set_property port_width 1 [get_debug_ports u_ila_0/probe19]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_ready* ]
connect_debug_port u_ila_0/probe19 [ get_nets [list {PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_ready} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe20]
set_property port_width 64 [get_debug_ports u_ila_0/probe20]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data* ]
connect_debug_port u_ila_0/probe20 [ get_nets [list {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[0]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[1]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[2]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[3]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[4]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[5]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[6]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[7]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[8]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[9]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[10]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[11]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[12]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[13]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[14]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[15]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[16]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[17]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[18]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[19]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[20]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[21]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[22]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[23]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[24]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[25]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[26]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[27]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[28]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[29]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[30]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[31]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[32]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[33]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[34]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[35]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[36]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[37]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[38]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[39]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[40]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[41]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[42]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[43]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[44]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[45]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[46]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[47]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[48]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[49]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[50]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[51]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[52]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[53]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[54]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[55]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[56]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[57]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[58]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[59]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[60]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[61]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[62]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data[63]} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe21]
set_property port_width 64 [get_debug_ports u_ila_0/probe21]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency* ]
connect_debug_port u_ila_0/probe21 [ get_nets [list {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[0]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[1]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[2]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[3]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[4]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[5]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[6]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[7]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[8]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[9]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[10]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[11]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[12]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[13]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[14]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[15]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[16]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[17]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[18]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[19]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[20]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[21]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[22]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[23]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[24]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[25]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[26]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[27]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[28]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[29]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[30]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[31]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[32]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[33]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[34]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[35]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[36]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[37]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[38]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[39]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[40]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[41]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[42]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[43]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[44]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[45]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[46]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[47]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[48]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[49]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[50]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[51]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[52]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[53]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[54]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[55]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[56]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[57]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[58]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[59]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[60]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[61]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[62]} {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency[63]} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe22]
set_property port_width 1 [get_debug_ports u_ila_0/probe22]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_valid* ]
connect_debug_port u_ila_0/probe22 [ get_nets [list {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_valid} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe23]
set_property port_width 1 [get_debug_ports u_ila_0/probe23]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_ready* ]
connect_debug_port u_ila_0/probe23 [ get_nets [list {PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_ready} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe24]
set_property port_width 4 [get_debug_ports u_ila_0/probe24]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/bus/arbiter/io_chosen* ]
connect_debug_port u_ila_0/probe24 [ get_nets [list {PipelinedCoreGroupAXI/memory/bus/arbiter/io_chosen[0]} {PipelinedCoreGroupAXI/memory/bus/arbiter/io_chosen[1]} {PipelinedCoreGroupAXI/memory/bus/arbiter/io_chosen[2]} {PipelinedCoreGroupAXI/memory/bus/arbiter/io_chosen[3]} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe25]
set_property port_width 1 [get_debug_ports u_ila_0/probe25]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/bus/arbiter/ResponseValid0* ]
connect_debug_port u_ila_0/probe25 [ get_nets [list {PipelinedCoreGroupAXI/memory/bus/arbiter/ResponseValid0} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe26]
set_property port_width 1 [get_debug_ports u_ila_0/probe26]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/bus/arbiter/ResponseValid1* ]
connect_debug_port u_ila_0/probe26 [ get_nets [list {PipelinedCoreGroupAXI/memory/bus/arbiter/ResponseValid1} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe27]
set_property port_width 1 [get_debug_ports u_ila_0/probe27]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/bus/arbiter/ResponseValid2* ]
connect_debug_port u_ila_0/probe27 [ get_nets [list {PipelinedCoreGroupAXI/memory/bus/arbiter/ResponseValid2} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe28]
set_property port_width 1 [get_debug_ports u_ila_0/probe28]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/bus/arbiter/ResponseValid3* ]
connect_debug_port u_ila_0/probe28 [ get_nets [list {PipelinedCoreGroupAXI/memory/bus/arbiter/ResponseValid3} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe29]
set_property port_width 1 [get_debug_ports u_ila_0/probe29]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/bus/arbiter/ResponseValid4* ]
connect_debug_port u_ila_0/probe29 [ get_nets [list {PipelinedCoreGroupAXI/memory/bus/arbiter/ResponseValid4} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe30]
set_property port_width 1 [get_debug_ports u_ila_0/probe30]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/bus/arbiter/ResponseValid5* ]
connect_debug_port u_ila_0/probe30 [ get_nets [list {PipelinedCoreGroupAXI/memory/bus/arbiter/ResponseValid5} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe31]
set_property port_width 1 [get_debug_ports u_ila_0/probe31]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/bus/arbiter/ResponseValid6* ]
connect_debug_port u_ila_0/probe31 [ get_nets [list {PipelinedCoreGroupAXI/memory/bus/arbiter/ResponseValid6} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe32]
set_property port_width 1 [get_debug_ports u_ila_0/probe32]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/bus/arbiter/ResponseValid7* ]
connect_debug_port u_ila_0/probe32 [ get_nets [list {PipelinedCoreGroupAXI/memory/bus/arbiter/ResponseValid7} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe33]
set_property port_width 1 [get_debug_ports u_ila_0/probe33]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/bus/arbiter/ResponseValid8* ]
connect_debug_port u_ila_0/probe33 [ get_nets [list {PipelinedCoreGroupAXI/memory/bus/arbiter/ResponseValid8} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe34]
set_property port_width 1 [get_debug_ports u_ila_0/probe34]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/bus/arbiter/validMaster_0* ]
connect_debug_port u_ila_0/probe34 [ get_nets [list {PipelinedCoreGroupAXI/memory/bus/arbiter/validMaster_0} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe35]
set_property port_width 1 [get_debug_ports u_ila_0/probe35]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/bus/arbiter/validMaster_1* ]
connect_debug_port u_ila_0/probe35 [ get_nets [list {PipelinedCoreGroupAXI/memory/bus/arbiter/validMaster_1} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe36]
set_property port_width 1 [get_debug_ports u_ila_0/probe36]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/bus/arbiter/validMaster_2* ]
connect_debug_port u_ila_0/probe36 [ get_nets [list {PipelinedCoreGroupAXI/memory/bus/arbiter/validMaster_2} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe37]
set_property port_width 1 [get_debug_ports u_ila_0/probe37]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/bus/arbiter/validMaster_3* ]
connect_debug_port u_ila_0/probe37 [ get_nets [list {PipelinedCoreGroupAXI/memory/bus/arbiter/validMaster_3} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe38]
set_property port_width 1 [get_debug_ports u_ila_0/probe38]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/bus/arbiter/validMaster_4* ]
connect_debug_port u_ila_0/probe38 [ get_nets [list {PipelinedCoreGroupAXI/memory/bus/arbiter/validMaster_4} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe39]
set_property port_width 1 [get_debug_ports u_ila_0/probe39]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/bus/arbiter/validMaster_5* ]
connect_debug_port u_ila_0/probe39 [ get_nets [list {PipelinedCoreGroupAXI/memory/bus/arbiter/validMaster_5} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe40]
set_property port_width 1 [get_debug_ports u_ila_0/probe40]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/bus/arbiter/validMaster_6* ]
connect_debug_port u_ila_0/probe40 [ get_nets [list {PipelinedCoreGroupAXI/memory/bus/arbiter/validMaster_6} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe41]
set_property port_width 1 [get_debug_ports u_ila_0/probe41]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/bus/arbiter/validMaster_7* ]
connect_debug_port u_ila_0/probe41 [ get_nets [list {PipelinedCoreGroupAXI/memory/bus/arbiter/validMaster_7} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe42]
set_property port_width 1 [get_debug_ports u_ila_0/probe42]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/bus/arbiter/validMaster_8* ]
connect_debug_port u_ila_0/probe42 [ get_nets [list {PipelinedCoreGroupAXI/memory/bus/arbiter/validMaster_8} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe43]
set_property port_width 1 [get_debug_ports u_ila_0/probe43]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/bus/arbiter/queued_0* ]
connect_debug_port u_ila_0/probe43 [ get_nets [list {PipelinedCoreGroupAXI/memory/bus/arbiter/queued_0} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe44]
set_property port_width 1 [get_debug_ports u_ila_0/probe44]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/bus/arbiter/queued_1* ]
connect_debug_port u_ila_0/probe44 [ get_nets [list {PipelinedCoreGroupAXI/memory/bus/arbiter/queued_1} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe45]
set_property port_width 1 [get_debug_ports u_ila_0/probe45]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/bus/arbiter/queued_2* ]
connect_debug_port u_ila_0/probe45 [ get_nets [list {PipelinedCoreGroupAXI/memory/bus/arbiter/queued_2} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe46]
set_property port_width 1 [get_debug_ports u_ila_0/probe46]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/bus/arbiter/queued_3* ]
connect_debug_port u_ila_0/probe46 [ get_nets [list {PipelinedCoreGroupAXI/memory/bus/arbiter/queued_3} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe47]
set_property port_width 1 [get_debug_ports u_ila_0/probe47]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/bus/arbiter/queued_4* ]
connect_debug_port u_ila_0/probe47 [ get_nets [list {PipelinedCoreGroupAXI/memory/bus/arbiter/queued_4} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe48]
set_property port_width 1 [get_debug_ports u_ila_0/probe48]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/bus/arbiter/queued_5* ]
connect_debug_port u_ila_0/probe48 [ get_nets [list {PipelinedCoreGroupAXI/memory/bus/arbiter/queued_5} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe49]
set_property port_width 1 [get_debug_ports u_ila_0/probe49]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/bus/arbiter/queued_6* ]
connect_debug_port u_ila_0/probe49 [ get_nets [list {PipelinedCoreGroupAXI/memory/bus/arbiter/queued_6} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe50]
set_property port_width 1 [get_debug_ports u_ila_0/probe50]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/bus/arbiter/queued_7* ]
connect_debug_port u_ila_0/probe50 [ get_nets [list {PipelinedCoreGroupAXI/memory/bus/arbiter/queued_7} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe51]
set_property port_width 1 [get_debug_ports u_ila_0/probe51]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/bus/arbiter/queued_8* ]
connect_debug_port u_ila_0/probe51 [ get_nets [list {PipelinedCoreGroupAXI/memory/bus/arbiter/queued_8} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe52]
set_property port_width 4 [get_debug_ports u_ila_0/probe52]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/bus/arbiter/slotRRCounter* ]
connect_debug_port u_ila_0/probe52 [ get_nets [list {PipelinedCoreGroupAXI/memory/bus/arbiter/slotRRCounter[0]} {PipelinedCoreGroupAXI/memory/bus/arbiter/slotRRCounter[1]} {PipelinedCoreGroupAXI/memory/bus/arbiter/slotRRCounter[2]} {PipelinedCoreGroupAXI/memory/bus/arbiter/slotRRCounter[3]} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe53]
set_property port_width 1 [get_debug_ports u_ila_0/probe53]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/bus/arbiter/slotQueue_io_enq_ready* ]
connect_debug_port u_ila_0/probe53 [ get_nets [list {PipelinedCoreGroupAXI/memory/bus/arbiter/slotQueue_io_enq_ready} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe54]
set_property port_width 1 [get_debug_ports u_ila_0/probe54]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/bus/arbiter/slotQueue_io_enq_valid* ]
connect_debug_port u_ila_0/probe54 [ get_nets [list {PipelinedCoreGroupAXI/memory/bus/arbiter/slotQueue_io_enq_valid} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe55]
set_property port_width 4 [get_debug_ports u_ila_0/probe55]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/bus/arbiter/slotQueue_io_enq_bits* ]
connect_debug_port u_ila_0/probe55 [ get_nets [list {PipelinedCoreGroupAXI/memory/bus/arbiter/slotQueue_io_enq_bits[0]} {PipelinedCoreGroupAXI/memory/bus/arbiter/slotQueue_io_enq_bits[1]} {PipelinedCoreGroupAXI/memory/bus/arbiter/slotQueue_io_enq_bits[2]} {PipelinedCoreGroupAXI/memory/bus/arbiter/slotQueue_io_enq_bits[3]} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe56]
set_property port_width 1 [get_debug_ports u_ila_0/probe56]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/bus/arbiter/slotQueue_io_deq_ready* ]
connect_debug_port u_ila_0/probe56 [ get_nets [list {PipelinedCoreGroupAXI/memory/bus/arbiter/slotQueue_io_deq_ready} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe57]
set_property port_width 1 [get_debug_ports u_ila_0/probe57]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/bus/arbiter/slotQueue_io_deq_valid* ]
connect_debug_port u_ila_0/probe57 [ get_nets [list {PipelinedCoreGroupAXI/memory/bus/arbiter/slotQueue_io_deq_valid} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe58]
set_property port_width 4 [get_debug_ports u_ila_0/probe58]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/bus/arbiter/slotQueue_io_deq_bits* ]
connect_debug_port u_ila_0/probe58 [ get_nets [list {PipelinedCoreGroupAXI/memory/bus/arbiter/slotQueue_io_deq_bits[0]} {PipelinedCoreGroupAXI/memory/bus/arbiter/slotQueue_io_deq_bits[1]} {PipelinedCoreGroupAXI/memory/bus/arbiter/slotQueue_io_deq_bits[2]} {PipelinedCoreGroupAXI/memory/bus/arbiter/slotQueue_io_deq_bits[3]} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe59]
set_property port_width 4 [get_debug_ports u_ila_0/probe59]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/bus/arbiter/slotQueue_io_count* ]
connect_debug_port u_ila_0/probe59 [ get_nets [list {PipelinedCoreGroupAXI/memory/bus/arbiter/slotQueue_io_count[0]} {PipelinedCoreGroupAXI/memory/bus/arbiter/slotQueue_io_count[1]} {PipelinedCoreGroupAXI/memory/bus/arbiter/slotQueue_io_count[2]} {PipelinedCoreGroupAXI/memory/bus/arbiter/slotQueue_io_count[3]} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe60]
set_property port_width 1 [get_debug_ports u_ila_0/probe60]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/bus/arbiter/ownerQueue_io_enq_ready* ]
connect_debug_port u_ila_0/probe60 [ get_nets [list {PipelinedCoreGroupAXI/memory/bus/arbiter/ownerQueue_io_enq_ready} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe61]
set_property port_width 1 [get_debug_ports u_ila_0/probe61]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/bus/arbiter/ownerQueue_io_enq_valid* ]
connect_debug_port u_ila_0/probe61 [ get_nets [list {PipelinedCoreGroupAXI/memory/bus/arbiter/ownerQueue_io_enq_valid} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe62]
set_property port_width 4 [get_debug_ports u_ila_0/probe62]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/bus/arbiter/ownerQueue_io_enq_bits* ]
connect_debug_port u_ila_0/probe62 [ get_nets [list {PipelinedCoreGroupAXI/memory/bus/arbiter/ownerQueue_io_enq_bits[0]} {PipelinedCoreGroupAXI/memory/bus/arbiter/ownerQueue_io_enq_bits[1]} {PipelinedCoreGroupAXI/memory/bus/arbiter/ownerQueue_io_enq_bits[2]} {PipelinedCoreGroupAXI/memory/bus/arbiter/ownerQueue_io_enq_bits[3]} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe63]
set_property port_width 1 [get_debug_ports u_ila_0/probe63]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/bus/arbiter/ownerQueue_io_deq_ready* ]
connect_debug_port u_ila_0/probe63 [ get_nets [list {PipelinedCoreGroupAXI/memory/bus/arbiter/ownerQueue_io_deq_ready} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe64]
set_property port_width 1 [get_debug_ports u_ila_0/probe64]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/bus/arbiter/ownerQueue_io_deq_valid* ]
connect_debug_port u_ila_0/probe64 [ get_nets [list {PipelinedCoreGroupAXI/memory/bus/arbiter/ownerQueue_io_deq_valid} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe65]
set_property port_width 4 [get_debug_ports u_ila_0/probe65]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/bus/arbiter/ownerQueue_io_deq_bits* ]
connect_debug_port u_ila_0/probe65 [ get_nets [list {PipelinedCoreGroupAXI/memory/bus/arbiter/ownerQueue_io_deq_bits[0]} {PipelinedCoreGroupAXI/memory/bus/arbiter/ownerQueue_io_deq_bits[1]} {PipelinedCoreGroupAXI/memory/bus/arbiter/ownerQueue_io_deq_bits[2]} {PipelinedCoreGroupAXI/memory/bus/arbiter/ownerQueue_io_deq_bits[3]} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe66]
set_property port_width 4 [get_debug_ports u_ila_0/probe66]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/bus/arbiter/ownerQueue_io_count* ]
connect_debug_port u_ila_0/probe66 [ get_nets [list {PipelinedCoreGroupAXI/memory/bus/arbiter/ownerQueue_io_count[0]} {PipelinedCoreGroupAXI/memory/bus/arbiter/ownerQueue_io_count[1]} {PipelinedCoreGroupAXI/memory/bus/arbiter/ownerQueue_io_count[2]} {PipelinedCoreGroupAXI/memory/bus/arbiter/ownerQueue_io_count[3]} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe67]
set_property port_width 1 [get_debug_ports u_ila_0/probe67]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/bus/arbiter/slotOwnerWrapWire* ]
connect_debug_port u_ila_0/probe67 [ get_nets [list {PipelinedCoreGroupAXI/memory/bus/arbiter/slotOwnerWrapWire} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe68]
set_property port_width 4 [get_debug_ports u_ila_0/probe68]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/bus/arbiter/slotOwnerWire* ]
connect_debug_port u_ila_0/probe68 [ get_nets [list {PipelinedCoreGroupAXI/memory/bus/arbiter/slotOwnerWire[0]} {PipelinedCoreGroupAXI/memory/bus/arbiter/slotOwnerWire[1]} {PipelinedCoreGroupAXI/memory/bus/arbiter/slotOwnerWire[2]} {PipelinedCoreGroupAXI/memory/bus/arbiter/slotOwnerWire[3]} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe69]
set_property port_width 1 [get_debug_ports u_ila_0/probe69]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/bus/arbiter/assertDeq* ]
connect_debug_port u_ila_0/probe69 [ get_nets [list {PipelinedCoreGroupAXI/memory/bus/arbiter/assertDeq} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe70]
set_property port_width 64 [get_debug_ports u_ila_0/probe70]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address* ]
connect_debug_port u_ila_0/probe70 [ get_nets [list {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[0]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[1]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[2]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[3]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[4]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[5]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[6]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[7]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[8]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[9]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[10]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[11]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[12]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[13]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[14]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[15]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[16]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[17]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[18]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[19]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[20]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[21]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[22]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[23]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[24]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[25]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[26]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[27]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[28]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[29]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[30]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[31]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[32]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[33]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[34]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[35]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[36]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[37]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[38]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[39]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[40]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[41]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[42]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[43]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[44]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[45]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[46]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[47]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[48]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[49]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[50]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[51]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[52]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[53]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[54]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[55]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[56]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[57]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[58]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[59]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[60]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[61]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[62]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address[63]} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe71]
set_property port_width 5 [get_debug_ports u_ila_0/probe71]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_req_type* ]
connect_debug_port u_ila_0/probe71 [ get_nets [list {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_req_type[0]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_req_type[1]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_req_type[2]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_req_type[3]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_req_type[4]} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe72]
set_property port_width 3 [get_debug_ports u_ila_0/probe72]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_requester_id* ]
connect_debug_port u_ila_0/probe72 [ get_nets [list {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_requester_id[0]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_requester_id[1]} {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_requester_id[2]} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe73]
set_property port_width 1 [get_debug_ports u_ila_0/probe73]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_valid* ]
connect_debug_port u_ila_0/probe73 [ get_nets [list {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_valid} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe74]
set_property port_width 1 [get_debug_ports u_ila_0/probe74]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_ready* ]
connect_debug_port u_ila_0/probe74 [ get_nets [list {PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_ready} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe75]
set_property port_width 64 [get_debug_ports u_ila_0/probe75]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address* ]
connect_debug_port u_ila_0/probe75 [ get_nets [list {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[0]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[1]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[2]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[3]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[4]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[5]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[6]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[7]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[8]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[9]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[10]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[11]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[12]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[13]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[14]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[15]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[16]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[17]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[18]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[19]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[20]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[21]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[22]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[23]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[24]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[25]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[26]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[27]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[28]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[29]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[30]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[31]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[32]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[33]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[34]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[35]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[36]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[37]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[38]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[39]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[40]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[41]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[42]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[43]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[44]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[45]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[46]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[47]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[48]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[49]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[50]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[51]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[52]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[53]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[54]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[55]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[56]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[57]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[58]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[59]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[60]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[61]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[62]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address[63]} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe76]
set_property port_width 1 [get_debug_ports u_ila_0/probe76]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/coherenceTable/io_enable* ]
connect_debug_port u_ila_0/probe76 [ get_nets [list {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/coherenceTable/io_enable} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe77]
set_property port_width 4 [get_debug_ports u_ila_0/probe77]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/coherenceTable/io_query_message* ]
connect_debug_port u_ila_0/probe77 [ get_nets [list {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/coherenceTable/io_query_message[0]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/coherenceTable/io_query_message[1]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/coherenceTable/io_query_message[2]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/coherenceTable/io_query_message[3]} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe78]
set_property port_width 5 [get_debug_ports u_ila_0/probe78]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/coherenceTable/io_query_state* ]
connect_debug_port u_ila_0/probe78 [ get_nets [list {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/coherenceTable/io_query_state[0]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/coherenceTable/io_query_state[1]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/coherenceTable/io_query_state[2]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/coherenceTable/io_query_state[3]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/coherenceTable/io_query_state[4]} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe79]
set_property port_width 5 [get_debug_ports u_ila_0/probe79]
set_property MARK_DEBUG TRUE [ get_nets PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/coherenceTable/io_resp_nextState* ]
connect_debug_port u_ila_0/probe79 [ get_nets [list {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/coherenceTable/io_resp_nextState[0]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/coherenceTable/io_resp_nextState[1]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/coherenceTable/io_resp_nextState[2]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/coherenceTable/io_resp_nextState[3]} {PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/coherenceTable/io_resp_nextState[4]} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe80]
set_property port_width 64 [get_debug_ports u_ila_0/probe80]
set_property MARK_DEBUG TRUE [ get_nets Confreg/stats_lat_1* ]
connect_debug_port u_ila_0/probe80 [ get_nets [list {Confreg/stats_lat_1[0]} {Confreg/stats_lat_1[1]} {Confreg/stats_lat_1[2]} {Confreg/stats_lat_1[3]} {Confreg/stats_lat_1[4]} {Confreg/stats_lat_1[5]} {Confreg/stats_lat_1[6]} {Confreg/stats_lat_1[7]} {Confreg/stats_lat_1[8]} {Confreg/stats_lat_1[9]} {Confreg/stats_lat_1[10]} {Confreg/stats_lat_1[11]} {Confreg/stats_lat_1[12]} {Confreg/stats_lat_1[13]} {Confreg/stats_lat_1[14]} {Confreg/stats_lat_1[15]} {Confreg/stats_lat_1[16]} {Confreg/stats_lat_1[17]} {Confreg/stats_lat_1[18]} {Confreg/stats_lat_1[19]} {Confreg/stats_lat_1[20]} {Confreg/stats_lat_1[21]} {Confreg/stats_lat_1[22]} {Confreg/stats_lat_1[23]} {Confreg/stats_lat_1[24]} {Confreg/stats_lat_1[25]} {Confreg/stats_lat_1[26]} {Confreg/stats_lat_1[27]} {Confreg/stats_lat_1[28]} {Confreg/stats_lat_1[29]} {Confreg/stats_lat_1[30]} {Confreg/stats_lat_1[31]} {Confreg/stats_lat_1[32]} {Confreg/stats_lat_1[33]} {Confreg/stats_lat_1[34]} {Confreg/stats_lat_1[35]} {Confreg/stats_lat_1[36]} {Confreg/stats_lat_1[37]} {Confreg/stats_lat_1[38]} {Confreg/stats_lat_1[39]} {Confreg/stats_lat_1[40]} {Confreg/stats_lat_1[41]} {Confreg/stats_lat_1[42]} {Confreg/stats_lat_1[43]} {Confreg/stats_lat_1[44]} {Confreg/stats_lat_1[45]} {Confreg/stats_lat_1[46]} {Confreg/stats_lat_1[47]} {Confreg/stats_lat_1[48]} {Confreg/stats_lat_1[49]} {Confreg/stats_lat_1[50]} {Confreg/stats_lat_1[51]} {Confreg/stats_lat_1[52]} {Confreg/stats_lat_1[53]} {Confreg/stats_lat_1[54]} {Confreg/stats_lat_1[55]} {Confreg/stats_lat_1[56]} {Confreg/stats_lat_1[57]} {Confreg/stats_lat_1[58]} {Confreg/stats_lat_1[59]} {Confreg/stats_lat_1[60]} {Confreg/stats_lat_1[61]} {Confreg/stats_lat_1[62]} {Confreg/stats_lat_1[63]} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe81]
set_property port_width 64 [get_debug_ports u_ila_0/probe81]
set_property MARK_DEBUG TRUE [ get_nets Confreg/stats_lat_3* ]
connect_debug_port u_ila_0/probe81 [ get_nets [list {Confreg/stats_lat_3[0]} {Confreg/stats_lat_3[1]} {Confreg/stats_lat_3[2]} {Confreg/stats_lat_3[3]} {Confreg/stats_lat_3[4]} {Confreg/stats_lat_3[5]} {Confreg/stats_lat_3[6]} {Confreg/stats_lat_3[7]} {Confreg/stats_lat_3[8]} {Confreg/stats_lat_3[9]} {Confreg/stats_lat_3[10]} {Confreg/stats_lat_3[11]} {Confreg/stats_lat_3[12]} {Confreg/stats_lat_3[13]} {Confreg/stats_lat_3[14]} {Confreg/stats_lat_3[15]} {Confreg/stats_lat_3[16]} {Confreg/stats_lat_3[17]} {Confreg/stats_lat_3[18]} {Confreg/stats_lat_3[19]} {Confreg/stats_lat_3[20]} {Confreg/stats_lat_3[21]} {Confreg/stats_lat_3[22]} {Confreg/stats_lat_3[23]} {Confreg/stats_lat_3[24]} {Confreg/stats_lat_3[25]} {Confreg/stats_lat_3[26]} {Confreg/stats_lat_3[27]} {Confreg/stats_lat_3[28]} {Confreg/stats_lat_3[29]} {Confreg/stats_lat_3[30]} {Confreg/stats_lat_3[31]} {Confreg/stats_lat_3[32]} {Confreg/stats_lat_3[33]} {Confreg/stats_lat_3[34]} {Confreg/stats_lat_3[35]} {Confreg/stats_lat_3[36]} {Confreg/stats_lat_3[37]} {Confreg/stats_lat_3[38]} {Confreg/stats_lat_3[39]} {Confreg/stats_lat_3[40]} {Confreg/stats_lat_3[41]} {Confreg/stats_lat_3[42]} {Confreg/stats_lat_3[43]} {Confreg/stats_lat_3[44]} {Confreg/stats_lat_3[45]} {Confreg/stats_lat_3[46]} {Confreg/stats_lat_3[47]} {Confreg/stats_lat_3[48]} {Confreg/stats_lat_3[49]} {Confreg/stats_lat_3[50]} {Confreg/stats_lat_3[51]} {Confreg/stats_lat_3[52]} {Confreg/stats_lat_3[53]} {Confreg/stats_lat_3[54]} {Confreg/stats_lat_3[55]} {Confreg/stats_lat_3[56]} {Confreg/stats_lat_3[57]} {Confreg/stats_lat_3[58]} {Confreg/stats_lat_3[59]} {Confreg/stats_lat_3[60]} {Confreg/stats_lat_3[61]} {Confreg/stats_lat_3[62]} {Confreg/stats_lat_3[63]} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe82]
set_property port_width 64 [get_debug_ports u_ila_0/probe82]
set_property MARK_DEBUG TRUE [ get_nets Confreg/stats_lat_5* ]
connect_debug_port u_ila_0/probe82 [ get_nets [list {Confreg/stats_lat_5[0]} {Confreg/stats_lat_5[1]} {Confreg/stats_lat_5[2]} {Confreg/stats_lat_5[3]} {Confreg/stats_lat_5[4]} {Confreg/stats_lat_5[5]} {Confreg/stats_lat_5[6]} {Confreg/stats_lat_5[7]} {Confreg/stats_lat_5[8]} {Confreg/stats_lat_5[9]} {Confreg/stats_lat_5[10]} {Confreg/stats_lat_5[11]} {Confreg/stats_lat_5[12]} {Confreg/stats_lat_5[13]} {Confreg/stats_lat_5[14]} {Confreg/stats_lat_5[15]} {Confreg/stats_lat_5[16]} {Confreg/stats_lat_5[17]} {Confreg/stats_lat_5[18]} {Confreg/stats_lat_5[19]} {Confreg/stats_lat_5[20]} {Confreg/stats_lat_5[21]} {Confreg/stats_lat_5[22]} {Confreg/stats_lat_5[23]} {Confreg/stats_lat_5[24]} {Confreg/stats_lat_5[25]} {Confreg/stats_lat_5[26]} {Confreg/stats_lat_5[27]} {Confreg/stats_lat_5[28]} {Confreg/stats_lat_5[29]} {Confreg/stats_lat_5[30]} {Confreg/stats_lat_5[31]} {Confreg/stats_lat_5[32]} {Confreg/stats_lat_5[33]} {Confreg/stats_lat_5[34]} {Confreg/stats_lat_5[35]} {Confreg/stats_lat_5[36]} {Confreg/stats_lat_5[37]} {Confreg/stats_lat_5[38]} {Confreg/stats_lat_5[39]} {Confreg/stats_lat_5[40]} {Confreg/stats_lat_5[41]} {Confreg/stats_lat_5[42]} {Confreg/stats_lat_5[43]} {Confreg/stats_lat_5[44]} {Confreg/stats_lat_5[45]} {Confreg/stats_lat_5[46]} {Confreg/stats_lat_5[47]} {Confreg/stats_lat_5[48]} {Confreg/stats_lat_5[49]} {Confreg/stats_lat_5[50]} {Confreg/stats_lat_5[51]} {Confreg/stats_lat_5[52]} {Confreg/stats_lat_5[53]} {Confreg/stats_lat_5[54]} {Confreg/stats_lat_5[55]} {Confreg/stats_lat_5[56]} {Confreg/stats_lat_5[57]} {Confreg/stats_lat_5[58]} {Confreg/stats_lat_5[59]} {Confreg/stats_lat_5[60]} {Confreg/stats_lat_5[61]} {Confreg/stats_lat_5[62]} {Confreg/stats_lat_5[63]} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe83]
set_property port_width 64 [get_debug_ports u_ila_0/probe83]
set_property MARK_DEBUG TRUE [ get_nets Confreg/stats_lat_7* ]
connect_debug_port u_ila_0/probe83 [ get_nets [list {Confreg/stats_lat_7[0]} {Confreg/stats_lat_7[1]} {Confreg/stats_lat_7[2]} {Confreg/stats_lat_7[3]} {Confreg/stats_lat_7[4]} {Confreg/stats_lat_7[5]} {Confreg/stats_lat_7[6]} {Confreg/stats_lat_7[7]} {Confreg/stats_lat_7[8]} {Confreg/stats_lat_7[9]} {Confreg/stats_lat_7[10]} {Confreg/stats_lat_7[11]} {Confreg/stats_lat_7[12]} {Confreg/stats_lat_7[13]} {Confreg/stats_lat_7[14]} {Confreg/stats_lat_7[15]} {Confreg/stats_lat_7[16]} {Confreg/stats_lat_7[17]} {Confreg/stats_lat_7[18]} {Confreg/stats_lat_7[19]} {Confreg/stats_lat_7[20]} {Confreg/stats_lat_7[21]} {Confreg/stats_lat_7[22]} {Confreg/stats_lat_7[23]} {Confreg/stats_lat_7[24]} {Confreg/stats_lat_7[25]} {Confreg/stats_lat_7[26]} {Confreg/stats_lat_7[27]} {Confreg/stats_lat_7[28]} {Confreg/stats_lat_7[29]} {Confreg/stats_lat_7[30]} {Confreg/stats_lat_7[31]} {Confreg/stats_lat_7[32]} {Confreg/stats_lat_7[33]} {Confreg/stats_lat_7[34]} {Confreg/stats_lat_7[35]} {Confreg/stats_lat_7[36]} {Confreg/stats_lat_7[37]} {Confreg/stats_lat_7[38]} {Confreg/stats_lat_7[39]} {Confreg/stats_lat_7[40]} {Confreg/stats_lat_7[41]} {Confreg/stats_lat_7[42]} {Confreg/stats_lat_7[43]} {Confreg/stats_lat_7[44]} {Confreg/stats_lat_7[45]} {Confreg/stats_lat_7[46]} {Confreg/stats_lat_7[47]} {Confreg/stats_lat_7[48]} {Confreg/stats_lat_7[49]} {Confreg/stats_lat_7[50]} {Confreg/stats_lat_7[51]} {Confreg/stats_lat_7[52]} {Confreg/stats_lat_7[53]} {Confreg/stats_lat_7[54]} {Confreg/stats_lat_7[55]} {Confreg/stats_lat_7[56]} {Confreg/stats_lat_7[57]} {Confreg/stats_lat_7[58]} {Confreg/stats_lat_7[59]} {Confreg/stats_lat_7[60]} {Confreg/stats_lat_7[61]} {Confreg/stats_lat_7[62]} {Confreg/stats_lat_7[63]} ] ]
create_debug_port u_ila_0 probe

set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe84]
set_property port_width 64 [get_debug_ports u_ila_0/probe84]
set_property MARK_DEBUG TRUE [ get_nets Confreg/stats_lat_8* ]
connect_debug_port u_ila_0/probe84 [ get_nets [list {Confreg/stats_lat_8[0]} {Confreg/stats_lat_8[1]} {Confreg/stats_lat_8[2]} {Confreg/stats_lat_8[3]} {Confreg/stats_lat_8[4]} {Confreg/stats_lat_8[5]} {Confreg/stats_lat_8[6]} {Confreg/stats_lat_8[7]} {Confreg/stats_lat_8[8]} {Confreg/stats_lat_8[9]} {Confreg/stats_lat_8[10]} {Confreg/stats_lat_8[11]} {Confreg/stats_lat_8[12]} {Confreg/stats_lat_8[13]} {Confreg/stats_lat_8[14]} {Confreg/stats_lat_8[15]} {Confreg/stats_lat_8[16]} {Confreg/stats_lat_8[17]} {Confreg/stats_lat_8[18]} {Confreg/stats_lat_8[19]} {Confreg/stats_lat_8[20]} {Confreg/stats_lat_8[21]} {Confreg/stats_lat_8[22]} {Confreg/stats_lat_8[23]} {Confreg/stats_lat_8[24]} {Confreg/stats_lat_8[25]} {Confreg/stats_lat_8[26]} {Confreg/stats_lat_8[27]} {Confreg/stats_lat_8[28]} {Confreg/stats_lat_8[29]} {Confreg/stats_lat_8[30]} {Confreg/stats_lat_8[31]} {Confreg/stats_lat_8[32]} {Confreg/stats_lat_8[33]} {Confreg/stats_lat_8[34]} {Confreg/stats_lat_8[35]} {Confreg/stats_lat_8[36]} {Confreg/stats_lat_8[37]} {Confreg/stats_lat_8[38]} {Confreg/stats_lat_8[39]} {Confreg/stats_lat_8[40]} {Confreg/stats_lat_8[41]} {Confreg/stats_lat_8[42]} {Confreg/stats_lat_8[43]} {Confreg/stats_lat_8[44]} {Confreg/stats_lat_8[45]} {Confreg/stats_lat_8[46]} {Confreg/stats_lat_8[47]} {Confreg/stats_lat_8[48]} {Confreg/stats_lat_8[49]} {Confreg/stats_lat_8[50]} {Confreg/stats_lat_8[51]} {Confreg/stats_lat_8[52]} {Confreg/stats_lat_8[53]} {Confreg/stats_lat_8[54]} {Confreg/stats_lat_8[55]} {Confreg/stats_lat_8[56]} {Confreg/stats_lat_8[57]} {Confreg/stats_lat_8[58]} {Confreg/stats_lat_8[59]} {Confreg/stats_lat_8[60]} {Confreg/stats_lat_8[61]} {Confreg/stats_lat_8[62]} {Confreg/stats_lat_8[63]} ] ]


# Something for the debug hub
# when you create u_ila_0, the dbg_hub is created as well
# these parts should be constant
set_property C_CLK_INPUT_FREQ_HZ 100000000 [get_debug_cores dbg_hub]
set_property C_ENABLE_CLK_DIVIDER true [get_debug_cores dbg_hub]
set_property C_USER_SCAN_CHAIN 1 [get_debug_cores dbg_hub]
connect_debug_port dbg_hub/clk [get_nets sh_root_clk]


