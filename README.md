# aig2qbf

aig2qbf is a tool written in Java for the conversion of [And-Inverter Graphs (AIG)](https://en.wikipedia.org/wiki/And-inverter_graph) to [Quantified Boolean Formulars (QBF)](https://en.wikipedia.org/wiki/True_quantified_Boolean_formula) using Simple Path as reduction method.

## What is AIG?

AIG is a short term for And-Inverter Graphs and it is used to describe or model digital circuits. Each AIG node has two inputs and one output, where the node itself represents an AND component. All edges can have a NOT component, if needed. With that, every combinational circuit can be defined.

## What is AIGER?

[AIGER](http://fmv.jku.at/aiger/FORMAT) is a format to describe And-Inverter Graphs. The [AIGER package](http://fmv.jku.at/aiger) contains tools to modify AIGER graphs. Our implementation of the aig2qbf converter only supports the initial AIGER format. Besides combinational circuits, the format allows to model sequential logic using latches.

The AIGER format defines two different file extensions, namely .aag and .aig. The main difference between these files is that .aag encodes a graph data structure using ASCII characters and .aig encodes a graph data structure in binary format. In general, the binary format has a smaller file size, but is not readable in the contrast to the non binary format.

## What is QBF?

QBF is a short term for Quantified Boolean Formula and represents a family of propositional formulas where every variable is quantified either existentially or universally and all terms are conjuncted like in [Conjunctive normal form (CNF)](http://en.wikipedia.org/wiki/Conjunctive_normal_form) formulas. QBF is the basis for symbolic model checking, which allows to traverse a much bigger state space in comparison to explicit model checking.

## What is qDimacs?

[qDimacs](http://satlive.org/QBFEvaluation/2004/qDimacs.ps) is a format to describe QBF formulas, which can be used as input format for many different SAT solvers. For example, the [DepQBF](http://fmv.jku.at/depqbf/) solver is able to check QBF formulas.

## What is the simple path constraint?

The simple path constraint is an approach described in the paper [“Compressing BMC Encodings with QBF”](http://fmv.jku.at/papers/JussilaBiere-BMC06.pdf) to check the satisfiability of AIG data structures up to a certain length. It follows the idea that each state in the state space should be traversed only once, which allows to narrow down the number of states to be checked significantly. A state basically represents a sequential logic at a particular time frame and is encoded by a bit vector. If a state could be reached again, a formula would be unsatisfiable, because then a short-circuit would be found. Each bit in the bit vector of a state defines whether the state of a particular latch output is true or false. In order to find out whether two states Si and Sj, n² comparisons would be necessary. By using the QBF reformulation of the simple path constraint, the check for uniqueness can be done in O(n).

## How does aig2qbf work?

aig2qbf is a conversion tool for AIGER to QBF files. For that, it implements the the simple path constraint described above. The tool allows different input formats such as .aig or .aag and different output formats such as .qbf. It uses [DepQBF](http://fmv.jku.at/depqbf/) to check the satisfiability of a particular formula.

When aig2qbf is started, it basically tries to parse a graph from an input file. This graph will then be unrolled several times as specified and all branches will be connected to each other using a global OR component. Having this graph, the next step is to apply the simple path constraint to it. This adds a constraint to the graph which allows that each state (= bit vector) must be unique. Finally, it has to be ensured that the output formula has CNF format. For that, Tseitin(http://en.wikipedia.org/wiki/Tseitin-Transformation) encoding will be applied. The resulting formula in QBF format will then be written to a specified output file, which can be checked by DepQBF.

Besides the conversion functionality, aig2qbf is able to visualize the produced graph data structure. This however is only tractable for small graphs.

![alt text](https://github.com/kg6/aig2qbf/raw/master/resources/screen01.png "Visualizing a sample structure with aig2qbf")

## How to setup aig2qbf?

To compile aig2qbf only <code>make</code>, <code>Java</code> and <code>Apache Ant</code> are needed. After installing all requirements, <code>make</code> compiles and generates all needed files into a JAR file to bin/aig2qbf.jar.

```bash
make
java -jar bin/aig2qbf.jar --help
```

For developing and testing <code>JUnit4</code>, <code>Perl</code> and some Perl modules are needed which can be installed by executing the following command.

```bash
sudo cpan File::Basename File::Slurp Getopt::Compact Modern::Perl String::Util Time::HiRes
```

Besides the Java unit tests some external tools can be found in the <code>scripts</code> folder to support testing.

## How can aig2qbf be used?

Some example arguments for aig2qbf:

* Unroll a given circuit and visualize the result
```
java -jar bin/aig2qbf.jar -v -k 3 -vis --input input/basic/toggle-re.aag
```
* Unroll a given circuit for k = 10 steps
```
java -jar bin/aig2qbf.jar -v -k 10 --input input/sequential/ken.flash^11.C.aig
```
* Unroll a given circuit and deactivate the sanity checks for performance improvement
```
java -jar bin/aig2qbf.jar -ns -k 10 --input input/sequential/ken.flash^11.C.aig
```

The following CLI arguments can be used:

```bash
 -h,--help                      Print help.
 -i,--input <FILE/AAG STRING>   The input file.
 -it,--input-type <TYPE>        Overwrite the format type of the input file.
 -lit,--list-input-types        List all supported input type formats.
 -lot,--list-output-types       List all supported output type formats.
 -k,--unroll <INTEGER>          Number of unrolling steps. Default is 1.
 -nr,--no-reduction             Do not reduce the tree.
 -ns,--no-sanity                Do not apply any sanity checks.
 -nt,--no-tseitin               Do not apply Tseitin conversion. The output is not necessarily in CNF.
 -nu,--no-unrolling             No unrolling will be applied. Implies --no-reduction.
 -o,--output <FILE>             The output file.
 -ot,--output-type <TYPE>       Overwrite the format type of the output file.
 -v,--verbose                   Enable verbose output.
 -vis,--visualize               Visualize the parsed graph data structure after all processing steps were applied.
 -vt,--verbose-times            Output execution time of different conversion stages.
```
