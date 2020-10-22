# MPI Sudoku solver

<p align="center">
	<img src="/examples/solved.png" width="80%" />
	<br>
	<font size="2">
		Example solve for a difficult Sudoku puzzle, solved in 60ms. 
	</font>
</p>


A Sudoku solver project which started back in 2015. Since then it's been highly optimized, and solves most sudokus within a few microseconds or milliseconds.

## Compile and run

To compile and run the non-MPI solver, use:

	make standard
	./solver.out <path-to-data-set> <num-threads>
	Example: ./solver.out data-sets/all_17_clue_sudokus.txt 8

To compile and run the MPI solver, use:

	make mpi
	mpirun --bind-to none -n <number-of-nodes> -f hostfile ./sudoku_mpi.out <path-to-data-set> <num-threads-per-node>

Here, `hostfile` should describe the network locations of all the nodes in your cluster. You should be familiar with running MPI within a cluster before attempting this. To handle the work balancing, the root node is tasked with handling the work balancing, and all other nodes request batches of 50000 puzzles from the root node. When all puzzles in a batch are solved, the worker node returns the result and requests another batch. If no more batches are available, the root node communicates this to the workers, who then exit their process. 

By default, the solver runs in verbose mode, where each node reports the most difficult puzzle (ranked by solve time) of each batch. This can be disabled by setting the `VERBOSE` flag to 0 in `sudoku_mpi.cpp`. 

The input format for this solver is a file on the format

    <num-puzzles>
    <puzzle-1>
    <puzzle-2>
    ...
    <puzzle-n>
    
where each puzzle consists of 81 characters, describing the puzzle in row-major order. Each known character is printed as is (e.g. `'1'`to denote a `1`), and each unknown character is represented by a `'0'`. When the solver has completed all puzzles, the solutions are printed to STDOUT, which can then be piped to a file or checksum for verification. All debug output and verbose output is printed to STDERR. 
