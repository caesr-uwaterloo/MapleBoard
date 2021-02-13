# this xdc file creates the clock for the shell

# 300mhz system clock
create_clock -name sysclk300mhz -add -period 3.333 [get_ports sys_clk_clk_p[0]]

set_property IOSTANDARD LVDS [ get_ports {sys_clk_clk_p[0]} ]
set_property IOSTANDARD LVDS [ get_ports {sys_clk_clk_n[0]} ]
set_property PACKAGE_PIN AY37 [ get_ports {sys_clk_clk_p[0]} ]
# set_property PACKAGE_PIN AY38 [ get_ports {sys_clk_clk_n[0]} ]

