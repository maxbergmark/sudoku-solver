/**
 **  Java Program to Implement Graph Coloring Algorithm
 **/
 
import java.util.Scanner;
import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Arrays;
import java.io.Console;
import java.io.File;
import java.io.PrintWriter;
 
/** Class Sudoku **/
public class Sudoku {	
	private int V, numOfColors;
	final private int[] unsolvedBoard;
	final private int[] color; 
	final private int[][] graph;
	final private int[][] neighbors;
	final private int[][] rowNeighbors;
	final private int[][] colNeighbors;
	final private int[][] boxNeighbors;
	final private int[][] cells;

	private static int[] clues;
	final private int[][] mask;
	final private int[] formattedMask;
	final private int[][] placedMask;
	final private int[][] lineMask;
	final private boolean[][][] lineMask2;
	final private int[] lineCounters;

	private int easySolved;
	private int totEasy;
	private int placedNumbers;
	private long totTime = 0;
	public long enables = 0;
	public long disables = 0;
	public long checks = 0;
	public long oneCount = 0;
	public long oneSuccess = 0;
	public long oneCancel = 0;
	private boolean solutionFound;
	public long lastPrint;
	private boolean shouldPrint;
	private boolean newMethod = true;

	public Sudoku() {
		mask = new int[81][9];
		formattedMask = new int[81];
		placedMask = new int[64][64];
		lineMask = new int[64][1];
		lineMask2 = new boolean[64][81][9];
		lineCounters = new int[64];
		graph = new int[81][81];
		neighbors = new int[81][20];
		rowNeighbors = new int[81][8];
		colNeighbors = new int[81][8];
		boxNeighbors = new int[81][8];
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
 
	/** Function to assign color **/
	public void graphColor(int noc, int[] board, int clue) {
		numOfColors = noc;
		// unsolvedBoard = board;
		System.arraycopy(board, 0, unsolvedBoard, 0, 81);
		System.arraycopy(board, 0, color, 0, 81);
		// color = board.clone();
		placedNumbers = 0;
		solutionFound = false;

		for (int i = 0; i < 81; i++) {
			for (int j = 0; j < 9; j++) {
				mask[i][j] = 0;
			}
		}
		
		if (newMethod) {
			for (int i = 0; i < 64; i++) {
				for (int j = 0; j < 81; j++) {
					for (int c = 0; c < 9; c++) {
						lineMask2[i][j][c] = false;
					}
				}
			}
		}

		for (int i = 0; i < 81; i++) {
			if (color[i] != -1) {
				put(i, color[i]);
				placedNumbers++;
			}
		}

		long t1 = 0,t2 = 0;
		t1 = System.nanoTime();
		solve(0, 0);

		if (solutionFound && placedNumbers == 81) {
			t2 = System.nanoTime();
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
		}
	}

	private void solve(int v, int vIndex) {

		lineCounters[vIndex] = 0;
		int easyIndex = placeEasy(vIndex);

		if (easyIndex == -1) {
			// System.out.println("corner");
			// display(color);
			if (newMethod) {
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
			} else {
				for (int i = 0; i < lineCounters[vIndex]; i += 2) {
					enable(lineMask[vIndex][i], lineMask[vIndex][i+1]);
				}
			}
			return;
		}

		if (placedNumbers == 81) {
			solutionFound = true;
			return;
		}

		while (v < 81 && color[v] >= 0) {
			v++;
		}

		for (int c = 0; c < numOfColors;c++) {
			if (isPossible(v, c)) {
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
		if (newMethod) {
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
		} else {
			for (int i = 0; i < lineCounters[vIndex]; i += 2) {
				enable(lineMask[vIndex][i], lineMask[vIndex][i+1]);
			}			
		}
	}

	private int placeEasy(int vIndex) {
		int easyIndex = 0;
		int lastPlaced = 0, tempPlaced = 0, easyplaced = 0;
		while (placedNumbers > lastPlaced) {
			lastPlaced = placedNumbers;
			while (placedNumbers > tempPlaced + 3) {
				tempPlaced = placedNumbers;
				for (int tempv = 0; tempv < 81; tempv++) {
					if (color[tempv] == -1) {
						int c = onePossible(tempv);
						if (c >= 0) {
							oneSuccess++;
							put(tempv, c);
							placedMask[vIndex][easyIndex++] = tempv;
							placedNumbers++;
						} else if (c == -2) {
							oneCancel++;
							for (int i = 0; i < easyIndex; i++) {
								int tempv2 = placedMask[vIndex][i];
								int c2 = color[tempv2];
								unput(tempv2, c2);
								placedNumbers--;
							}
							return -1;
						}
					}
				}
			}
			// tempPlaced = 1;
			// while (tempPlaced > 0) {
			if (placedNumbers < 45) {
				checkNakedPairs(vIndex);
			}
			if (placedNumbers < 45) {
				tempPlaced = identifyLines(vIndex);
			}
			// }
			// break;
		}
		return easyIndex;
	}

	private int getFormattedMask(int v) {
		if (color[v] >= 0) {
			return 0;
		}
		int x = 0;
		int y = 0;
		for (int c = 8; c >= 0; c--) {
		// for (int c = 0; c < 9; c++) {
			x <<= 1;
			x += mask[v][c] == 0 ? 1 : 0;
			y += mask[v][c] == 0 ? 1 : 0;
		}
		x <<= 16;
		return x + y;
	}

	private int getCachedMask(int v) {
		return formattedMask[v];
	}

	private void generateFormattedMasks() {
		for (int i = 0; i < 81; i++) {
			formattedMask[i] = getFormattedMask(i);
		}
	}

	private void checkNakedPairs(int vIndex) {
		// System.out.println("naked pairs");
		generateFormattedMasks();
		for (int i = 0; i < 81; i++) {
			// int bitmask = getFormattedMask(i);
			int bitmask = formattedMask[i];
			if ((bitmask & 0xffff) == 2) {
				for (int j = i+1; j < (i/9+1)*9; j++) {
					// int bitmask_j = getFormattedMask(j);
					int bitmask_j = formattedMask[j];
					if (bitmask == bitmask_j) {
						// System.out.println("found candidate");
						bitmask >>= 16;
						int c0, c1, k = 0;
						while ((bitmask & 1) == 0) {
							k++;
							bitmask >>= 1;
						}
						c0 = k;
						while ((bitmask & 1) == 0) {
							k++;
							bitmask >>= 1;
						}
						c1 = k;
						for (int cell = (i/9)*9; cell < (i/9+1)*9; cell++) {
							if (newMethod && (cell != i && cell != j)) {
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
			// int bitmask = getFormattedMask(i);
			int bitmask = formattedMask[i];
			if ((bitmask & 0xffff) == 2) {
				for (int j = i+9; j < 81; j += 9) {
					// int bitmask_j = getFormattedMask(j);
					int bitmask_j = formattedMask[j];
					if (bitmask == bitmask_j) {
						// System.out.println("found candidate");
						bitmask >>= 16;
						int c0, c1, k = 0;
						while ((bitmask & 1) == 0) {
							k++;
							bitmask >>= 1;
						}
						c0 = k;
						while ((bitmask & 1) == 0) {
							k++;
							bitmask >>= 1;
						}
						c1 = k;
						for (int cell = i % 9; cell < 81; cell += 9) {
							if (newMethod && (cell != i && cell != j)) {
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
				// int bitmask = getFormattedMask(cells[idx][i]);
				int bitmask = formattedMask[cells[idx][i]];
				if ((bitmask & 0xffff) == 2) {
					for (int j = i+1; j < 9; j++) {
						// int bitmask_j = getFormattedMask(cells[idx][j]);
						int bitmask_j = formattedMask[cells[idx][j]];
						if (bitmask == bitmask_j) {
							// System.out.println("found candidate");
							bitmask >>= 16;
							int c0, c1, k = 0;
							while ((bitmask & 1) == 0) {
								k++;
								bitmask >>= 1;
							}
							c0 = k;
							while ((bitmask & 1) == 0) {
								k++;
								bitmask >>= 1;
							}
							c1 = k;
							for (int cellIdx = 0; cellIdx < 9; cellIdx++) {
								if (newMethod && (cellIdx != i && cellIdx != j)) {
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

	private int identifyLines(int vIndex) {

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
					disabledLines++;
					// System.out.println("should disable " + (c+1) + " on row " + ((i/3)*3 + rowIdx));
					// System.out.println(Arrays.toString(tempRowMask[0]));
					// System.out.println(Arrays.toString(tempRowMask[1]));
					// System.out.println(Arrays.toString(tempRowMask[2]));
					for (int j = (i/3)*3; j < (i/3 + 1)*3; j++) {
						if (j != i) {
							for (int k = rowIdx*3; k < (rowIdx+1)*3; k++) {
								int cell = cells[j][k];
								// System.out.println("    disabling " + (c+1) + " on cell " + cell);
								if (newMethod) {
									if (!lineMask2[vIndex][cell][c]) {
										disable(cell, c);
										lineMask2[vIndex][cell][c] = true;
										lineCounters[vIndex]++;
										// lineMask[vIndex][lineCounters[vIndex]++] = cell;
										// lineMask[vIndex][lineCounters[vIndex]++] = c;
									}
								} else {
									disable(cell, c);
									lineMask[vIndex][lineCounters[vIndex]++] = cell;
									lineMask[vIndex][lineCounters[vIndex]++] = c;
								}
							}
						}
					}

				}
				if (colCount == 1) {
					disabledLines++;
					// System.out.println("should disable " + (c+1) + " on col " + ((i%3)*3 + colIdx));
					// System.out.println(Arrays.toString(tempColMask[0]));
					// System.out.println(Arrays.toString(tempColMask[1]));
					// System.out.println(Arrays.toString(tempColMask[2]));
					for (int j = i % 3; j < 9; j += 3) {
						if (j != i) {
							for (int k = colIdx; k < 9; k += 3) {
								int cell = cells[j][k];
								// System.out.println("    disabling " + (c+1) + " on cell " + cell);
								if (newMethod) {
									if (!lineMask2[vIndex][cell][c]) {
										disable(cell, c);
										lineMask2[vIndex][cell][c] = true;
										lineCounters[vIndex]++;
										// lineMask[vIndex][lineCounters[vIndex]++] = cell;
										// lineMask[vIndex][lineCounters[vIndex]++] = c;
									}
								} else {
									disable(cell, c);
									lineMask[vIndex][lineCounters[vIndex]++] = cell;
									lineMask[vIndex][lineCounters[vIndex]++] = c;
								}
							}
						}
					}
				}
			}
		}
		return disabledLines;
		// System.out.println(Arrays.toString(mask[26]));
	}

	private boolean isPossible(int v, int c) {
		checks++;
		return mask[v][c] == 0;
	}

	private int checkMask(int[][] neighbors, int v, int c) {
		int tempValue = 0;
		for (int n : neighbors[v]) {
			if (mask[n][c] > 0) {
				tempValue++;
			}
		}
		return tempValue;
	}

	private int onePossible(int v) {
		oneCount++;

		int temp = 0;
		int tempcol = -1;
		for (int c = 0; c < 9; c++) {

			boolean b = isPossible(v,c);
			temp += b ? 1:0;
			tempcol = b ? c : tempcol;
		}
		if (temp == 1) {
			return tempcol;
		}

		int tempValue;
		int tempColor = -1;
		int retValue = 0;
		for (int i = 0; i < 9; i++) {
			if (!isPossible(v, i)) {
				continue;
			}
			int rowValue = checkMask(rowNeighbors, v, i);
			int colValue = checkMask(colNeighbors, v, i);
			int boxValue = checkMask(boxNeighbors, v, i);

			tempValue = Math.max(rowValue, Math.max(colValue, boxValue));
			if (retValue == 8 && tempValue == 8) {
				// System.out.println(String.format(
					// "found impossible state on %d (%d, %d) (%d, %d)", 
					// v, (v/9)+1, (v%9)+1, tempColor+1, i+1));
				// System.out.println(String.format("%d, %d, %d", rowValue, colValue, boxValue));
				// System.out.println(Arrays.toString(mask[58]));

				return -2;
			} else if (tempValue == 8) {
				retValue = 8;
				tempColor = i;
			}
		}
		return retValue == 8 ? tempColor : -1;
	}

	private void put(int v, int c) {
		color[v] = c;
		disables++;
		for (int i : neighbors[v]) {
			mask[i][c]++;
			// if (i == 58) {
				// System.out.println("putting " + (c+1) + " from " + v + String.format(" (%d, %d) ", (v/9)+1, (v%9)+1) + Arrays.toString(mask[58]));
			// }
		}
		for (int i = 0; i < 9; i++) {
			mask[v][i]++;
		}
	}

	private void disable(int v, int c) {
		// if (v == 58) {
			// System.out.println("disabling " + (c+1));
		// }
		mask[v][c]++;
		// for (int i : neighbors[v]) {
			// mask[i][c]++;
		// }
	}

	private void unput(int v, int c) {
		color[v] = -1;
		enables++;
		for (int i : neighbors[v]) {
			mask[i][c]--;
			// if (i == 58) {
				// System.out.println("unputting " + (c+1) + " from " + v + String.format(" (%d, %d) ", (v/9)+1, (v%9)+1) + Arrays.toString(mask[58]));
			// }
		}
		for (int i = 0; i < 9; i++) {
			mask[v][i]--;
		}		
	}

	private void enable(int v, int c) {
		mask[v][c]--;
		// for (int i : neighbors[v]) {
			// mask[i][c]--;
		// }
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
		int[] n_rowCount = new int[81];
		int[] n_colCount = new int[81];
		int[] n_boxCount = new int[81];
		V = graph.length;

		HashMap<Integer,ArrayList<Integer>> map = new HashMap<Integer,ArrayList<Integer>>();
 
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
					graph[i][j] = graph[j][i] = 1;
					neighbors[i][n_count[i]++] = j;
					rowNeighbors[i][n_rowCount[i]++] = j;
				}
			}
			for (int j = i%9; j < 81; j += 9) {
				if (i != j) {
					graph[i][j] = graph[j][i] = 1;
					neighbors[i][n_count[i]++] = j;
					colNeighbors[i][n_colCount[i]++] = j;
				}
			}
			for (int j : map.get(i)) {
				if (i != j) {
					graph[i][j] = graph[j][i] = 1;
					if (!contains(neighbors[i], j)) {
						neighbors[i][n_count[i]++] = j;
					}
					boxNeighbors[i][n_boxCount[i]++] = j;
				}
			}
		}
	}

	public static int[][] getInput() {
		Scanner scan = new Scanner(System.in);
		int n = scan.nextInt();
		int[][] board = new int[n][81];
		for (int k = 0; k < n; k++) {
			for (int i = 0; i < 81; i++) {
				int x = scan.nextInt()-1;
				board[k][i] = x;
			}
		}
		return board;
	}

	public static int[][] getInput2() {
		Scanner scan = new Scanner(System.in);
		int n = scan.nextInt();
		clues = new int[n];
		int[][] board = new int[n][81];
		for (int k = 0; k < n; k++) {
			String line = scan.next();
			int clue = 0;
			for (int i = 0; i < 81; i++) {
				int x = Character.getNumericValue(line.charAt(i))-1;
				if (x != -1) {clue++;}
				board[k][i] = x;
			}
			clues[k] = clue;
		}
		return board;
	}

	private int getTotEasy() {
		return totEasy;
	}

	public String getSolution() {
		StringBuilder s = new StringBuilder();
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
		long t0 = System.nanoTime();
		int[][] board = gc.getInput2();
		long tinp = System.nanoTime();
		gc.connect();
		long t1 = System.nanoTime();
		p.println(board.length);

		for (int i = 0; i < board.length; i++) {
			long tempTime = System.nanoTime();
			if (tempTime - gc.lastPrint > 20_000_000) {
				gc.shouldPrint = true;
				gc.lastPrint = tempTime;
				System.out.print(String.format(
					"\r(%7d/%7d) ", i+1, board.length));
			}
			gc.graphColor(9, board[i], gc.clues[i]);
			p.println(gc.getSolution());
			// break;
			// if (i == 4) {
				// break;
			// }
		}
		p.close();
		System.out.println();
		System.out.println(String.format("disables: %d\nenables: %d\n" 
			+ "checks: %d\noneCount: %d\noneSuccess: %d\noneCancel: %d", 
			gc.disables, gc.enables, gc.checks, 
			gc.oneCount, gc.oneSuccess, gc.oneCancel));

		long t2 = System.nanoTime();
		System.out.println("Total time (including prints): " 
			+ gc.printTime(t1,t2));
		System.out.println("Sudoku solving time: " 
			+ gc.printTime(0,gc.getTime()));
		System.out.println("Average time per board: " 
			+ gc.printTime(0,gc.getTime()/board.length));
		System.out.println("Number of one-choice digits per board: " 
			+ String.format("%.2f", gc.getTotEasy()/(double)board.length));  
		System.out.println("Easily solvable boards: " + gc.easySolved);
		System.out.println("\nInput time: " + gc.printTime(t0,tinp));
		System.out.println("Connect time: " + gc.printTime(tinp,t1));
	}
}
