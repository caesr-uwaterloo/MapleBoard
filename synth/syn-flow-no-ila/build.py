"""utilities for building python across machine boundaries"""

import ray
import time
import os
import subprocess as sp
import itertools as it

ray.init(address='127.0.0.1:6379', redis_password='5241590000000000')

CORES=[2, 4, 8]
PROTO=['carp']
BUSES=['shared', 'dedicated']
LINES=[16, 32]

@ray.remote(max_calls=3)
def builder(conf):
    c, p, b, l, env = conf
    os.chdir('/home/allen/working/playground/vivado/syn-flow-no-ila')
    res = sp.check_output([f'make -f Makefile.single all CORES={c} PROTO={p} BUS={b} LINES={l}'], shell=True, env=env)
    return res

@ray.remote(max_calls=12)
def report(conf):
    c, p, b, env = conf
    os.chdir('/home/allen/working/playground/vivado/syn-flow-no-ila')
    res = sp.check_output([f'make -f Makefile.single report CORES={c} PROTO={p} BUS={b} LINES={l}'], shell=True, env=env)
    return res


confs = list(it.product(CORES, PROTO, BUSES, LINES, [os.environ]))
res = ray.get([builder.remote(c) for c in confs])
for conf, r in zip(confs, res):
    c, p, b, l, _ = conf
    logfile = f'logs/{p}.{c}.{b}.{l}.log'
    with open(logfile, 'w') as f:
        f.writelines(r.decode('utf-8'))

