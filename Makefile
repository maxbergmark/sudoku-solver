all:
	mpic++ -O3 sudoku_mpi.cpp sudoku.cpp -fopenmp -o sudoku_mpi.out -g -D VERBOSE
clean:
	rm *.out
