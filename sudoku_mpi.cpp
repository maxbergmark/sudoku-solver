#include <fstream>
#include <iostream>
#include <bits/stdc++.h> 
#include <omp.h>
#include <mpi.h>
#include <sys/time.h>

#include "sudoku.h"

double get_wall_time() {
	struct timeval time;
	if (gettimeofday(&time,NULL)){
		return 0;
	}
	return (double)time.tv_sec + (double)time.tv_usec * 1e-6;
}

void process_batch(std::vector<std::vector<signed char>> &boards, 
	int n, int world_rank) {

	omp_set_num_threads(n);
	std::vector<Sudoku> solvers(n);
	for (Sudoku &solver : solvers) {
		solver.connect();
	}
	std::vector<std::string> res;
	res.resize(boards.size());
	std::cout << boards.size() << std::endl;
	double max_time = 0;
	int max_index;

	#pragma omp parallel for schedule(dynamic, 1000)
	for (int i = 0; i < boards.size(); i++) {
		int tid = omp_get_thread_num();
		double t0 = get_wall_time();
		solvers[tid].solveSudoku(boards[i]);
		double t1 = get_wall_time();
		res[i] = solvers[tid].getSolution();
		if (i > 0 && i % 10000 == 0) {
			fprintf(stderr, "Completed sudoku %d on %d\n", i, world_rank);
		}
		if (t1-t0 > max_time) {
			#pragma omp critical
			{
				max_time = t1-t0;
				max_index = i;
			}
		}
	}

	std::cerr << "Hardest board: " << Sudoku::printTime(0, max_time*1e9) 
		<< " for board " << max_index << std::endl;
	// fprintf(stderr, "Hardest board: %s for board %d\n", , max_index);
	Sudoku::display(boards[max_index], std::cerr);

	for (Sudoku &solver : solvers) {
		fprintf(stderr, "easily solved: %d / %d\n", 
			solver.easySolved, solver.totalSolved);
		fprintf(stderr, "guesses/board: %.2f\n", 
			(double) solver.guesses / solver.totalSolved);
	}
	for (std::string s : res) {
		// std::cout << s << std::endl;
	}
}

std::vector<std::vector<signed char>> divide_work(
	std::string filename, int world_rank, int world_size) {

	int size, own_size;
	int send_counts[world_size], displs[world_size];
	char *boards, *recvbuf;
	if (world_rank == 0) {
		boards = Sudoku::getInputChars(filename, size);
		displs[0] = 0;
		for (int i = 0; i < world_size; i++) {
			send_counts[i] = 82 * (size/world_size + (i < size % world_size));
			if (i > 0) {
				displs[i] = displs[i-1] + send_counts[i-1];
			}
		}
	} 
	MPI_Bcast(&size, 1, MPI_INT, 0, MPI_COMM_WORLD);
	own_size = 82 * (size/world_size + (world_rank < size % world_size));
	recvbuf = (char*) malloc(own_size * sizeof(char));

	// std::vector<std::vector<signed char>> batch(own_size);

	MPI_Scatterv(boards, send_counts, displs,
		MPI_CHAR, recvbuf, own_size,
		MPI_CHAR, 0, MPI_COMM_WORLD);

	std::vector<std::vector<signed char>> batch(
		own_size / 82, std::vector<signed char>(81));

	for (int i = 0; i < own_size / 82; i++) {
		for (int j = 0; j < 81; j++) {
			batch[i][j] = recvbuf[82*i + j] - 49;
		}
	}

	return batch;
}

int main(int argc, char **argv) {

	MPI_Init(NULL, NULL);
	int world_rank, world_size;
	MPI_Comm_rank(MPI_COMM_WORLD, &world_rank);
	MPI_Comm_size(MPI_COMM_WORLD, &world_size);

	int n = 1;
	if (argc == 3) {
		n = std::atoi(argv[2]);
		if (world_rank == 0) {
			fprintf(stderr, "Running on %d threads\n", n);
		}
	}
	std::string filename = argv[1];
	std::vector<std::vector<signed char>> batch = divide_work(
		filename, world_rank, world_size);
	process_batch(batch, n, world_rank);

	MPI_Finalize();

}