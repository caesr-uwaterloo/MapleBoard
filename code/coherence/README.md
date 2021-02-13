[![Build Status](http://caesr5:8080/job/test/job/cc/badge/icon)](http://caesr5:8080/job/test/job/master/)

# The scala version of snoopy coherence

## Get Started

### Running tests

To run the unit test for HashTable, in `sbt` shell:

```
testOnly components.HashTableTester
```

To run the firrtl repl for debugging, in `sbt` shell:
```
test:runMain components.HashTableRepl
```

### Exporting Verilog Code

To generate Verilog for a module, in `sbt` shell:
```
runMain components.HashTableRTL
```

## Package information

- `components` include RTL modules used in the coherence framework.


## TODOs

## Done modules

- HashTable
- Bus


## Caveats ☣
### OrderedDecoupledHWIOTester Probelm
Note the `OrderedDecoupledHWIOTester` only checks for the content/order of the responses.
And it will not check whether the number of responses matches the `outputEvent`.
So for the last responses, if they are not present, the tests will still past.

### getWidth problem
For declaring bundles outside of a chisel context, the `getWidth` might not return the correct value.
Currently, the workaround is to replace `getWidth` with a `getWidthM`.
This could be a problem in future updates.
[Github Issue](https://github.com/freechipsproject/chisel-testers/issues/231)

### Running tests with external modules
When running tests with external modules, we might need to refer to verilog design.
The tests that need to execute verilog code should be using verilog backend.
Otherwise, there will be errors similar to: `WARNING: external module was not matched with an implementation` and the output of the module will be all-zeros.

### Updating Macros
When using `sbt`, one needs to call the `clean` command after editing the macros for them to take effect.
Also, when a macro does not seem to work, it might be worthwhile to printout the generated tree in the macro project.

### Estimating Hardware costs
When using Vivado, remember to select `-rebuild none` option to get accurate hardware cost for each component.
Otherwise, one module may be absorbed by another module and this results in mis-leading results.

## Useful resources

- [📃Chisel Tester Cheatsheet](https://chisel.eecs.berkeley.edu/doc/chisel-testercheatsheet.pdf)
- [How to *run* Chisel](https://github.com/ucb-bar/chisel3-wiki/blob/master/Running-Stuff.md), 
what does those `Driver`s mean?
- [Chisel3 Repo](https://github.com/freechipsproject/chisel3)
- [📝Rocket Chip Note](https://github.com/cnrv/rocket-chip-read)
- Quasiquotes
- scalameta

## Misc
- The discussion of the use of a new parallel tester on [Github](https://github.com/freechipsproject/chisel3/issues/725)
Time permitting, we should look at it.
- When tring to build the code in IntelliJ IDEA, there might be errors saying `ClassNotFound`.
One way to solve this problem is to invalidate the cache. (`File`-`Invalidate Caches / Restart ...`)
