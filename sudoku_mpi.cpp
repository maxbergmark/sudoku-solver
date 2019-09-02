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

std::vector<std::string> process_batch(
	std::vector<std::vector<signed char>> &boards, 
	int n, int world_rank, int size) {

	omp_set_num_threads(n);
	std::vector<Sudoku> solvers(n);
	for (Sudoku &solver : solvers) {
		solver.connect();
	}
	std::vector<std::string> res;
	res.resize(boards.size());
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
			fprintf(stderr, "Completed sudoku %d / %lu (%d total) on %d\n", 
				i, boards.size(), size, world_rank);
		}
		if (t1-t0 > max_time) {
			#pragma omp critical
			{
				max_time = t1-t0;
				max_index = i;
			}
		}
	}

	std::cerr << "Hardest board on " << world_rank <<": " 
		<< Sudoku::printTime(0, max_time*1e9) 
		<< " for board " << max_index << std::endl;
	Sudoku::display(boards[max_index], std::cerr);

	int easySolved = 0, totalSolved = 0, guesses = 0;
	for (Sudoku &solver : solvers) {
		easySolved += solver.easySolved;
		totalSolved += solver.totalSolved;
		guesses += solver.guesses;
	}
	fprintf(stderr, "Easily solved: %d / %d\tGuesses/board: %.2f\n", 
		easySolved, totalSolved, (double) guesses / totalSolved);
	return res;	
}

int get_scatter_buffer_size(int size, int world_rank, int world_size) {
	return (81 + 1) * (size/world_size + (world_rank < size % world_size));
}

int get_gather_buffer_size(int size, int world_rank, int world_size) {
	return (81*2 + 2) * (size/world_size + (world_rank < size % world_size));
}

std::vector<std::vector<signed char>> divide_work(
	std::string filename, int world_rank, int world_size, int &size) {

	int own_size;
	int send_counts[world_size], displs[world_size];
	char *boards, *recvbuf;
	if (world_rank == 0) {
		boards = Sudoku::getInputChars(filename, size);
		displs[0] = 0;
		for (int i = 0; i < world_size; i++) {
			send_counts[i] = get_scatter_buffer_size(size, i, world_size);
			if (i > 0) {
				displs[i] = displs[i-1] + send_counts[i-1];
			}
		}
	} 
	MPI_Bcast(&size, 1, MPI_INT, 0, MPI_COMM_WORLD);
	own_size = get_scatter_buffer_size(size, world_rank, world_size);
	recvbuf = (char*) malloc(own_size * sizeof(char));

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
	free(recvbuf);

	return batch;
}

void collect_work(int world_rank, int world_size, 
	std::vector<std::string>& res, int size) {


	int buf_size = get_gather_buffer_size(
		size, world_rank, world_size);
	MPI_Barrier(MPI_COMM_WORLD);
	char *buf = (char*) malloc((buf_size+1) * sizeof(char));
	char *recvbuf;
	int recv_counts[world_size], displs[world_size];
	int recv_count;

	for (int i = 0; i < res.size(); i++) {
		sprintf(&buf[(81*2+2)*i], "%163s\n", res[i].c_str());
	}

	if (world_rank == 0) {
		recv_count = 0;
		displs[0] = 0;
		for (int i = 0; i < world_size; i++) {
			recv_counts[i] = get_gather_buffer_size(size, i, world_size);
			recv_count += recv_counts[i];
			if (i > 0) {
				displs[i] = displs[i-1] + recv_counts[i-1];
			}
		}
		recvbuf = (char*) malloc((recv_count + 1) * sizeof(char));
	}

	MPI_Gatherv(buf, buf_size, MPI_CHAR,
        recvbuf, recv_counts, displs, MPI_CHAR,
        0, MPI_COMM_WORLD);

	if (world_rank == 0) {
		printf("%d\n", recv_count / 164);
		printf("%s", recvbuf);
		free(recvbuf);
	}
	free(buf);

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
	int size;
	std::vector<std::vector<signed char>> batch = divide_work(
		filename, world_rank, world_size, size);
	std::vector<std::string> res = process_batch(batch, n, world_rank, size);
	collect_work(world_rank, world_size, res, size);

	MPI_Finalize();

}