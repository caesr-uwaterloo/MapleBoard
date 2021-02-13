"""
This script generates the xdc file for debugging

Note that all the wires to debug must be present in the synthesized design

Currently, the debug hub clock and the ila clock should be the same and should be free-running
Not sure if other setup works correctly.
"""

# ---- This part is the only part that needs to be changed ----
nets_clk = 'sh_root_clk'
nets_clk_mhz = '100'
nets_to_debug = [ 
        ("Confreg/reg_initPC", 64) , 
        ("Confreg/reg_resetReg", 64),
        ("Confreg/reg_st", 2),
        # Core 0 Interfaces
        ("PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_bits_address", 64),
        ("PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_valid", 1),
        ("PipelinedCoreGroupAXI/memory/cache/io_core_request_channel_ready", 1),
        ("PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_data", 64),
        ("PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_bits_latency", 64),
        ("PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_valid", 1),
        ("PipelinedCoreGroupAXI/memory/cache/io_core_response_channel_ready", 1),
        # Core Messages
        # ("PipelinedCoreGroupAXI/memory/cache_1/tag_check/coherenceTable/io_enable", 1),
        # ("PipelinedCoreGroupAXI/memory/cache_1/tag_check/coherenceTable/io_query_message", 5),
        # ("PipelinedCoreGroupAXI/memory/cache_1/tag_check/coherenceTable/io_query_state", 5),
        # ("PipelinedCoreGroupAXI/memory/cache_1/tag_check/coherenceTable/io_resp_nextState", 5),
        # ("PipelinedCoreGroupAXI/memory/cache_1/tag_check/io_pipe_in_bits_address", 64),
        # ("PipelinedCoreGroupAXI/memory/cache_1/tag_check/io_pipe_in_valid", 1),
        ("PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_bits_address", 64),
        ("PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_valid", 1),
        ("PipelinedCoreGroupAXI/memory/cache_1/io_core_request_channel_ready", 1),
        ("PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_data", 64),
        ("PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_bits_latency", 64),
        ("PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_valid", 1),
        ("PipelinedCoreGroupAXI/memory/cache_1/io_core_response_channel_ready", 1),


        # ("PipelinedCoreGroupAXI/memory/cache_1/writeback/pendingWriteback/io_enq_valid", 1),
        # ("PipelinedCoreGroupAXI/memory/cache_1/writeback/pendingWriteback/io_deq_valid", 1),
        # ("PipelinedCoreGroupAXI/memory/cache_1/writeback/pendingWriteback/io_deq_ready", 1),
        # ("PipelinedCoreGroupAXI/memory/cache_1/writeback/pendingWriteback/io_deq_bits_address", 64),
        # ("PipelinedCoreGroupAXI/memory/cache_1/writeback/pendingWriteback/io_count", 5),
        # ("PipelinedCoreGroupAXI/memory/cache_1/writeback/io_bus_request_channel_valid", 1),
        # ("PipelinedCoreGroupAXI/memory/cache_1/writeback/io_bus_request_channel_ready", 1),
        # ("PipelinedCoreGroupAXI/memory/cache_1/tag_check/coherenceTable/io_resp_broadcastWB", 1),
        # # instruction flow
        # # Core 1
        # ("PipelinedCoreGroupAXI/memory/cache_3/tag_check/coherenceTable/io_enable", 1),
        # ("PipelinedCoreGroupAXI/memory/cache_3/tag_check/coherenceTable/io_query_message", 5),
        # ("PipelinedCoreGroupAXI/memory/cache_3/tag_check/coherenceTable/io_query_state", 5),
        # ("PipelinedCoreGroupAXI/memory/cache_3/tag_check/coherenceTable/io_resp_nextState", 5),
        # ("PipelinedCoreGroupAXI/memory/cache_3/tag_check/io_pipe_in_bits_address", 64),
        # ("PipelinedCoreGroupAXI/memory/cache_3/tag_check/io_pipe_in_valid", 1),
        ("PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_bits_address", 64),
        ("PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_valid", 1),
        ("PipelinedCoreGroupAXI/memory/cache_3/io_core_request_channel_ready", 1),
        ("PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_data", 64),
        ("PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_bits_latency", 64),
        ("PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_valid", 1),
        ("PipelinedCoreGroupAXI/memory/cache_3/io_core_response_channel_ready", 1),

        # ("PipelinedCoreGroupAXI/memory/cache_3/writeback/pendingWriteback/io_enq_valid", 1),
        # ("PipelinedCoreGroupAXI/memory/cache_3/writeback/pendingWriteback/io_deq_valid", 1),
        # ("PipelinedCoreGroupAXI/memory/cache_3/writeback/pendingWriteback/io_deq_ready", 1),
        # ("PipelinedCoreGroupAXI/memory/cache_3/writeback/pendingWriteback/io_deq_bits_address", 64),
        # ("PipelinedCoreGroupAXI/memory/cache_3/writeback/pendingWriteback/io_count", 5),
        # ("PipelinedCoreGroupAXI/memory/cache_3/writeback/io_bus_request_channel_valid", 1),
        # ("PipelinedCoreGroupAXI/memory/cache_3/writeback/io_bus_request_channel_ready", 1),
        # # ("PipelinedCoreGroupAXI/memory/cache_3/writeback/rrCounter", 1),
        # ("PipelinedCoreGroupAXI/memory/cache_3/tag_check/coherenceTable/io_resp_broadcastWB", 1),

        # # Core 2
        # ("PipelinedCoreGroupAXI/memory/cache_5/tag_check/coherenceTable/io_enable", 1),
        # ("PipelinedCoreGroupAXI/memory/cache_5/tag_check/coherenceTable/io_query_message", 5),
        # ("PipelinedCoreGroupAXI/memory/cache_5/tag_check/coherenceTable/io_query_state", 5),
        # ("PipelinedCoreGroupAXI/memory/cache_5/tag_check/coherenceTable/io_resp_nextState", 5),
        # ("PipelinedCoreGroupAXI/memory/cache_5/tag_check/io_pipe_in_bits_address", 64),
        # ("PipelinedCoreGroupAXI/memory/cache_5/tag_check/io_pipe_in_valid", 1),
        # ("PipelinedCoreGroupAXI/memory/cache_5/io_core_request_channel_bits_address", 64),
        # ("PipelinedCoreGroupAXI/memory/cache_5/io_core_request_channel_valid", 1),
        # ("PipelinedCoreGroupAXI/memory/cache_5/io_core_request_channel_ready", 1),
        # ("PipelinedCoreGroupAXI/memory/cache_5/io_core_response_channel_bits_data", 64),
        # ("PipelinedCoreGroupAXI/memory/cache_5/io_core_response_channel_bits_latency", 64),
        # ("PipelinedCoreGroupAXI/memory/cache_5/io_core_response_channel_valid", 1),
        # ("PipelinedCoreGroupAXI/memory/cache_5/io_core_response_channel_ready", 1),

        # ("PipelinedCoreGroupAXI/memory/cache_5/writeback/pendingWriteback/io_enq_valid", 1),
        # ("PipelinedCoreGroupAXI/memory/cache_5/writeback/pendingWriteback/io_deq_valid", 1),
        # ("PipelinedCoreGroupAXI/memory/cache_5/writeback/pendingWriteback/io_deq_ready", 1),
        # ("PipelinedCoreGroupAXI/memory/cache_5/writeback/pendingWriteback/io_deq_bits_address", 64),
        # ("PipelinedCoreGroupAXI/memory/cache_5/writeback/pendingWriteback/io_count", 5),
        # ("PipelinedCoreGroupAXI/memory/cache_5/writeback/io_bus_request_channel_valid", 1),
        # ("PipelinedCoreGroupAXI/memory/cache_5/writeback/io_bus_request_channel_ready", 1),
        # # ("PipelinedCoreGroupAXI/memory/cache_5/writeback/rrCounter", 1),
        # ("PipelinedCoreGroupAXI/memory/cache_5/tag_check/coherenceTable/io_resp_broadcastWB", 1),

        # # Core 7
        # ("PipelinedCoreGroupAXI/memory/cache_7/tag_check/coherenceTable/io_enable", 1),
        # ("PipelinedCoreGroupAXI/memory/cache_7/tag_check/coherenceTable/io_query_message", 5),
        # ("PipelinedCoreGroupAXI/memory/cache_7/tag_check/coherenceTable/io_query_state", 5),
        # ("PipelinedCoreGroupAXI/memory/cache_7/tag_check/coherenceTable/io_resp_nextState", 5),
        # ("PipelinedCoreGroupAXI/memory/cache_7/tag_check/io_pipe_in_bits_address", 64),
        # ("PipelinedCoreGroupAXI/memory/cache_7/tag_check/io_pipe_in_valid", 1),
        # ("PipelinedCoreGroupAXI/memory/cache_7/io_core_request_channel_bits_address", 64),
        # ("PipelinedCoreGroupAXI/memory/cache_7/io_core_request_channel_valid", 1),
        # ("PipelinedCoreGroupAXI/memory/cache_7/io_core_request_channel_ready", 1),
        # ("PipelinedCoreGroupAXI/memory/cache_7/io_core_response_channel_bits_data", 64),
        # ("PipelinedCoreGroupAXI/memory/cache_7/io_core_response_channel_bits_latency", 64),
        # ("PipelinedCoreGroupAXI/memory/cache_7/io_core_response_channel_valid", 1),
        # ("PipelinedCoreGroupAXI/memory/cache_7/io_core_response_channel_ready", 1),


        # ("PipelinedCoreGroupAXI/memory/cache_7/writeback/pendingWriteback/io_enq_valid", 1),
        # ("PipelinedCoreGroupAXI/memory/cache_7/writeback/pendingWriteback/io_deq_valid", 1),
        # ("PipelinedCoreGroupAXI/memory/cache_7/writeback/pendingWriteback/io_deq_ready", 1),
        # ("PipelinedCoreGroupAXI/memory/cache_7/writeback/pendingWriteback/io_deq_bits_address", 64),
        # ("PipelinedCoreGroupAXI/memory/cache_7/writeback/pendingWriteback/io_count", 5),
        # ("PipelinedCoreGroupAXI/memory/cache_7/writeback/io_bus_request_channel_valid", 1),
        # ("PipelinedCoreGroupAXI/memory/cache_7/writeback/io_bus_request_channel_ready", 1),
        # # ("PipelinedCoreGroupAXI/memory/cache_7/writeback/rrCounter", 1),
        # ("PipelinedCoreGroupAXI/memory/cache_7/tag_check/coherenceTable/io_resp_broadcastWB", 1),
        # # Core 3
        # ("PipelinedCoreGroupAXI/memory/cache_8/tag_check/coherenceTable/io_enable", 1),
        # ("PipelinedCoreGroupAXI/memory/cache_8/tag_check/coherenceTable/io_query_message", 5),
        # ("PipelinedCoreGroupAXI/memory/cache_8/tag_check/coherenceTable/io_query_state", 5),
        # ("PipelinedCoreGroupAXI/memory/cache_8/tag_check/coherenceTable/io_resp_nextState", 5),
        # ("PipelinedCoreGroupAXI/memory/cache_8/tag_check/io_pipe_in_bits_address", 64),
        # ("PipelinedCoreGroupAXI/memory/cache_8/tag_check/io_pipe_in_valid", 1),
        # ("PipelinedCoreGroupAXI/memory/cache_8/io_core_request_channel_bits_address", 64),
        # ("PipelinedCoreGroupAXI/memory/cache_8/io_core_request_channel_valid", 1),
        # ("PipelinedCoreGroupAXI/memory/cache_8/io_core_request_channel_ready", 1),
        # ("PipelinedCoreGroupAXI/memory/cache_8/io_core_response_channel_bits_data", 64),
        # ("PipelinedCoreGroupAXI/memory/cache_8/io_core_response_channel_bits_latency", 64),
        # ("PipelinedCoreGroupAXI/memory/cache_8/io_core_response_channel_valid", 1),
        # ("PipelinedCoreGroupAXI/memory/cache_8/io_core_response_channel_ready", 1),

        # ("PipelinedCoreGroupAXI/memory/cache_8/writeback/pendingWriteback/io_enq_valid", 1),
        # ("PipelinedCoreGroupAXI/memory/cache_8/writeback/pendingWriteback/io_deq_valid", 1),
        # ("PipelinedCoreGroupAXI/memory/cache_8/writeback/pendingWriteback/io_deq_ready", 1),
        # ("PipelinedCoreGroupAXI/memory/cache_8/writeback/pendingWriteback/io_deq_bits_address", 64),
        # ("PipelinedCoreGroupAXI/memory/cache_8/writeback/pendingWriteback/io_count", 5),
        # ("PipelinedCoreGroupAXI/memory/cache_8/writeback/io_bus_request_channel_valid", 1),
        # ("PipelinedCoreGroupAXI/memory/cache_8/writeback/io_bus_request_channel_ready", 1),
        # # ("PipelinedCoreGroupAXI/memory/cache_8/writeback/rrCounter", 1),
        # ("PipelinedCoreGroupAXI/memory/cache_8/tag_check/coherenceTable/io_resp_broadcastWB", 1),

        # # for Slot
        # ("AXI4ToReq/io_req_bits_address", 64),
        # ("AXI4ToReq/io_req_bits_wstrb", 8),
        # ("AXI4ToReq/io_req_bits_use_wstrb", 1),
        # ("AXI4ToReq/io_req_bits_data", 64),
        # # ("AXI4ToReq/io_req_bits_len", 2),
        # ("AXI4ToReq/io_req_valid", 1),
        # ("AXI4ToReq/io_req_ready", 1),

        # ("AXI4ToReq/io_resp_bits_data", 64),
        # ("AXI4ToReq/io_resp_valid", 1),
        # ("AXI4ToReq/io_resp_ready", 1),

        # # SLOT s_axi
        # # AR
        # ("AXI4ToReq/s_axi_ar_bits_addr", 64),
        # ("AXI4ToReq/s_axi_ar_bits_size", 3),
        # ("AXI4ToReq/s_axi_ar_bits_len", 8),
        # ("AXI4ToReq/s_axi_ar_valid", 1),
        # ("AXI4ToReq/s_axi_ar_ready", 1),
        # # R
        # ("AXI4ToReq/s_axi_r_bits_data", 64),
        # ("AXI4ToReq/s_axi_r_valid", 1),
        # ("AXI4ToReq/s_axi_r_ready", 1),
        # # AW
        # ("AXI4ToReq/s_axi_aw_bits_addr", 64),
        # ("AXI4ToReq/s_axi_aw_bits_size", 3),
        # ("AXI4ToReq/s_axi_aw_bits_len", 8),
        # ("AXI4ToReq/s_axi_aw_valid", 1),
        # ("AXI4ToReq/s_axi_aw_ready", 1),
        # # W
        # ("AXI4ToReq/s_axi_w_bits_data", 64),
        # ("AXI4ToReq/s_axi_w_bits_strb", 8),
        # ("AXI4ToReq/s_axi_w_valid", 1),
        # ("AXI4ToReq/s_axi_w_ready", 1),
        # # B
        # ("AXI4ToReq/s_axi_b_valid", 1),
        # ("AXI4ToReq/s_axi_b_ready", 1),

        # Bus
        ("PipelinedCoreGroupAXI/memory/bus/arbiter/io_chosen", 4),
        ("PipelinedCoreGroupAXI/memory/bus/arbiter/ResponseValid0", 1),
        ("PipelinedCoreGroupAXI/memory/bus/arbiter/ResponseValid1", 1),
        ("PipelinedCoreGroupAXI/memory/bus/arbiter/ResponseValid2", 1),
        ("PipelinedCoreGroupAXI/memory/bus/arbiter/ResponseValid3", 1),
        ("PipelinedCoreGroupAXI/memory/bus/arbiter/ResponseValid4", 1),
        ("PipelinedCoreGroupAXI/memory/bus/arbiter/ResponseValid5", 1),
        ("PipelinedCoreGroupAXI/memory/bus/arbiter/ResponseValid6", 1),
        ("PipelinedCoreGroupAXI/memory/bus/arbiter/ResponseValid7", 1),
        ("PipelinedCoreGroupAXI/memory/bus/arbiter/ResponseValid8", 1),

        ("PipelinedCoreGroupAXI/memory/bus/arbiter/validMaster_0", 1),
        ("PipelinedCoreGroupAXI/memory/bus/arbiter/validMaster_1", 1),
        ("PipelinedCoreGroupAXI/memory/bus/arbiter/validMaster_2", 1),
        ("PipelinedCoreGroupAXI/memory/bus/arbiter/validMaster_3", 1),
        ("PipelinedCoreGroupAXI/memory/bus/arbiter/validMaster_4", 1),
        ("PipelinedCoreGroupAXI/memory/bus/arbiter/validMaster_5", 1),
        ("PipelinedCoreGroupAXI/memory/bus/arbiter/validMaster_6", 1),
        ("PipelinedCoreGroupAXI/memory/bus/arbiter/validMaster_7", 1),
        ("PipelinedCoreGroupAXI/memory/bus/arbiter/validMaster_8", 1),

        ("PipelinedCoreGroupAXI/memory/bus/arbiter/queued_0", 1),
        ("PipelinedCoreGroupAXI/memory/bus/arbiter/queued_1", 1),
        ("PipelinedCoreGroupAXI/memory/bus/arbiter/queued_2", 1),
        ("PipelinedCoreGroupAXI/memory/bus/arbiter/queued_3", 1),
        ("PipelinedCoreGroupAXI/memory/bus/arbiter/queued_4", 1),
        ("PipelinedCoreGroupAXI/memory/bus/arbiter/queued_5", 1),
        ("PipelinedCoreGroupAXI/memory/bus/arbiter/queued_6", 1),
        ("PipelinedCoreGroupAXI/memory/bus/arbiter/queued_7", 1),
        ("PipelinedCoreGroupAXI/memory/bus/arbiter/queued_8", 1),

        ("PipelinedCoreGroupAXI/memory/bus/arbiter/slotRRCounter", 4),

        ("PipelinedCoreGroupAXI/memory/bus/arbiter/slotQueue_io_enq_ready", 1),
        ("PipelinedCoreGroupAXI/memory/bus/arbiter/slotQueue_io_enq_valid", 1),
        ("PipelinedCoreGroupAXI/memory/bus/arbiter/slotQueue_io_enq_bits", 4),
        ("PipelinedCoreGroupAXI/memory/bus/arbiter/slotQueue_io_deq_ready", 1),
        ("PipelinedCoreGroupAXI/memory/bus/arbiter/slotQueue_io_deq_valid", 1),
        ("PipelinedCoreGroupAXI/memory/bus/arbiter/slotQueue_io_deq_bits", 4),
        ("PipelinedCoreGroupAXI/memory/bus/arbiter/slotQueue_io_count", 4),

        ("PipelinedCoreGroupAXI/memory/bus/arbiter/ownerQueue_io_enq_ready", 1),
        ("PipelinedCoreGroupAXI/memory/bus/arbiter/ownerQueue_io_enq_valid", 1),
        ("PipelinedCoreGroupAXI/memory/bus/arbiter/ownerQueue_io_enq_bits", 4),
        ("PipelinedCoreGroupAXI/memory/bus/arbiter/ownerQueue_io_deq_ready", 1),
        ("PipelinedCoreGroupAXI/memory/bus/arbiter/ownerQueue_io_deq_valid", 1),
        ("PipelinedCoreGroupAXI/memory/bus/arbiter/ownerQueue_io_deq_bits", 4),
        ("PipelinedCoreGroupAXI/memory/bus/arbiter/ownerQueue_io_count", 4),

        ("PipelinedCoreGroupAXI/memory/bus/arbiter/slotOwnerWrapWire", 1),
        ("PipelinedCoreGroupAXI/memory/bus/arbiter/slotOwnerWire", 4),
        ("PipelinedCoreGroupAXI/memory/bus/arbiter/assertDeq", 1),

        # # tooLarge
        # # ("PipelinedCoreGroupAXI/tooLarge", 9),

        # # MC

        # # ("PipelinedCoreGroupAXI/mem_to_axi/maxLatency", 64),

        ("PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_address", 64),
        ("PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_req_type", 5),
        ("PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_bits_requester_id", 3),
        ("PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_valid", 1),
        ("PipelinedCoreGroupAXI/memory/memory_controller/frontend/io_bus_request_channel_ready", 1),

        ("PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/address", 64),
        ("PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/coherenceTable/io_enable", 1),
        ("PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/coherenceTable/io_query_message", 4),
        ("PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/coherenceTable/io_query_state", 5),
        ("PipelinedCoreGroupAXI/memory/memory_controller/dramEngine/coherenceTable/io_resp_nextState", 5),

        # ("Confreg/stats_lat_0", 64),
        ("Confreg/stats_lat_1", 64),
        # ("Confreg/stats_lat_2", 64),
        ("Confreg/stats_lat_3", 64),
        # ("Confreg/stats_lat_4", 64),
        ("Confreg/stats_lat_5", 64),
        # ("Confreg/stats_lat_6", 64),
        ("Confreg/stats_lat_7", 64),
        ("Confreg/stats_lat_8", 64),

        # # Interface to the main memory...
        # # ("PipelinedCoreGroupAXI/mem_to_axi4/io_dramReq_bits_address", 64),
        # # ("PipelinedCoreGroupAXI/mem_to_axi4/io_dramReq_bits_mem_type", 1),
        # ("PipelinedCoreGroupAXI/mem_to_axi4/io_dramReq_valid", 1),
        # ("PipelinedCoreGroupAXI/mem_to_axi4/io_dramReq_bits_address", 64),
        # ("PipelinedCoreGroupAXI/mem_to_axi4/io_dramReq_ready", 1),

        # ("PipelinedCoreGroupAXI/mem_to_axi4/io_dramResp_valid", 1),
        # ("PipelinedCoreGroupAXI/mem_to_axi4/io_dramResp_ready", 1),

        # # MC Interface
        # # AR
        # ("PipelinedCoreGroupAXI/mem_to_axi4/m_axi_araddr", 64),
        # ("PipelinedCoreGroupAXI/mem_to_axi4/m_axi_arvalid", 1),
        # ("PipelinedCoreGroupAXI/mem_to_axi4/m_axi_arready", 1),
        # # R
        # ("PipelinedCoreGroupAXI/mem_to_axi4/m_axi_rdata", 64),
        # ("PipelinedCoreGroupAXI/mem_to_axi4/m_axi_rvalid", 1),
        # ("PipelinedCoreGroupAXI/mem_to_axi4/m_axi_rready", 1),
        # # AW
        # ("PipelinedCoreGroupAXI/m_axi_awaddr", 64),
        # ("PipelinedCoreGroupAXI/mem_to_axi4/m_axi_awvalid", 1),
        # ("PipelinedCoreGroupAXI/mem_to_axi4/m_axi_awready", 1),
        # # W
        # ("PipelinedCoreGroupAXI/mem_to_axi4/m_axi_wdata", 64),
        # ("PipelinedCoreGroupAXI/mem_to_axi4/m_axi_wvalid", 1),
        # ("PipelinedCoreGroupAXI/mem_to_axi4/m_axi_wready", 1),
        # # B
        # ("PipelinedCoreGroupAXI/mem_to_axi4/m_axi_bvalid", 1),
        # ("PipelinedCoreGroupAXI/mem_to_axi4/m_axi_bready", 1)

        ]
