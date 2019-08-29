import java.util.Scanner;
import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Arrays;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.Console;
import java.io.File;
import java.io.PrintWriter;
 
public class Sudoku {	

	final private int[] unsolvedBoard;
	final private int[] color; 
	final private int[][] neighbors;
	final private int[][] cells;

	private static int[] clues;
	final private int[][] mask;
	final private int[] formattedMask;
	final private int[][] placedMask;
	final private int[][] lineMask;
	final private boolean[][][] lineMask2;
	final private int[] lineCounters;
	final private int[][] sectionCounters;
	final private int[][] sectionMask;

	private int easySolved;
	private boolean isEasy;
	private int totEasy;
	private int placedNumbers;
	public long totTime = 0;
	public long enables = 0;
	public long disables = 0;
	public long puts = 0;
	public long unputs = 0;
	public long checks = 0;
	public long oneCount = 0;
	public long oneSuccess = 0;
	public long oneCancel = 0;
	private boolean solutionFound;
	public long lastPrint;
	private boolean shouldPrint;
	private boolean isImpossible = false;

	public Sudoku() {
		mask = new int[81][9];
		formattedMask = new int[81];
		placedMask = new int[64][64];
		lineMask = new int[64][1];
		lineMask2 = new boolean[64][81][9];
		sectionCounters = new int[9][27];
		sectionMask = new int[9][27];
		lineCounters = new int[64];
		neighbors = new int[81][20];
		unsolvedBoard = new int[81];
		color = new int[81];
		cells = new int[][] {{0 ,1 ,2 ,9 ,10,11,18,19,20},
							 {3 ,4 ,5 ,12,13,14,21,22,23},
							 {6 ,7 ,8 ,15,16,17,24,25,26},
							 {27,28,29,36,37,38,45,46,47},
							 {30,31,32,39,40,41,48,49,50},
							 {33,34,35,42,43,44,51,52,53},
							 {54,55,56,63,64,65,72,73,74},
							 {57,58,59,66,67,68,75,76,77},
							 {60,61,62,69,70,71,78,79,80}};
	}
 
	final public long solveSudoku(int[] board, int clue) {

		long t1 = 0,t2 = 0;
		t1 = System.nanoTime();
		System.arraycopy(board, 0, unsolvedBoard, 0, 81);
		System.arraycopy(board, 0, color, 0, 81);

		placedNumbers = 0;
		solutionFound = false;
		isEasy = true;
		isImpossible = false;

		for (int[] i : mask) {
			Arrays.fill(i, 0);
		}
		
		for (boolean[][] i : lineMask2) {
			for (boolean[] j : i) {
				Arrays.fill(j, false);
			}
		}

		for (int i = 0; i < 81; i++) {
			if (color[i] != -1) {
				put(i, color[i]);
				placedNumbers++;
			}
		}

		solve(0, 0);
		t2 = System.nanoTime();
		easySolved += isEasy ? 1 : 0;

		if (solutionFound && placedNumbers == 81) {
			totTime += t2-t1;
			if (shouldPrint || t2-t1 > 5*1_000_000_000L) {
				System.out.print(String.format(
					"Solution from %2d clues found in %7s", 
					clue, 
					printTime(t1, t2)
				));
				shouldPrint = false;
				if (t2-t1 > 1*1000_000_000L) {
					System.out.println();
					display2(board, color);
				}
			}
		} else {
			System.out.println("No solution");
			display2(board, color);
			return -1;
		}
		return t2 - t1;
	}

