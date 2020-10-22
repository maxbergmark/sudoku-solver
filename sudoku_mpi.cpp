#include <fstream>
#include <iostream>
#include <bits/stdc++.h>
#include <omp.h>
#include <mpi.h>
#include <sys/time.h>
#include <algorithm>
#include <unistd.h>

#include "sudoku.h"

#define VERBOSE 1

double get_wall_time() {
	struct timeval time;
	if (gettimeofday(&time,NULL)){
		return 0;
	}
	return (double)time.tv_sec + (double)time.tv_usec * 1e-6;
}

void process_batch(
	std::vector<std::vector<signed char>> &boards,
	int n, int world_rank, int size, std::vector<std::string> &res) {

	omp_set_num_threads(n);
	std::vector<Sudoku> solvers(n);
	for (Sudoku &solver : solvers) {
		solver.connect();
	}
	// std::vector<std::string> res;
	res.resize(boards.size());
	double max_time = 0;
	int max_index;

	#pragma omp parallel for schedule(dynamic, 1000)
	for (long unsigned int i = 0; i < boards.size(); i++) {
		int tid = omp_get_thread_num();
		double t0 = get_wall_time();
		solvers[tid].solveSudoku(boards[i]);
		double t1 = get_wall_time();
		res[i] = solvers[tid].getSolution();
		if (i > 0 && i % 10000 == 0 && VERBOSE) {
			fprintf(stderr, "Completed sudoku %lu / %lu (%d total) on %d\n",
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

	int easySolved = 0, totalSolved = 0, guesses = 0;
	for (Sudoku &solver : solvers) {
		easySolved += solver.easySolved;
		totalSolved += solver.totalSolved;
		guesses += solver.guesses;
	}
	if (VERBOSE) {
		std::cerr << "Hardest board on " << world_rank <<": "
			<< Sudoku::printTime(0, max_time*1e9)
			<< " for board " << max_index << std::endl;
		fprintf(stderr, "Easily solved: %d / %d\tGuesses/board: %.2f\n",
			easySolved, totalSolved, (double) guesses / totalSolved);
		std::cerr << "Solution size: " << res.size() << std::endl;

		std::vector<signed char> max_solved(
			res[max_index].begin()+82, res[max_index].end());

		for (long unsigned int i = 0; i < max_solved.size(); i++) {
			max_solved[i] -= 49;
		}
		Sudoku::display2(boards[max_index], max_solved, std::cerr);

	}
}

void transform_buffer(char* buffer, int size, 
	std::vector<std::vector<signed char>> &batch) {

	batch.resize(size / 82, std::vector<signed char>(81));
	#pragma omp parallel for schedule(static)
	for (int i = 0; i < size / 82; i++) {
		for (int j = 0; j < 81; j++) {
			batch[i][j] = buffer[82*i + j] - 49;
		}
	}
}

void process_work(int world_rank, int n, int world_size) {
	int recv_size, batch_size;
	MPI_Bcast(&batch_size, 1, MPI_INT, 0, MPI_COMM_WORLD);
	char* buf = (char*) malloc((82 * batch_size + 1) * sizeof(char));
	char* send_buf = (char*) malloc((164 * batch_size + 1) * sizeof(char));
	send_buf[164 * batch_size] = '\0';
	// double t0, t1 = get_wall_time();
	std::vector<std::vector<signed char>> batch;
	std::vector<std::string> res;

	do {
		MPI_Recv(&recv_size, 1, MPI_INT,
			0, 0, MPI_COMM_WORLD, MPI_STATUS_IGNORE);
		if (recv_size > 0) {
			MPI_Recv(buf, recv_size * 82, MPI_CHAR,
				0, 1, MPI_COMM_WORLD, MPI_STATUS_IGNORE);
			// t0 = get_wall_time();
			// fprintf(stderr, "communicate: %.3f\n", 1e3*(t0-t1));
			transform_buffer(buf, recv_size * 82, batch);
			process_batch(batch, n, world_rank, batch_size, res);
			#pragma omp parallel for schedule(static)
			for (long unsigned int i = 0; i < res.size(); i++) {
				sprintf(&send_buf[(81*2+2)*i], "%s", res[i].c_str());
				send_buf[164 * (i+1) - 1] = '\n';
			}

			// t1 = get_wall_time();
			// fprintf(stderr, "crunching:   %.3f\n", 1e3*(t1-t0));
			// fprintf(stderr, "%s", send_buf);

			// fprintf(stderr, "Sending %d from rank %d\n", recv_size, world_rank);
			MPI_Send(send_buf, recv_size * 164, MPI_CHAR, 0,
				1, MPI_COMM_WORLD);
		}

		// fprintf(stderr, "recv_size: %d from rank %d\n", recv_size, world_rank);
	} while (recv_size > 0);

	// fprintf(stderr, "%d done!\n", world_rank);
}

void manage_work(int world_rank, int world_size, std::string filename, int n) {

	int size;
	int batch_size = 50000;
	char *sudokus = Sudoku::getInputChars(filename, size);
	char *output = (char*) malloc((164 * size + 1) * sizeof(char));
	output[164*size] = '\0';

	if (batch_size * (world_size - 1) > size) {
		batch_size = size / (world_size - 1) + 1;
	}

	MPI_Bcast(&batch_size, 1, MPI_INT, 0, MPI_COMM_WORLD);

	int size_left = size;
	int batch_number = 0;
	int recv_index = 0, send_index = 0;
	int batches = (size + batch_size - 1) / batch_size;

	MPI_Request size_requests[world_size - 1];
	MPI_Request batch_requests[world_size - 1];
	MPI_Request recv_requests[world_size - 1];
	int send_sizes[world_size];

	int completed_workers = 0;
	bool worker_started[world_size];
	bool worker_done[world_size];
	for(int i = 0; i < world_size; i++) {
		worker_started[i] = false;
		worker_done[i] = false;
	}
	double t0 = get_wall_time();

	while (completed_workers < world_size - 1) {

		for (int i = 1; i < world_size; i++) {
			int check = 0;
			if (worker_started[i]) {
				MPI_Test(&recv_requests[i-1], &check, MPI_STATUS_IGNORE);
			}
			// fprintf(stderr, "Found a completed send!\n");
			if (check || !worker_started[i]) {
				worker_started[i] = true;
				int send_size = std::min(batch_size, size_left);
				send_sizes[i] = send_size;
				size_left -= send_size;

				// fprintf(stderr, "Sending %d to %d\n", send_sizes[i], i);
				MPI_Isend(&send_sizes[i], 1, MPI_INT, i,
					0, MPI_COMM_WORLD, &size_requests[i-1]);
				if (send_sizes[i] > 0) {
					batch_number++;
					double t1 = get_wall_time();
					double elapsed = t1 - t0;
					double puzzles_per_second = 0;
					if (batch_number > 1) {
						puzzles_per_second = (
							(batch_number-1) * batch_size) / elapsed;
					}
					fprintf(stderr, "Sending batch %3d / %3d of "
						"size %6d to %2d (%9d / %9lu) (%.2f puzzles/second)\n",
						batch_number, batches, send_sizes[i] * 82, 
						i, send_index, strlen(sudokus), puzzles_per_second);
					MPI_Isend(&sudokus[send_index], send_sizes[i] * 82, 
						MPI_CHAR, i, 1, MPI_COMM_WORLD, &batch_requests[i-1]);

					MPI_Irecv(&output[recv_index], 164 * send_sizes[i], 
						MPI_CHAR, i, 1, MPI_COMM_WORLD, &recv_requests[i-1]);
					recv_index += 164 * send_sizes[i];
					send_index += 82 * send_sizes[i];
				} else if (!worker_done[i])  {
					// fprintf(stderr, "worker done: %d\n", i);
					completed_workers++;
					worker_done[i] = true;
				}
			}
		}
		usleep(1000);
	}
	fprintf(stderr, "Manager completed!\n");
	printf("%d\n", size);
	printf("%s", output);


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

	if (world_rank != 0) {
		process_work(world_rank, n, world_size);
	} else {
		manage_work(world_rank, world_size, filename, n);
	}

	MPI_Finalize();

}
