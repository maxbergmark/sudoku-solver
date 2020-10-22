#include <fstream>
#include <iostream>
#include <bits/stdc++.h> 
#include <omp.h>

#include "sudoku.h"

Sudoku::Sudoku() {
	mask.resize(81, std::vector<char>(9, 0));
	formattedMask.resize(81, 0);
	placedMask.resize(64, std::vector<char>(64, 0));
	sectionCounters.resize(9, std::vector<char>(27, 0));
	sectionMask.resize(9, std::vector<char>(27, 0));
	neighbors.resize(81, std::vector<char>(20, 0));
	lineMask.resize(64, std::vector<std::vector<bool>>(81, std::vector<bool>(9, 0)));
	lineCounters.resize(64, false);
	unsolvedBoard.resize(81, 0);
	solvedBoard.resize(81, 0);
	tempRowMask.resize(3, std::vector<char>(9, 0));
	tempColMask.resize(3, std::vector<char>(9, 0));
	cells = {{0 ,1 ,2 ,9 ,10,11,18,19,20},
			 {3 ,4 ,5 ,12,13,14,21,22,23},
			 {6 ,7 ,8 ,15,16,17,24,25,26},
			 {27,28,29,36,37,38,45,46,47},
			 {30,31,32,39,40,41,48,49,50},
			 {33,34,35,42,43,44,51,52,53},
			 {54,55,56,63,64,65,72,73,74},
			 {57,58,59,66,67,68,75,76,77},
			 {60,61,62,69,70,71,78,79,80}};
	easySolved = 0;
	totalSolved = 0;
	guesses = 0;
}

void Sudoku::solveSudoku(std::vector<signed char> &board) {
	unsolvedBoard = board;
	solvedBoard = board;

	placedNumbers = 0;
	solutionFound = false;
	isEasy = true;
	isImpossible = false;

	for (std::vector<char> &i : mask) {
		std::fill(i.begin(), i.end(), 0);
	}
	
	for (std::vector<std::vector<bool>> &i : lineMask) {
		for (std::vector<bool> &j : i) {
			std::fill(j.begin(), j.end(), false);
		}
	}

	for (int i = 0; i < 81; i++) {
		if (solvedBoard[i] != -1) {
			put(i, solvedBoard[i]);
			placedNumbers++;
		}
	}

	solve(0, 0);

	if (solutionFound && placedNumbers == 81) {
		easySolved += isEasy ? 1 : 0;
		totalSolved++;
		// display2(board, solvedBoard, std::cerr);
		// totTime += t2-t1;
		// if (shouldPrint || t2-t1 > 5*1_000_000_000L) {
			// printf("Solution found\n");
			// shouldPrint = false;
			// if (t2-t1 > 1*1000_000_000L) {
				// printf("\n");
				// display2(board, solvedBoard);
			// }
		// }
	} else {
		printf("No solution\n");
		display2(unsolvedBoard, solvedBoard, std::cerr);
		return;
	}
}

void Sudoku::solve(int v, int vIndex) {

	lineCounters[vIndex] = false;
	int easyIndex = placeEasy(vIndex);

	if (isImpossible) {
		resetEasy(vIndex, easyIndex);
		resetLineMask(vIndex);
		return;
	}

	if (placedNumbers == 81) {
		solutionFound = true;
		return;
	}

	// get the cell with the fewest options
	generateFormattedMasks();
	int minOptions = 9;
	for (int i = 0; i < 81; i++) {
		int options = formattedMask[i] & 0xffff;
		if (options > 0 && options < minOptions) {
			minOptions = options;
			v = i;
		}
		if (options == 0 && solvedBoard[i] == -1) {
			isImpossible = true;
		}
	}
	if (!isImpossible) {
		for (int c = 0; c < 9; c++) {
			if (isPossible(v, c)) {
				isEasy = false;
				guesses++;
				put(v, c);
				placedNumbers++;
				solve(v + 1, vIndex + 1); 
				if (solutionFound) {
					return;
				}
				unput(v, c);
				placedNumbers--;
			}
		}
	}
	resetEasy(vIndex, easyIndex);
	resetLineMask(vIndex);
}

