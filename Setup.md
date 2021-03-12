# Setup guide

This documentation guides you through the setup process of using this repository

## Using IntelliJ

### Install IntelliJ

Download and install IntelliJ and ensure that you can launch IntelliJ.

#### Install scala plugin

The community edition of IntelliJ does not come with the scala plugin, however, to use the project in IntelliJ, you will need the plugin for tools such as sbt.
[This link](https://www.jetbrains.com/help/idea/discover-intellij-idea-for-scala.html#scala_plugin) provides guide on installing the scala plugin.

When the scala plugin is installed, you need to re-start the IDE so changes can take place.

### Install Verilator

Follow [this link](https://www.veripool.org/projects/verilator/wiki/Installing) to install verilator v4.030

### Clone the repository
Clone this repo with `git clone git@github.com:caesr-uwaterloo/MapleBoard.git`

### Setup repository in IntelliJ 

#### Setup chisel-iotesters
We use a specialized version of chisel-iotesters2 which supports Bundle with Enum for testing.
This repository resides in `code/chisel-testers2`.

Follow [this link](https://www.jetbrains.com/help/idea/discover-intellij-idea-for-scala.html#scala_plugin) to open the project, in which you should select the `code/chisel-testers2/` folder.
It may takes serveral minutes for the project to setup.

After the project is loaded into IntelliJ, you should observe an sbt shell tab at the bottom of the IDE.
Execute the following command in the sbt shell and you should observe the output ends with `[success]`.
```
> publishLocal
...
[success] Total time: 38 s, completed Mar 10, 2021, 4:51:20 PM
```

#### Setup the RISC-V project
We developed an in-order RISC-V core with a minimal support for `ecall`. 
This repository resides in `code/chisel-testers2`.

Follow [this link](https://www.jetbrains.com/help/idea/discover-intellij-idea-for-scala.html#scala_plugin) to open the project, in which you should select the `code/riscv-new/` folder.
Similarly, it may takes several minutes for the project to setup.

Execute the following command in the sbt shell and you should observe the output ends with `[success]`.
```
> publishLocal
...
[success] Total time: 61 s (01:01), completed Mar 10, 2021, 7:33:23 PM
```

#### Setup the coherence project
The main repository that we use resides in `code/coherence`.

Follow [this link](https://www.jetbrains.com/help/idea/discover-intellij-idea-for-scala.html#scala_plugin) to open the project, in which you should select the `code/coherence/` folder.
It may takes several minutes for the project to setup.

For now, the project should be setup and an sbt shell should show up at the bottom of the IDE, you should be able to run the following command in the sbt shell.
Note that it may take several minutes to finish.

```
> test:testOnly coherences.PipelinedPMSIReadWriteSpec -- -l Deprecated -oF
...
[success] Total time: 158 s (02:38), completed Mar 12, 2021, 11:36:53 AM
```

Now, the project should be ready for development.


#### Generating the top design

To generate a top design that can be synthesized on board, execute the following command in the sbt shell

```
> runMain utils.VU9PTop --core 2 --line 32 --protocol pmesi --bus dedicated
```

After the command is finished, a `VU9PTop.v` will be generated in the `pmesi.2c.32.dedicated` folder.

The `VU9PTop.v` is the top design file that is passed to the synthesis tool flow.