	final private void solve(int v, int vIndex) {

		lineCounters[vIndex] = 0;
		int easyIndex = placeEasy(vIndex);

		if (easyIndex == -1 || isImpossible) {
			if (lineCounters[vIndex] > 0) {
				for (int i = 0; i < 81; i++) {
					for (int c = 0; c < 9; c++) {
						if (lineMask2[vIndex][i][c]) {
							enable(i, c);
							lineMask2[vIndex][i][c] = false;
						}
					}
				}
			}
			isImpossible = false;
			return;
		}

		if (placedNumbers == 81) {
			solutionFound = true;
			return;
		}

		while (v < 81 && color[v] >= 0) {
			v++;
		}
		// generateFormattedMasks();
		// int minOptions = 9;
		// for (int i = 0; i < 81; i++) {
			// int options = formattedMask[i] & 0xffff;
			// if (options > 0 && options < minOptions) {
				// minOptions = options;
				// v = i;
			// }
		// }

		for (int c = 0; c < 9; c++) {
			if (isPossible(v, c)) {
				isEasy = false;
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

		for (int i = 0; i < easyIndex; i++) {
			int tempv = placedMask[vIndex][i];
			int c = color[tempv];
			unput(tempv, c);
			placedNumbers--;
		}
		if (lineCounters[vIndex] > 0) {
			for (int i = 0; i < 81; i++) {
				for (int c = 0; c < 9; c++) {
					if (lineMask2[vIndex][i][c]) {
						enable(i, c);
						lineMask2[vIndex][i][c] = false;
					}
				}
			}
		}
	}

	final private int placeEasy(int vIndex) {
		int easyIndex = 0;
		int lastPlaced = 0, tempPlaced = 0, easyplaced = 0;
		int iter = 0;
		while (placedNumbers > lastPlaced+1) {
			lastPlaced = placedNumbers;
			tempPlaced = 0;
			while (placedNumbers > tempPlaced + 5) {
				tempPlaced = placedNumbers;
				easyIndex = placeNakedSingles(vIndex, easyIndex);
				if (easyIndex < 0) {
					return -1;
				}
			}

			tempPlaced = 0;
			while (placedNumbers < 55*1 && placedNumbers > tempPlaced + 2) {
				tempPlaced = placedNumbers;
				easyIndex = placeHiddenSingles(vIndex, easyIndex);
			}

			tempPlaced = 0;
			while (placedNumbers < 65*1 && placedNumbers > tempPlaced + 1) {
				tempPlaced = placedNumbers;
				easyIndex = placeNakedSingles(vIndex, easyIndex);
				if (easyIndex < 0) {
					return -1;
				}
			}

			if (iter < 2 && placedNumbers < 55*1) {
				checkNakedTriples(vIndex);
			}
			if (placedNumbers < 45*1) {
				checkNakedDoubles(vIndex);
				identifyLines(vIndex);
			}
			iter++;
		}
		return easyIndex;
	}

	final private int placeNakedSingles(int vIndex, int easyIndex) {
		generateFormattedMasks();
		for (int tempv = 0; tempv < 81; tempv++) {
			int possibilities = formattedMask[tempv];
			if ((possibilities & 0xffff) == 1) {
				possibilities >>= 16;
				int c = 0;
				while ((possibilities & 1) == 0) {
					possibilities >>= 1;
					c++;
				}
				put(tempv, c);
				placedMask[vIndex][easyIndex++] = tempv;
				placedNumbers++;				
			} else if (possibilities == 0 && color[tempv] == -1 
				|| isImpossible) {
				
				for (int i = 0; i < easyIndex; i++) {
					int tempv2 = placedMask[vIndex][i];
					int c2 = color[tempv2];
					unput(tempv2, c2);
					placedNumbers--;
				}
				return -1;
			}
		}
		return easyIndex;
	}

	final private int placeHiddenSingles(int vIndex, int easyIndex) {
		for (int[] i : sectionCounters) {
			Arrays.fill(i, 0);
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
					}
				}
			}

		}
		return easyIndex;
	}

	final private int getFormattedMask(int v) {
		if (color[v] >= 0) {
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

	final private int getCachedMask(int v) {
		return formattedMask[v];
	}

	final private void generateFormattedMasks() {
		for (int i = 0; i < 81; i++) {
			formattedMask[i] = getFormattedMask(i);
		}
	}

	final private void checkNakedDoubles(int vIndex) {
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
								if (!lineMask2[vIndex][cell][c0]) {
									disable(cell, c0);
									lineMask2[vIndex][cell][c0] = true;
									lineCounters[vIndex]++;
								}
								if (!lineMask2[vIndex][cell][c1]) {
									disable(cell, c1);
									lineMask2[vIndex][cell][c1] = true;
									lineCounters[vIndex]++;
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
								if (!lineMask2[vIndex][cell][c0]) {
									disable(cell, c0);
									lineMask2[vIndex][cell][c0] = true;
									lineCounters[vIndex]++;
								}
								if (!lineMask2[vIndex][cell][c1]) {
									disable(cell, c1);
									lineMask2[vIndex][cell][c1] = true;
									lineCounters[vIndex]++;
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
									if (!lineMask2[vIndex][cell][c0]) {
										disable(cell, c0);
										lineMask2[vIndex][cell][c0] = true;
										lineCounters[vIndex]++;
									}
									if (!lineMask2[vIndex][cell][c1]) {
										disable(cell, c1);
										lineMask2[vIndex][cell][c1] = true;
										lineCounters[vIndex]++;
									}
								}
							}
						}
					}
				}
			}
		}
	}

	final private void checkNakedTriples(int vIndex) {

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
										if (!lineMask2[vIndex][cell][c0]) {
											disable(cell, c0);
											lineMask2[vIndex][cell][c0] = true;
											lineCounters[vIndex]++;
										}
										if (!lineMask2[vIndex][cell][c1]) {
											disable(cell, c1);
											lineMask2[vIndex][cell][c1] = true;
											lineCounters[vIndex]++;
										}
										if (!lineMask2[vIndex][cell][c2]) {
											disable(cell, c2);
											lineMask2[vIndex][cell][c2] = true;
											lineCounters[vIndex]++;
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
					// if (bitmask == bitmask_j) {
					if (bitmask_j > 0 && bitmask == (bitmask | bitmask_j)) {
						for (int k = j+9; k < 81; k += 9) {
							int bitmask_k = formattedMask[k];
							// if (bitmask == bitmask_k) {
							if (bitmask_k > 0 && bitmask == (bitmask | bitmask_k)) {

								// bitmask >>= 16;
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
										if (!lineMask2[vIndex][cell][c0]) {
											disable(cell, c0);
											lineMask2[vIndex][cell][c0] = true;
											lineCounters[vIndex]++;
										}
										if (!lineMask2[vIndex][cell][c1]) {
											disable(cell, c1);
											lineMask2[vIndex][cell][c1] = true;
											lineCounters[vIndex]++;
										}
										if (!lineMask2[vIndex][cell][c2]) {
											disable(cell, c2);
											lineMask2[vIndex][cell][c2] = true;
											lineCounters[vIndex]++;
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
						// if (bitmask == bitmask_j) {
						if (bitmask_j > 0 && bitmask == (bitmask | bitmask_j)) {
							for (int k = j+1; k < 9; k++) {
								int bitmask_k = formattedMask[cells[idx][k]];
								// if (bitmask == bitmask_k) {
								if (bitmask_k > 0 && bitmask == (bitmask | bitmask_k)) {

									// bitmask >>= 16;
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
											if (!lineMask2[vIndex][cell][c0]) {
												disable(cell, c0);
												lineMask2[vIndex][cell][c0] = true;
												lineCounters[vIndex]++;
											}
											if (!lineMask2[vIndex][cell][c1]) {
												disable(cell, c1);
												lineMask2[vIndex][cell][c1] = true;
												lineCounters[vIndex]++;
											}
											if (!lineMask2[vIndex][cell][c2]) {
												disable(cell, c2);
												lineMask2[vIndex][cell][c2] = true;
												lineCounters[vIndex]++;
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

	final private void identifyLines(int vIndex) {

		int disabledLines = 0;
		int[][] tempRowMask = new int[3][9];
		int[][] tempColMask = new int[3][9];
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
								if (!lineMask2[vIndex][cell][c]) {
									disable(cell, c);
									lineMask2[vIndex][cell][c] = true;
									lineCounters[vIndex]++;
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
								if (!lineMask2[vIndex][cell][c]) {
									disable(cell, c);
									lineMask2[vIndex][cell][c] = true;
									lineCounters[vIndex]++;
								}
							}
						}
					}
				}
			}
		}
	}

	final private boolean isPossible(int v, int c) {
		// checks++;
		return mask[v][c] == 0;
	}

	final private int checkMask(int[][] neighbors, int v, int c) {
		int tempValue = 0;
		for (int n : neighbors[v]) {
			if (mask[n][c] > 0) {
				tempValue++;
			}
		}
		return tempValue;
	}

	final private void put(int v, int c) {
		color[v] = c;
		puts++;
		for (int i : neighbors[v]) {
			mask[i][c]++;
		}
		for (int i = 0; i < 9; i++) {
			mask[v][i]++;
		}
	}

	final private void disable(int v, int c) {
		disables++;
		mask[v][c]++;
	}

	final private void unput(int v, int c) {
		color[v] = -1;
		unputs++;
		for (int i : neighbors[v]) {
			mask[i][c]--;
		}
		for (int i = 0; i < 9; i++) {
			mask[v][i]--;
		}		
	}

	final private void enable(int v, int c) {
		enables++;
		mask[v][c]--;
	}

	public String getString(int[] board) {
		StringBuilder s = new StringBuilder();
		for (int i : board) {
			s.append(i+1);
		}
		return s.toString();
	}

	public long getTime() {
		return totTime;
	}

	public static String printTime(long t1, long t2) {
		String unit = " ns";
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
		return (t2-t1) + unit;
	}

	public void display(int[] color) {

		for (int i = 0; i < 9; i++) {
			if (i % 3 == 0) {
				System.out.println("+-----+-----+-----+");
			}
			for (int j = 0; j < 9; j++) {
				if (j % 3 == 0) {
					System.out.print("|");
				} else {
					System.out.print(" ");
				}
				if (color[i*9+j] != -1) {
					System.out.print(color[i*9+j]+1);
				} else {
					System.out.print(" ");
				}
			}
			System.out.println("|");
		}
		System.out.println("+-----+-----+-----+");
	}

	public void display2(int[] board, int[] color) {

		for (int i = 0; i < 9; i++) {
			if (i % 3 == 0) {
				System.out.println("+-----+-----+-----+	 +-----+-----+-----+");
			}
			for (int j = 0; j < 9; j++) {
				if (j % 3 == 0) {
					System.out.print("|");
				} else {
					System.out.print(" ");
				}
				if (board[i*9+j] != -1) {
					System.out.print(board[i*9+j]+1);
				} else {
					System.out.print(" ");
				}
			}

			System.out.print("|	 ");

			for (int j = 0; j < 9; j++) {
				if (j % 3 == 0) {
					System.out.print("|");
				} else {
					System.out.print(" ");
				}
				if (color[i*9+j] != -1) {
					System.out.print(color[i*9+j]+1);
				} else {
					System.out.print(" ");
				}
			}

			System.out.println("|");
		}
		System.out.println("+-----+-----+-----+	 +-----+-----+-----+");
	}

	private boolean contains(int[] a, int v) {
		for (int i : a) {
			if (i == v) {
				return true;
			}
		}
		return false;
	}

	public void connect() {
		for (int i = 0; i < 81; i++) {
			for (int j = 0; j < 20; j++) {
				neighbors[i][j] = -1;
			}
		}
		int[] n_count = new int[81];

		HashMap<Integer,ArrayList<Integer>> map 
			= new HashMap<Integer,ArrayList<Integer>>();
 
		for (int[] c: cells) {
			ArrayList<Integer> temp = new ArrayList<Integer>();
			for (int v : c) {
				temp.add(v);
			}
			for (int v : c) {
				map.put(v,temp);
			}
		}

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
			for (int j : map.get(i)) {
				if (i != j) {
					if (!contains(neighbors[i], j)) {
						neighbors[i][n_count[i]++] = j;
					}
				}
			}
		}
	}

	public static int[][] getInput(String filename) {
		int[][] boards;
		try (BufferedInputStream in = new BufferedInputStream(
			new FileInputStream(filename))) {
			
			BufferedReader r = new BufferedReader(
				new InputStreamReader(in, StandardCharsets.UTF_8));
			int n = Integer.valueOf(r.readLine());
			boards = new int[n][81];
			clues = new int[n];
			for (int i = 0; i < n; i++) {
				for (int j = 0; j < 81; j++) {
					int x = r.read();
					boards[i][j] = x - 49;
					clues[i] += x > 48 ? 1 : 0;
				}
				r.read();
			}
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
		return boards;
	}

	private int getTotEasy() {
		return totEasy;
	}

	public String getSolution() {
		StringBuilder s = new StringBuilder(256);
		for (int i : unsolvedBoard) {
			s.append(i+1);
		}
		s.append(",");
		for (int i : color) {
			s.append(i+1);
		}
		return s.toString();
	}

	public static void main (String[] args) {
		Sudoku gc = new Sudoku();
		File f;
		PrintWriter p;
		try {
			f = new File("sudoku_output.txt");
			p = new PrintWriter(f);
		} catch (Exception e) {
			return;
		}
		if (args.length != 1) {
			System.out.println("Usage: java Sudoku <input_file>");
			return;
		}
		long t0 = System.nanoTime();
		int[][] boards = gc.getInput(args[0]);
		long tinp = System.nanoTime();
		gc.connect();
		long t1 = System.nanoTime();
		p.println(boards.length);

		long maxSolveTime = 0;
		int maxSolveIndex = 0;
		long[] solveTimes = new long[boards.length];
		for (int i = 0; i < boards.length; i++) {
			long tempTime = System.nanoTime();
			if (tempTime - gc.lastPrint > 200_000_000 
				|| i == boards.length - 1) {
				
				gc.shouldPrint = true;
				gc.lastPrint = tempTime;
				System.out.print(String.format(
					"\r(%7d/%7d) ", i+1, boards.length));
			}
			long elapsed = gc.solveSudoku(boards[i], gc.clues[i]);
			if (elapsed == -1) {
				System.out.println("Impossible: " + i);
			}
			if (elapsed > maxSolveTime) {
				maxSolveTime = elapsed;
				maxSolveIndex = i;
			}
			solveTimes[i] = elapsed;
			p.println(gc.getSolution());
		}

		p.close();
		long t2 = System.nanoTime();
		Arrays.sort(solveTimes);
		System.out.println();
		System.out.println("Median solve time: " 
			+ gc.printTime(0, solveTimes[boards.length/2]));
		System.out.println("Longest solve time: " 
			+ gc.printTime(0, maxSolveTime) + " for board " + maxSolveIndex);
		gc.display(boards[maxSolveIndex]);
		System.out.println();
		System.out.println(String.format("disables: %9d\nenables:  %9d\n"
			+ "puts:     %9d\nunputs:   %9d\n" 
			+ "checks: %d\noneCount: %d\noneSuccess: %d\noneCancel: %d", 
			gc.disables, gc.enables, gc.puts, gc.unputs, gc.checks, 
			gc.oneCount, gc.oneSuccess, gc.oneCancel));

		System.out.println("Total time (including prints): " 
			+ gc.printTime(t1,t2));
		System.out.println("Sudoku solving time: " 
			+ gc.printTime(0,gc.getTime()));
		System.out.println("Average time per board: " 
			+ gc.printTime(0,gc.getTime()/boards.length));
		System.out.println("Number of one-choice digits per board: " 
			+ String.format("%.2f", gc.getTotEasy()/(double)boards.length));  
		System.out.println("Easily solvable boards: " + gc.easySolved);
		System.out.println("\nInput time: " + gc.printTime(t0,tinp));
		System.out.println("Connect time: " + gc.printTime(tinp,t1));
	}
}