inline void Sudoku::resetEasy(int vIndex, int easyIndex) {
	for (int i = 0; i < easyIndex; i++) {
		int tempv2 = placedMask[vIndex][i];
		int c2 = solvedBoard[tempv2];
		unput(tempv2, c2);
		placedNumbers--;
	}
}

inline void Sudoku::resetLineMask(int vIndex) {
	if (lineCounters[vIndex]) {
		for (int i = 0; i < 81; i++) {
			for (int c = 0; c < 9; c++) {
				if (lineMask[vIndex][i][c]) {
					enable(i, c);
					lineMask[vIndex][i][c] = false;
				}
			}
		}
	}
	isImpossible = false;
}



int Sudoku::placeEasy(int vIndex) {
	int easyIndex = 0;
	int lastPlaced = 0, tempPlaced = 0;
	int iter = 0;
	if (vIndex == 0 && placedNumbers < 35) {
		checkNakedTriples(vIndex);
	}

	while (placedNumbers > lastPlaced+1) {
		lastPlaced = placedNumbers;
		tempPlaced = 0;
		while (placedNumbers > tempPlaced + 5) {
			tempPlaced = placedNumbers;
			easyIndex = placeNakedSingles(vIndex, easyIndex);
			if (isImpossible) {
				return easyIndex;
			}
		}
		tempPlaced = 0;
		while (placedNumbers < 55*2 && placedNumbers > tempPlaced + 3) {
			tempPlaced = placedNumbers;
			easyIndex = placeHiddenSingles(vIndex, easyIndex);
			if (isImpossible) {
				return easyIndex;
			}
		}


		tempPlaced = 0;
		while (placedNumbers < 65*1 && placedNumbers > tempPlaced + 2) {
			tempPlaced = placedNumbers;
			easyIndex = placeNakedSingles(vIndex, easyIndex);
			if (isImpossible) {
				return easyIndex;
			}
		}

		if (placedNumbers < 45*1) {
			checkNakedDoubles(vIndex);
			identifyLines(vIndex);
		}
		iter++;
	}
	return easyIndex;
}

int Sudoku::placeNakedSingles(int vIndex, int easyIndex) {
	generateFormattedMasks();
	for (int tempv = 0; tempv < 81; tempv++) {
		int possibilities = formattedMask[tempv];
		// printMask(tempv, mask[tempv]);
		if ((possibilities & 0xffff) == 1) {
			possibilities >>= 16;
			int c = 0;
			while ((possibilities & 1) == 0) {
				possibilities >>= 1;
				c++;
			}
			if (isPossible(tempv, c)) {
				// printf("found candidate\n");
				put(tempv, c);
				placedMask[vIndex][easyIndex++] = tempv;
				placedNumbers++;
			} else {
				isImpossible = true;
				return easyIndex;
			}
		} else if (possibilities == 0 && solvedBoard[tempv] == -1) {
			isImpossible = true;
			return easyIndex;
		}
	}
	return easyIndex;
}


int Sudoku::placeHiddenSingles(int vIndex, int easyIndex) {
	for (std::vector<char> &i : sectionCounters) {
		std::fill(i.begin(), i.end(), 0);
	}

	for (int c = 0; c < 9; c++) {
		for (int v = 0; v < 81; v++) {
			if (isPossible(v, c)) {
				int cell = 3 * (v / 27) + ((v / 3) % 3);
				sectionCounters[c][v / 9]++;
				sectionCounters[c][9 + (v % 9)]++;
				sectionCounters[c][18 + cell]++;
				sectionMask[c][v / 9] = v;
				sectionMask[c][9 + (v % 9)] = v;
				sectionMask[c][18 + cell] = v;
			}
		}

		int v;

		for (int i = 0; i < 9; i++) {
			if (sectionCounters[c][i] == 1) {
				v = sectionMask[c][i];
				if (isPossible(v, c)) {
					put(v, c);
					placedMask[vIndex][easyIndex++] = v;
					placedNumbers++;
					int cell = 3 * (v / 27) + ((v / 3) % 3);
					sectionCounters[c][9 + (v%9)] = 9;
					sectionCounters[c][18 + cell] = 9;
				} else {
					isImpossible = true;
					return easyIndex;
				}
			}
		}

		for (int i = 9; i < 18; i++) {
			if (sectionCounters[c][i] == 1) {
				v = sectionMask[c][i];
				if (isPossible(v, c)) {
					put(v, c);
					placedMask[vIndex][easyIndex++] = v;
					int cell = 3 * (v / 27) + ((v / 3) % 3);
					placedNumbers++;
					sectionCounters[c][18 + cell]++;
				} else {
					isImpossible = true;
					return easyIndex;
				}
			}
		}


		for (int i = 18; i < 27; i++) {
			if (sectionCounters[c][i] == 1) {
				v = sectionMask[c][i];
				if (isPossible(v, c)) {
					put(v, c);
					placedMask[vIndex][easyIndex++] = v;
					placedNumbers++;
				} else {
					isImpossible = true;
					return easyIndex;
				}
			}
		}

	}
	return easyIndex;
}

