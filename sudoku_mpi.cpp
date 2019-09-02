#include <fstream>
#include <iostream>
#include <bits/stdc++.h> 
#include <omp.h>
#include <mpi.h>
#include <sys/time.h>
#include <algorithm>
#include <unistd.h>

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

	if (world_rank == 0) {
		std::vector<std::string> ret;
		return ret;
	}

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

	int easySolved = 0, totalSolved = 0, guesses = 0;
	for (Sudoku &solver : solvers) {
		easySolved += solver.easySolved;
		totalSolved += solver.totalSolved;
		guesses += solver.guesses;
	}
	std::cerr << "Hardest board on " << world_rank <<": " 
		<< Sudoku::printTime(0, max_time*1e9) 
		<< " for board " << max_index << std::endl;
	fprintf(stderr, "Easily solved: %d / %d\tGuesses/board: %.2f\n", 
		easySolved, totalSolved, (double) guesses / totalSolved);
	Sudoku::display(boards[max_index], std::cerr);
	return res;	
}

int get_scatter_buffer_size(int size, int world_rank, int world_size) {
	if (world_rank == 0) {
		return 0;
	}
	return (81 + 1) * (size/(world_size - 1) 
		+ (world_rank - 1 < size % (world_size - 1)));
}

int get_gather_buffer_size(int size, int world_rank, int world_size) {
	if (world_rank == 0) {
		return 0;
	}
	return (81*2 + 2) * (size/(world_size - 1) 
		+ (world_rank - 1 < size % (world_size - 1)));
}

std::vector<std::vector<signed char>> transform_buffer(char* buffer, int size) {

	std::vector<std::vector<signed char>> batch(
		size / 82, std::vector<signed char>(81));

	for (int i = 0; i < size / 82; i++) {
		for (int j = 0; j < 81; j++) {
			batch[i][j] = buffer[82*i + j] - 49;
		}
	}
	// free(buffer);
	return batch;
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

	std::vector<std::vector<signed char>> batch = transform_buffer(recvbuf, own_size);

	free(boards);
	return batch;
}

void collect_work(int world_rank, int world_size, 
	std::vector<std::string>& res, int size) {

	int buf_size = get_gather_buffer_size(
		size, world_rank, world_size);
	char *buf = (char*) malloc((buf_size+1) * sizeof(char));
	char *recvbuf;
	int recv_counts[world_size], displs[world_size];
	int recv_count;

	for (int i = 0; i < res.size(); i++) {
		sprintf(&buf[(81*2+2)*i], "%s", res[i].c_str());
		buf[164 * (i+1) - 1] = '\n';
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

void process_work(int world_rank, int n, int world_size, int batch_size) {
	int recv_size;
	char* buf = (char*) malloc((82 * batch_size + 1) * sizeof(char));
	char* send_buf = (char*) malloc((164 * batch_size + 1) * sizeof(char));
	send_buf[164 * batch_size] = '\0';
	do {
		MPI_Recv(&recv_size, 1, MPI_INT,
			0, 0, MPI_COMM_WORLD, MPI_STATUS_IGNORE);
		// fprintf(stderr, "Processing %d on rank %d\n", recv_size * 82, world_rank);
		if (recv_size > 0) {
			MPI_Recv(buf, recv_size * 82, MPI_CHAR,
				0, 1, MPI_COMM_WORLD, MPI_STATUS_IGNORE);

			std::vector<std::vector<signed char>> batch = transform_buffer(buf, recv_size * 82);
			std::vector<std::string> res = process_batch(batch, n, world_rank, batch_size);
			for (int i = 0; i < res.size(); i++) {
				sprintf(&send_buf[(81*2+2)*i], "%s", res[i].c_str());
				send_buf[164 * (i+1) - 1] = '\n';
			}
			// fprintf(stderr, "%s", send_buf);

			// fprintf(stderr, "Sending %d from rank %d\n", recv_size, world_rank);
			MPI_Send(send_buf, recv_size * 164, MPI_CHAR, 0,
				1, MPI_COMM_WORLD);
		}

		fprintf(stderr, "recv_size: %d from rank %d\n", recv_size, world_rank);
	} while (recv_size > 0);

	fprintf(stderr, "%d done!\n", world_rank);
}

void manage_work(int world_rank, int world_size, std::string filename, int n, int batch_size) {
	int size;
	char *sudokus = Sudoku::getInputChars(filename, size);
	char *output = (char*) malloc((164 * size + 1) * sizeof(char));
	output[164*size] = '\0';
	// int batch_size = 1000;
	int size_left = size;
	int batch_number = 0;
	int recv_index = 0, send_index = 0;
	// std::cerr << "size_left: " << size_left << std::endl;
	MPI_Request size_requests[world_size - 1];
	MPI_Request batch_requests[world_size - 1];
	MPI_Request recv_requests[world_size - 1];
	int send_sizes[world_size];
	int batch_numbers[world_size];

	if (size < batch_size) {
		fprintf(stderr, "running task on master\n");
		std::vector<std::vector<signed char>> batch = transform_buffer(sudokus, 82*size);
		process_batch(batch, n, world_rank, size);
		int tmp = 0;
		for (int i = 1; i < world_size; i++) {
			MPI_Isend(&tmp, 1, MPI_INT, i,
				0, MPI_COMM_WORLD, &size_requests[i-1]);
		}
		return;
	}


	int completed_workers = 0;
	bool worker_started[world_size];
	bool worker_done[world_size];
	for(int i = 0; i < world_size; i++) {
		worker_started[i] = false;
		worker_done[i] = false;
	}

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
					// fprintf(stderr, "Sending char buffer of size %d to %d (%d / %lu)\n", 
						// send_sizes[i] * 82, i, send_index, strlen(sudokus));
					MPI_Isend(&sudokus[send_index], send_sizes[i] * 82, MPI_CHAR, i,
						1, MPI_COMM_WORLD, &batch_requests[i-1]);

					// fprintf(stderr, "Waiting for %d to index %d from %d (%d)\n", 164 * send_sizes[i], recv_index, i, size_left);
					MPI_Irecv(&output[recv_index], 164 * send_sizes[i], MPI_CHAR,
						i, 1, MPI_COMM_WORLD, &recv_requests[i-1]);
					recv_index += 164 * send_sizes[i];
					send_index += 82 * send_sizes[i];
				} else if (!worker_done[i])  {
					// fprintf(stderr, "worker done: %d\n", i);
					completed_workers++;
					worker_done[i] = true;
				}
			} else {
				// fprintf(stderr, "check failed: %d\n", check);
			}
		}
		usleep(1000);
	}
	fprintf(stderr, "manager completed!\n");
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
	int batch_size = 10000;
	// int size;
	// std::vector<std::vector<signed char>> batch = divide_work(
		// filename, world_rank, world_size, size);
	if (world_rank != 0) {
		process_work(world_rank, n, world_size, batch_size);
		// std::vector<std::string> res = process_batch(batch, n, world_rank, size);
		// collect_work(world_rank, world_size, res, size);
	} else {
		manage_work(world_rank, world_size, filename, n, batch_size);
	}

	MPI_Finalize();

}