#include <iostream>
#include <omp.h>

#include "sudoku.h"


int main(int argc, char **argv) {
	int n = 1;
	if (argc == 3) {
		n = std::atoi(argv[2]);
	} else {
		std::cout << "Usage: ./solver.out <path-to-data-set> <num_threads>\n";
		return 0;
	}
	omp_set_num_threads(n);
	std::vector<Sudoku> solvers(n);
	std::vector<std::vector<signed char>> boards = Sudoku::getInput(argv[1]);
	for (Sudoku &solver : solvers) {
		solver.connect();
	}
	std::vector<std::string> res;
	res.resize(boards.size());
	std::cout << boards.size() << std::endl;
	#pragma omp parallel for schedule(dynamic, 5000)
	for (long unsigned int i = 0; i < boards.size(); i++) {
		int tid = omp_get_thread_num();
		solvers[tid].solveSudoku(boards[i]);
		res[i] = solvers[tid].getSolution();
	}
	for (Sudoku &solver : solvers) {
		fprintf(stderr, "easily solved: %d / %d\n", 
			solver.easySolved, solver.totalSolved);
		fprintf(stderr, "guesses/board: %.2f\n", 
			(double) solver.guesses / solver.totalSolved);
	}
	for (std::string s : res) {
		std::cout << s << std::endl;
	}
}
