standard:
	g++ -O3 solver.cpp sudoku.cpp -fopenmp -o solver.out -g -Wall
mpi:
	mpic++ -O3 sudoku_mpi.cpp sudoku.cpp -fopenmp -o sudoku_mpi.out -g -Wall
clean:
	rm *.out