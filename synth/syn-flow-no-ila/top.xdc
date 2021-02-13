# preventing the inference of buffers on pcie ports, these are defined in the shell.dcp
# these should be put into shell...

set_property IO_BUFFER_TYPE NONE [ get_ports sys_clk_p ]
set_property IO_BUFFER_TYPE NONE [ get_ports sys_clk_n ]
set_property IO_BUFFER_TYPE NONE [ get_ports pci_express_x16_rxn ]
set_property IO_BUFFER_TYPE NONE [ get_ports pci_express_x16_rxp ]
set_property IO_BUFFER_TYPE NONE [ get_ports pci_express_x16_txn ]
set_property IO_BUFFER_TYPE NONE [ get_ports pci_express_x16_txp ]
# maybe just this one not done properly?
# set_property IO_BUFFER_TYPE NONE [ get_ports pcie_perstn ]
set_property IO_BUFFER_TYPE NONE [ get_ports pcie_refclk_clk_n ]
set_property IO_BUFFER_TYPE NONE [ get_ports pcie_refclk_clk_p ]

# following UG905, but basically, here we mark the shell as `partition'
# this should not be necessary according to the document...not sure though
set_property HD.PARTITION 1 [get_cells sh]

# Don't touch my confreg module interfaces!
# set_property DONT_TOUCH true [get_cells Confreg]
# set_property DONT_TOUCH true [get_cells PipelinedCoreGroupAXI ]
# set_property DONT_TOUCH true [get_cells PipelinedCoreGroupAXI/cores_0]
# set_property DONT_TOUCH true [get_cells PipelinedCoreGroupAXI/cores_1]
# set_property DONT_TOUCH true [get_cells PipelinedCoreGroupAXI/cores_2]
# set_property DONT_TOUCH true [get_cells PipelinedCoreGroupAXI/cores_3]
# set_property DONT_TOUCH true [get_cells PipelinedCoreGroupAXI/memory ]
# set_property DONT_TOUCH true [get_cells PipelinedCoreGroupAXI/memory/bus]
# set_property DONT_TOUCH true [get_cells PipelinedCoreGroupAXI/memory/bus/arbiter]
# 
# set_property DONT_TOUCH true [get_cells AXI4ToReq ]
# set_property KEEP true [ get_nets -of_objects [get_cells AXI4ToReq ] ]
# 
# set_property DONT_TOUCH true [get_cells PipelinedCoreGroupAXI ]
# set_property DONT_TOUCH true [get_cells PipelinedCoreGroupAXI/memory/cache/tag_check/coherenceTable ]
# set_property DONT_TOUCH true [get_cells PipelinedCoreGroupAXI/memory/cache_1/tag_check/coherenceTable ]
# set_property DONT_TOUCH true [get_cells PipelinedCoreGroupAXI/memory/cache_2/tag_check/coherenceTable ]
# set_property DONT_TOUCH true [get_cells PipelinedCoreGroupAXI/memory/cache_3/tag_check/coherenceTable ]
# set_property DONT_TOUCH true [get_cells PipelinedCoreGroupAXI/memory/cache_4/tag_check/coherenceTable ]
# 
# set_property KEEP true [ get_nets -of_objects [get_cells PipelinedCoreGroupAXI/memory/cache/tag_check/coherenceTable ] ]
# set_property KEEP true [ get_nets -of_objects [get_cells PipelinedCoreGroupAXI/memory/cache_1/tag_check/coherenceTable ] ]
# set_property KEEP true [ get_nets -of_objects [get_cells PipelinedCoreGroupAXI/memory/cache_2/tag_check/coherenceTable ] ]
# set_property KEEP true [ get_nets -of_objects [get_cells PipelinedCoreGroupAXI/memory/cache_3/tag_check/coherenceTable ] ]
# set_property KEEP true [ get_nets -of_objects [get_cells PipelinedCoreGroupAXI/memory/cache_4/tag_check/coherenceTable ] ]
# set_property KEEP true [ get_nets -of_objects [get_cells PipelinedCoreGroupAXI/memory/bus ] ]
# set_property KEEP true [ get_nets -of_objects [get_cells PipelinedCoreGroupAXI/memory/bus/arbiter ] ]
# 
# # dont change the address wires...stupid
# set_property KEEP true [ get_nets -of_objects [get_cells PipelinedCoreGroupAXI/memory/cache/tag_check ] ]
# set_property KEEP true [ get_nets -of_objects [get_cells PipelinedCoreGroupAXI/memory/cache_1/tag_check ] ]
# set_property KEEP true [ get_nets -of_objects [get_cells PipelinedCoreGroupAXI/memory/cache_2/tag_check ] ]
# set_property KEEP true [ get_nets -of_objects [get_cells PipelinedCoreGroupAXI/memory/cache_3/tag_check ] ]
# set_property KEEP true [ get_nets -of_objects [get_cells PipelinedCoreGroupAXI/memory/cache_4/tag_check ] ]
# 
# set_property DONT_TOUCH true [get_cells PipelinedCoreGroupAXI/memory/memory_controller/dramEngine ]
# set_property KEEP true [ get_nets -of_objects [get_cells PipelinedCoreGroupAXI/memory/memory_controller/dramEngine ] ]
# 
# set_property DONT_TOUCH true [get_cells PipelinedCoreGroupAXI/mem_to_axi4 ]
# set_property KEEP true [ get_nets -of_objects [ get_cells PipelinedCoreGroupAXI/mem_to_axi4 ] ]
