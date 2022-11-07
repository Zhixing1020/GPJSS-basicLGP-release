# GPJSS-basicLGP-release
 
This project is developed based on https://github.com/meiyi1986/GPJSS and the pseudo codes in [1]. 

[1] M. Brameier and W. Banzhaf, Linear Genetic Programming. New York, NY: Springer US, 2007.

### What is this repository for? ###

* This is a package for algorithms for automatic rule design in Dynamic Job Shop Scheduling (DJSS) using Linear Genetic Programming (GP). Written by Zhixing Huang.
* The package is based on the Java ECJ package, which is available from https://cs.gmu.edu/~eclab/projects/ecj/.
* Version 1.0.0

### How do I get set up? ###

1. Download the source files in the `src/` folder and the dependencies in the `libraries/` folder.
2. Create a Java Project using `src/` and `libraries/`.
3. The ECJ functions are located at `src/ec/`, and the functions for JSS, tree-based GP are in `src/yimei/jss/`, and linear GP for solving DJSS are in `src/zhixing/jss/cpxInd`. Now you are ready to run a number of different algorithms.
4. Before starting, it is highly recommended to get yourself familiar with the ECJ package, especially the GP part. You can start from the four tutorials located at `src/ec/app/tutorialx` (x = 1, ..., 4). Turorial 4 is about GP for symbolic regression, which is very useful for understanding this project. A more thorough manual can be found in https://cs.gmu.edu/~eclab/projects/ecj/docs/manual/manual.pdf.

### Project structure ###

The main project is located in `/src/zhixing/jss/cpxInd`. It contains the following packages:

* `individual` contains the core classes of LGP individual (mainly in `individual/LGPIndividual.java`). `individual/primitive` contains the core classes of the newly introduced primitives in LGP. `individual/reproduce` contains the core classes of the basic genetic operators of LGP (`LGP2PointCrossoverPipeline.java` for linear crossover, `LGPMacroMutationPipeline.java` for macro mutation (i.e., effmut1~3 in [1]), and `LGPMicroMutationPipeline.java` for micro mutation)

* `jobshop/` contains the core class for representing a job shop and accepting an LGP individual for problem solving.

* `ruleanalysis/` contains the classes for rule analysis, e.g. reading rules from the ECJ result file, testing rules, calculating the program length, depth, number of unique terminals, etc.

* `individualevalulation/` contains the evaluation models for LGP dispatching rules.

* `individualoptimization/` contains the classes for optimization for LGP dispatching rules.

* `simulation/` contains the classes for discrete event simulation for dynamic job shop scheduling.

* `species` contains the classes of overwritting GPSpecies classes.

* `statistics` contains the core classes of recording the behaviours of LGP evolution.

* `parameters` contains the example parameter files of LGP evolution.



### Running experiments ###

**Example 1 (Training a basic linear GP):**

1. Locate the param file `/GPJSS-basicLGP-release/src/zhixing/jss/cpxInd/parameters/simpleLGP-JSS.params`

2. The main class is specified in `ec.Evolve.java`. Run the main class with argument "-file [path of params file]\simpleLGP-JSS.params -p eval.problem.eval-model.sim-models.0.util-level=0.85 -p seed.0=4 -p eval.problem.eval-model.sim-seed=8 -p eval.problem.eval-model.sim-models.0.num-machines=10".
By this means, most of the parameters are specified by "simpleLGP-JSS.params", while some specific parameters are defined by "*-p xxx*", the same way as any ECJ applications.

3. Finally you will get two result files `out.stat` and `outtabular.stat` in the project home directory. 
The format of `outtabular.stat` is
"[Generation index] [Population mean fitness]\t[Best fitness per generation]\t[Best fitness so far]\t[Population mean absolutate program length]\t[Population mean effective program length]\t[Population average effective rate]\t[Absolute program length of the best individual]\t[Effective program length of the best individual]\t[Effective rate of the best individual]\t[running time so far in seconds]"
In the batch running model, you are required to define the name of the results files by yourselves. For example,
"-p stat.file=[home directory]/job.((ARRAY_TASK_ID)).out.stat -p stat.child.0.file=[home directory]/job.((ARRAY_TASK_ID)).outtabular.stat". where ARRAY_TASK_ID is the ID of each run in the batch model.


**Example 2 (Testing the training outputs from a basic linear GP):**

1. The main class is specified in `zhixing.jss.cpxInd.ruleanalysis.RuleTest4LGP.java`. Run the main class with argument "[path of the result files]\ [number of independent runs] dynamic-job-shop [DJSS scenario] [maximum number of registers] [maximum iteration times of loops] [number of objectives] [objective]".
For example,
"[path of the result files]\ 1 dynamic-job-shop missing-0.85-1.5 16 100 1 mean-weighted-tardiness"

2. The example `out.stat` and `outtabular.stat` of basic LGP are attached in the home directory of the project.


### Who do I talk to? ###

* Email: zhixing.huang@ecs.vuw.ac.nz