depth = 16384
pipe_stages = 3
# ---- Generate the ports ----
current_port_id = 0
res = ''
for net_id in nets_to_debug:
    if current_port_id != 0:
        res = res + 'create_debug_port u_ila_0 probe\n'
    if net_id[1] == 1:
        net_list = '{' + net_id[0] + '}'
    else:
        net_list = ' '.join('{' + net_id[0] + f'[{idx}]' + '}' for idx in range(net_id[1]))
    res +=  f"""
set_property PROBE_TYPE DATA_AND_TRIGGER [get_debug_ports u_ila_0/probe{current_port_id}]
set_property port_width {net_id[1]} [get_debug_ports u_ila_0/probe{current_port_id}]
set_property MARK_DEBUG TRUE [ get_nets {net_id[0]}* ]
connect_debug_port u_ila_0/probe{current_port_id} [ get_nets [list {net_list} ] ]
"""

    current_port_id += 1
# ---- ----

template = f"""
# This file is generated automatically by Python script
# create the debug core (and dbg_hub automatically)
create_debug_core u_ila_0 ila

# some fixed parameter that is used in the dialog
set_property ALL_PROBE_SAME_MU true [get_debug_cores u_ila_0]
set_property ALL_PROBE_SAME_MU_CNT 4 [get_debug_cores u_ila_0]
set_property C_ADV_TRIGGER true [get_debug_cores u_ila_0]
set_property C_DATA_DEPTH {depth} [get_debug_cores u_ila_0]
set_property C_EN_STRG_QUAL true [get_debug_cores u_ila_0]
set_property C_INPUT_PIPE_STAGES {pipe_stages} [get_debug_cores u_ila_0]
set_property C_TRIGIN_EN false [get_debug_cores u_ila_0]
set_property C_TRIGOUT_EN false [get_debug_cores u_ila_0]

# Setup the clock for u_ila_0
set_property port_width 1 [get_debug_ports u_ila_0/clk]
connect_debug_port u_ila_0/clk [get_nets [list {nets_clk}]]

{res}

# Something for the debug hub
# when you create u_ila_0, the dbg_hub is created as well
# these parts should be constant
set_property C_CLK_INPUT_FREQ_HZ {nets_clk_mhz}000000 [get_debug_cores dbg_hub]
set_property C_ENABLE_CLK_DIVIDER true [get_debug_cores dbg_hub]
set_property C_USER_SCAN_CHAIN 1 [get_debug_cores dbg_hub]
connect_debug_port dbg_hub/clk [get_nets {nets_clk}]

"""


print(template)
