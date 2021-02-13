# takes into verilog port decl and genearte the Chisel port decl
import re

with open("ports", 'r') as f:
    lines = f.readlines()

reg = r'(?P<inout>input|output)(\s*)((\[(?P<w>\d+):0\])?)(\s*)(?P<name>[a-zA-Z0-9_]+)'
for l in lines:
    search_res = re.search(reg, l)
    if search_res:
        inout = search_res.group('inout')
        width = search_res.group('w')
        name = search_res.group('name')
        if width:
            width = int(width) + 1
        else:
            width = 1
        if inout == 'input':
            print(f'val {name} = Input(UInt({width}.W))')
        elif inout == 'output':
            print(f'val {name} = Output(UInt({width}.W))')