inline int Sudoku::getFormattedMask(int v) {
	if (solvedBoard[v] >= 0) {
		return 0;
	}
	int x = 0;
	int y = 0;
	for (int c = 8; c >= 0; c--) {
		x <<= 1;
		x += mask[v][c] == 0 ? 1 : 0;
		y += mask[v][c] == 0 ? 1 : 0;
	}
	x <<= 16;
	return x + y;
}

int Sudoku::getCachedMask(int v) {
	return formattedMask[v];
}

void Sudoku::generateFormattedMasks() {
	for (int i = 0; i < 81; i++) {
		formattedMask[i] = getFormattedMask(i);
	}
}

void Sudoku::generateFormattedMasks(std::vector<char> &idxs) {
	for (int i : idxs) {
		formattedMask[i] = getFormattedMask(i);
	}
}


void Sudoku::checkNakedDoubles(int vIndex) {
	generateFormattedMasks();
	for (int i = 0; i < 81; i++) {
		int bitmask = formattedMask[i];
		if ((bitmask & 0xffff) == 2) {
			for (int j = i+1; j < (i/9+1)*9; j++) {
				int bitmask_j = formattedMask[j];
				if (bitmask == bitmask_j) {
					bitmask >>= 16;
					int c0, c1, k = 0;
					while ((bitmask & 1) == 0) {
						k++;
						bitmask >>= 1;
					}
					c0 = k;
					bitmask >>= 1;
					k++;
					while ((bitmask & 1) == 0) {
						k++;
						bitmask >>= 1;
					}
					c1 = k;
					for (int cell = (i/9)*9; cell < (i/9+1)*9; cell++) {
						if (cell != i && cell != j) {
							if (!lineMask[vIndex][cell][c0]) {
								disable(cell, c0);
								lineMask[vIndex][cell][c0] = true;
								lineCounters[vIndex] = true;
							}
							if (!lineMask[vIndex][cell][c1]) {
								disable(cell, c1);
								lineMask[vIndex][cell][c1] = true;
								lineCounters[vIndex] = true;
							}
						}
					}
				}
			}
		}
	}

	for (int idx = 0; idx < 81; idx++) {
		int i = (idx%9)*9 + idx/9;
		int bitmask = formattedMask[i];
		if ((bitmask & 0xffff) == 2) {
			for (int j = i+9; j < 81; j += 9) {
				int bitmask_j = formattedMask[j];
				if (bitmask == bitmask_j) {
					bitmask >>= 16;
					int c0, c1, k = 0;
					while ((bitmask & 1) == 0) {
						k++;
						bitmask >>= 1;
					}
					c0 = k;
					bitmask >>= 1;
					k++;
					while ((bitmask & 1) == 0) {
						k++;
						bitmask >>= 1;
					}
					c1 = k;
					for (int cell = i % 9; cell < 81; cell += 9) {
						if (cell != i && cell != j) {
							if (!lineMask[vIndex][cell][c0]) {
								disable(cell, c0);
								lineMask[vIndex][cell][c0] = true;
								lineCounters[vIndex] = true;
							}
							if (!lineMask[vIndex][cell][c1]) {
								disable(cell, c1);
								lineMask[vIndex][cell][c1] = true;
								lineCounters[vIndex] = true;
							}
						}
					}
				}
			}
		}
	}

	for (int idx = 0; idx < 9; idx++) {
		for (int i = 0; i < 9; i++) {
			int bitmask = formattedMask[cells[idx][i]];
			if ((bitmask & 0xffff) == 2) {
				for (int j = i+1; j < 9; j++) {
					int bitmask_j = formattedMask[cells[idx][j]];
					if (bitmask == bitmask_j) {
						bitmask >>= 16;
						int c0, c1, k = 0;
						while ((bitmask & 1) == 0) {
							k++;
							bitmask >>= 1;
						}
						c0 = k;
						bitmask >>= 1;
						k++;
						while ((bitmask & 1) == 0) {
							k++;
							bitmask >>= 1;
						}
						c1 = k;
						for (int cellIdx = 0; cellIdx < 9; cellIdx++) {
							if (cellIdx != i && cellIdx != j) {
								int cell = cells[idx][cellIdx];
								if (!lineMask[vIndex][cell][c0]) {
									disable(cell, c0);
									lineMask[vIndex][cell][c0] = true;
									lineCounters[vIndex] = true;
								}
								if (!lineMask[vIndex][cell][c1]) {
									disable(cell, c1);
									lineMask[vIndex][cell][c1] = true;
									lineCounters[vIndex] = true;
								}
							}
						}
					}
				}
			}
		}
	}
}

