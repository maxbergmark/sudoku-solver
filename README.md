# MPI Sudoku solver

	<figure class="image">
<p align="center">
		<img src="/examples/solved.png" width="80%" />
		<font size="2">
			<figcaption>
				Example solve for a difficult Sudoku puzzle
			</figcaption>
		</font>
</p>
	</figure>


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