void Sudoku::checkNakedTriples(int vIndex) {

	generateFormattedMasks();

	for (int i = 0; i < 81; i++) {
		int bitmask = formattedMask[i];
		if ((bitmask & 0xffff) == 3) {
			for (int j = i+1; j < (i/9+1)*9; j++) {
				int bitmask_j = formattedMask[j];
				if (bitmask_j > 0 && bitmask == (bitmask | bitmask_j)) {
					for (int k = j+1; k < (i/9+1)*9; k++) {
						int bitmask_k = formattedMask[k];
						if (bitmask_k > 0 && bitmask == (bitmask | bitmask_k)) {

							int bitmask_shifted = bitmask >> 16;
							int c0, c1, c2, l = 0;
							while ((bitmask_shifted & 1) == 0) {
								l++;
								bitmask_shifted >>= 1;
							}
							c0 = l;
							bitmask_shifted >>= 1;
							l++;
							while ((bitmask_shifted & 1) == 0) {
								l++;
								bitmask_shifted >>= 1;
							}
							c1 = l;
							bitmask_shifted >>= 1;
							l++;
							while ((bitmask_shifted & 1) == 0) {
								l++;
								bitmask_shifted >>= 1;
							}
							c2 = l;
							for (int cell = (i/9)*9; cell < (i/9+1)*9; cell++) {
								if (cell != i && cell != j && cell != k) {
									if (!lineMask[vIndex][cell][c0]) {
										disable(cell, c0);
										lineMask[vIndex][cell][c0] = true;
										lineCounters[vIndex] = true;
									}
									if (!lineMask[vIndex][cell][c1]) {
										disable(cell, c1);
										lineMask[vIndex][cell][c1] = true;
										lineCounters[vIndex] = true;
									}
									if (!lineMask[vIndex][cell][c2]) {
										disable(cell, c2);
										lineMask[vIndex][cell][c2] = true;
										lineCounters[vIndex] = true;
									}
								}
							}
						}
					}
				}
			}
		}
	}

	for (int idx = 0; idx < 81; idx++) {
		int i = (idx%9)*9 + idx/9;
		int bitmask = formattedMask[i];
		if ((bitmask & 0xffff) == 3) {
			for (int j = i+9; j < 81; j += 9) {
				int bitmask_j = formattedMask[j];
				if (bitmask_j > 0 && bitmask == (bitmask | bitmask_j)) {
					for (int k = j+9; k < 81; k += 9) {
						int bitmask_k = formattedMask[k];
						if (bitmask_k > 0 && bitmask == (bitmask | bitmask_k)) {

							int bitmask_shifted = bitmask >> 16;
							int c0, c1, c2, l = 0;
							while ((bitmask_shifted & 1) == 0) {
								l++;
								bitmask_shifted >>= 1;
							}
							c0 = l;
							bitmask_shifted >>= 1;
							l++;
							while ((bitmask_shifted & 1) == 0) {
								l++;
								bitmask_shifted >>= 1;
							}
							c1 = l;
							bitmask_shifted >>= 1;
							l++;
							while ((bitmask_shifted & 1) == 0) {
								l++;
								bitmask_shifted >>= 1;
							}
							c2 = l;
							for (int cell = i%9; cell < 81; cell += 9) {
								if (cell != i && cell != j && cell != k) {
									if (!lineMask[vIndex][cell][c0]) {
										disable(cell, c0);
										lineMask[vIndex][cell][c0] = true;
										lineCounters[vIndex] = true;
									}
									if (!lineMask[vIndex][cell][c1]) {
										disable(cell, c1);
										lineMask[vIndex][cell][c1] = true;
										lineCounters[vIndex] = true;
									}
									if (!lineMask[vIndex][cell][c2]) {
										disable(cell, c2);
										lineMask[vIndex][cell][c2] = true;
										lineCounters[vIndex] = true;
									}
								}
							}
						}
					}
				}
			}
		}
	}

	for (int idx = 0; idx < 9; idx++) {
		for (int i = 0; i < 9; i++) {
			int bitmask = formattedMask[cells[idx][i]];
			if ((bitmask & 0xffff) == 3) {
				for (int j = i+1; j < 9; j++) {
					int bitmask_j = formattedMask[cells[idx][j]];
					if (bitmask_j > 0 && bitmask == (bitmask | bitmask_j)) {
						for (int k = j+1; k < 9; k++) {
							int bitmask_k = formattedMask[cells[idx][k]];
							if (bitmask_k > 0 && bitmask == (bitmask | bitmask_k)) {

								int bitmask_shifted = bitmask >> 16;
								int c0, c1, c2, l = 0;
								while ((bitmask_shifted & 1) == 0) {
									l++;
									bitmask_shifted >>= 1;
								}
								c0 = l;
								bitmask_shifted >>= 1;
								l++;
								while ((bitmask_shifted & 1) == 0) {
									l++;
									bitmask_shifted >>= 1;
								}
								c1 = l;
								bitmask_shifted >>= 1;
								l++;
								while ((bitmask_shifted & 1) == 0) {
									l++;
									bitmask_shifted >>= 1;
								}
								c2 = l;
								for (int cellIdx = 0; cellIdx < 9; cellIdx++) {
									if (cellIdx != i && cellIdx != j && cellIdx != k) {
										int cell = cells[idx][cellIdx];
										if (!lineMask[vIndex][cell][c0]) {
											disable(cell, c0);
											lineMask[vIndex][cell][c0] = true;
											lineCounters[vIndex] = true;
										}
										if (!lineMask[vIndex][cell][c1]) {
											disable(cell, c1);
											lineMask[vIndex][cell][c1] = true;
											lineCounters[vIndex] = true;
										}
										if (!lineMask[vIndex][cell][c2]) {
											disable(cell, c2);
											lineMask[vIndex][cell][c2] = true;
											lineCounters[vIndex] = true;
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

}

void Sudoku::identifyLines(int vIndex) {

	for (int i = 0; i < 3; i++) {
		std::fill(tempRowMask[i].begin(), tempRowMask[i].end(), 0);
		std::fill(tempColMask[i].begin(), tempColMask[i].end(), 0);
	}
	for (int i = 0; i < 9; i++) {
		for (int c = 0; c < 9; c++) {
			for (int j = 0; j < 3; j++) {
				tempRowMask[j][c] = 0;
				tempColMask[j][c] = 0;
			}
			for (int j = 0; j < 9; j++) {
				if (mask[cells[i][j]][c] == 0) {
					tempRowMask[j/3][c]++;
					tempColMask[j%3][c]++;
				}
			}

			int rowCount = 0;
			int colCount = 0;
			int rowIdx = -1, colIdx = -1;
			for (int j = 0; j < 3; j++) {
				if (tempRowMask[j][c] > 0) {
					rowCount++;
					rowIdx = j;
				}
				if (tempColMask[j][c] > 0) {
					colCount++;
					colIdx = j;
				}
			}
			if (rowCount == 1) {
				for (int j = (i/3)*3; j < (i/3 + 1)*3; j++) {
					if (j != i) {
						for (int k = rowIdx*3; k < (rowIdx+1)*3; k++) {
							int cell = cells[j][k];
							if (!lineMask[vIndex][cell][c]) {
								disable(cell, c);
								lineMask[vIndex][cell][c] = true;
								lineCounters[vIndex] = true;
							}
						}
					}
				}
			}
			if (colCount == 1) {
				for (int j = i % 3; j < 9; j += 3) {
					if (j != i) {
						for (int k = colIdx; k < 9; k += 3) {
							int cell = cells[j][k];
							if (!lineMask[vIndex][cell][c]) {
								disable(cell, c);
								lineMask[vIndex][cell][c] = true;
								lineCounters[vIndex] = true;
							}
						}
					}
				}
			}
		}
	}
}

inline bool Sudoku::isPossible(int v, int c) {
	return mask[v][c] == 0;
}

inline void Sudoku::put(int v, int c) {
	solvedBoard[v] = c;
	for (int i : neighbors[v]) {
		// printf("%d, %d, %d\n", v, c, i);
		mask[i][c]++;
	}
	for (int i = 0; i < 9; i++) {
		mask[v][i]++;
	}
}

inline void Sudoku::disable(int v, int c) {
	mask[v][c]++;
}

inline void Sudoku::unput(int v, int c) {
	solvedBoard[v] = -1;
	for (int i : neighbors[v]) {
		mask[i][c]--;
	}
	for (int i = 0; i < 9; i++) {
		mask[v][i]--;
	}		
}

inline void Sudoku::enable(int v, int c) {
	mask[v][c]--;
}


void Sudoku::display(std::vector<signed char> &board, std::ostream& stream) {

	stream << "┌─────┬─────┬─────┐" << std::endl;
	for (int i = 0; i < 9; i++) {
		if (i > 0 && i % 3 == 0) {
			stream << "├─────┼─────┼─────┤" << std::endl;
		}
		for (int j = 0; j < 9; j++) {
			if (j % 3 == 0) {
				stream << "│";
			} else {
				stream << " ";
			}
			if (board[i*9+j] != -1) {
				stream << board[i*9+j]+1;
			} else {
				stream << " ";
			}
		}
		stream << "│" << std::endl;
	}
	stream << "└─────┴─────┴─────┘" << std::endl;
}

void Sudoku::display2(std::vector<signed char> &board, 
	std::vector<signed char> &solved, std::ostream& stream) {

	stream << "┌─────┬─────┬─────┐\t┌─────┬─────┬─────┐" << std::endl;
	for (int i = 0; i < 9; i++) {
		if (i > 0 && i % 3 == 0) {
			// printf("+-----+-----+-----+	 +-----+-----+-----+\n");
			stream << "├─────┼─────┼─────┤\t├─────┼─────┼─────┤" << std::endl;
		}
		for (int j = 0; j < 9; j++) {
			if (j % 3 == 0) {
				stream << "│";
				// printf("|");
			} else {
				stream << " ";
				// printf(" ");
			}
			if (board[i*9+j] != -1) {
				stream << board[i*9+j]+1;
				// printf("%d", board[i*9+j]+1);
			} else {
				stream << " ";
				// printf(" ");
			}
		}

		stream << "│\t";
		// printf("|\t");

		for (int j = 0; j < 9; j++) {
			if (j % 3 == 0) {
				stream << "│";
				// printf("|");
			} else {
				stream << " ";
				// printf(" ");
			}
			if (solved[i*9+j] != -1) {
				stream << solved[i*9+j]+1;
				// printf("%d", solved[i*9+j]+1);
			} else {
				stream << " ";
				// printf(" ");
			}
		}

		stream << "│" << std::endl;
		// printf("|\n");
	}
	// printf("+-----+-----+-----+	 +-----+-----+-----+\n");
	stream << "└─────┴─────┴─────┘\t└─────┴─────┴─────┘" << std::endl;
}

std::string Sudoku::printTime(long t1, long t2) {
	std::string unit = " ns";
	if (t2-t1 > 10000) {
		unit = " us";
		t1 /= 1000; t2 /= 1000;
	}
	if (t2-t1 > 10000) {
		unit = " ms";
		t1 /= 1000; t2 /= 1000;
	}
	if (t2-t1 > 10000) {
		unit = " seconds";
		t1 /= 1000; t2 /= 1000;
	}
	return std::to_string(t2-t1) + unit;
}

std::string Sudoku::getSolution() {
	std::string ret;
	for (char i : unsolvedBoard) {
		ret += 49+i;
	}
	ret += ',';
	for (char i : solvedBoard) {
		ret += 49+i;
	}
	return ret;
}

inline bool Sudoku::contains(std::vector<char> &a, int v) {
	return std::find(a.begin(), a.end(), v) != a.end();
}

inline int Sudoku::getCell(int v) {
	return 3 * (v / 27) + ((v / 3) % 3);
}

void Sudoku::connect() {
	for (int i = 0; i < 81; i++) {
		for (int j = 0; j < 20; j++) {
			neighbors[i][j] = -1;
		}
	}
	std::vector<char> n_count(81, 0);

	for (int i = 0; i < 81; i++) {
		for (int j = (i/9)*9; j < (i/9)*9 + 9; j++) {
			if (i != j) {
				neighbors[i][n_count[i]++] = j;
			}
		}
		for (int j = i%9; j < 81; j += 9) {
			if (i != j) {
				neighbors[i][n_count[i]++] = j;
			}
		}
		for (int j : cells[getCell(i)]) {
			if (i != j) {
				if (!contains(neighbors[i], j)) {
					neighbors[i][n_count[i]++] = j;
				}
			}
		}
	}
}

std::vector<std::vector<signed char>> Sudoku::getInput(std::string filename) {
	char buffer[82];
	std::ifstream in(filename);

	std::string first_line;
	getline(in, first_line);
	int num_lines = std::stoi(first_line);
	std::vector<std::vector<signed char>> boards(num_lines);
	for (int i = 0; i < num_lines; i++) {
		boards[i].resize(81);
		in.read(buffer, 82);
		for (int j = 0; j < 81; j++) {
			boards[i][j] = buffer[j] - 49;
		}

	}
	return boards;
}

char* Sudoku::getInputChars(std::string filename, int &size) {

	std::ifstream in(filename);

	std::string first_line;
	getline(in, first_line);
	size = std::stoi(first_line);
	char* boards = (char*) malloc((size * 82 + 1) * sizeof(char));

	for (int i = 0; i < size; i++) {
		in.read(&boards[82*i], 82);
	}
	boards[82 * size] = '\0';
	return boards;

}

ParallelSolver::ParallelSolver(int n) {
	solvers.resize(n);
	num_threads = n;
	for (Sudoku &s : solvers) {
		s.connect();
	}
}

extern "C" {

	Sudoku* Sudoku_new() {


		Sudoku* s = new Sudoku();
		s->connect();
		return s;
	}
	ParallelSolver* ParallelSolver_new(int n) {
		omp_set_num_threads(n);
		ParallelSolver* s = new ParallelSolver(n);
		return s;
	}

	void solve_sudokus_parallel(ParallelSolver* solver, char** boards, int n) {
		std::vector<std::vector<signed char>> vector_boards(n);
		omp_set_num_threads(solver->num_threads);
		#pragma omp parallel for schedule(dynamic, 1000)
		for (int i = 0; i < n; i++) {
			int tid = omp_get_thread_num();
			Sudoku searcher = solver->solvers[tid];

			vector_boards[i].resize(81);
			for (int j = 0; j < 81; j++) {
				vector_boards[i][j] = boards[i][j] - 49;
			}
			searcher.solveSudoku(vector_boards[i]);
			std::string sol = searcher.getSolution();
			for (int j = 0; j < 81; j++) {
				boards[i][j] = sol[82+j];
			}
		}
	}


	void solve_sudokus(Sudoku* searcher, char** boards, int n) {
		std::vector<std::vector<signed char>> vector_boards(n);
		for (int i = 0; i < n; i++) {
			vector_boards[i].resize(81);
			for (int j = 0; j < 81; j++) {
				vector_boards[i][j] = boards[i][j] - 49;
			}
			searcher->solveSudoku(vector_boards[i]);
			std::string sol = searcher->getSolution();
			for (int j = 0; j < 81; j++) {
				boards[i][j] = sol[82+j];
			}
		}
	}
/*
	void free_pointer(char* p) {
		printf("freeing %p\n", p);
		free(p);
		printf("pointer freed\n");
	}
*/
}
