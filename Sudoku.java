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
	private int[] unsolvedBoard;
	private int[] color; 
	private int[][] graph;
	final private int[][] neighbors;
	final private int[][] rowNeighbors;
	final private int[][] colNeighbors;
	final private int[][] boxNeighbors;
	private static int[] clues;
	final private int[][] mask;
	final private int[][] placedMask;

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

	public Sudoku() {
		mask = new int[81][9];
		placedMask = new int[81][64];
		neighbors = new int[81][20];
		rowNeighbors = new int[81][8];
		colNeighbors = new int[81][8];
		boxNeighbors = new int[81][8];
	}
 
	/** Function to assign color **/
	public void graphColor(int noc, int[] board, int clue) {
		numOfColors = noc;
		color = new int[V];
		unsolvedBoard = board;
		color = board.clone();
		placedNumbers = 0;
		solutionFound = false;

		for (int i = 0; i < 81; i++) {
			for (int j = 0; j < 9; j++) {
				mask[i][j] = 0;
			}
		}
		for (int i = 0; i < 81; i++) {
			if (color[i] != -1) {
				disable(i, color[i]);
				placedNumbers++;
			}
		}

		long t1 = 0,t2 = 0;
		t1 = System.nanoTime();
		solve(0);

		if (solutionFound && placedNumbers == 81) {
			t2 = System.nanoTime();
			totTime += t2-t1;
			if (shouldPrint || t2-t1 > 5*1000000000L) {
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

	private int placeEasy(int vIndex) {
		int easyIndex = 0;
		int tempPlaced = 0, easyplaced = 0;
		while (placedNumbers - tempPlaced > 3 && placedNumbers < 68) {
			tempPlaced = placedNumbers;
			for (int tempv = 0; tempv < 81; tempv++) {
				if (color[tempv] == -1) {
					int c = onePossible(tempv);
					if (c >= 0) {
						color[tempv] = c;
						disable(tempv, c);
						placedMask[vIndex][easyIndex++] = tempv;
						placedNumbers++;
					} else if (c == -2) {
						oneCancel++;
						for (int i = 0; i < easyIndex; i++) {
							int tempv2 = placedMask[vIndex][i];
							int c2 = color[tempv2];
							color[tempv2] = -1;
							enable(tempv2, c2);
							placedNumbers--;
						}
						return -1;
					}
				}
			}
		}
		return easyIndex;
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

	private void solve(int v) {

		while (v < 81 && color[v] >= 0) {
			v++;
		}

		if (v == V) {
			solutionFound = true;
			return;
		}

		int vIndex = v;
		int easyIndex = placeEasy(vIndex);

		if (easyIndex == -1) {
			return;
		}


		while (v < 81 && color[v] >= 0) {
			v++;
		}


		if (placedNumbers == 81) {
			solutionFound = true;
			return;
		}

		for (int c = 0; c < numOfColors;c++) {
			if (isPossible(v, c)) {
				color[v] = c;
				disable(v, c);
				placedNumbers++;
				solve(v + 1); 
				if (solutionFound) {
					return;
				}
				color[v] = -1;
				enable(v, c);
				placedNumbers--;
			}
		}

		for (int i = 0; i < easyIndex; i++) {
			int tempv = placedMask[vIndex][i];
			int c = color[tempv];
			color[tempv] = -1;
			enable(tempv, c);
			placedNumbers--;
		}
	}



	public void solve2(int v) {
  
		while (v < 81 && color[v] >= 0) {v++;}
  
		if (v == V) {
			solutionFound = true;
			return;
			// throw new Exception("Solution found");
		}

		int oneOption = onePossible(v);
		if (oneOption == -2) {
			oneCancel++;
			return;
		} else if (oneOption >= 0) {
			oneSuccess++;
			// System.out.println("Got one option only: " + oneOption);
			color[v] = oneOption;
			disable(v, oneOption);
			solve(v + 1);
			if (!solutionFound) {
				color[v] = -1;
				enable(v, oneOption);
			}
			return;
		}

		for (int c = 0; c < numOfColors;c++) {
			if (isPossible(v, c)) {
				color[v] = c;
				disable(v, c);
				solve(v + 1); 
				if (solutionFound) {
					return;
				}
				color[v] = -1;
				enable(v, c);
			}
		}	

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
			// System.out.println("tempcol: " + tempcol);
			return tempcol;
			// color[v] = tempcol;
			// disable(v, tempcol);
			// easyplaced++;
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
				return -2;
			} else if (tempValue == 8) {
				retValue = 8;
				tempColor = i;
			}
		}
		return retValue == 8 ? tempColor : -1;
	}

	public boolean isPossiblea(int v, int c) {
		for (int i = 0; i < V; i++) {
			checks++;
			if (graph[v][i] == 1 && c == color[i]) {
				return false;
			}
		}
		return true;
	}

	private void disable(int v, int c) {
		disables++;
		for (int i : neighbors[v]) {
			mask[i][c]++;
		}
		for (int i = 0; i < 9; i++) {
			mask[v][i]++;
		}
		// for (int i : rowNeighbors[v]) {
			// rowMask[i][c]++;
		// }
		// for (int i : colNeighbors[v]) {
			// colMask[i][c]++;
		// }
		// for (int i : boxNeighbors[v]) {
			// boxMask[i][c]++;
		// }
	}

	private void enable(int v, int c) {
		enables++;
		for (int i : neighbors[v]) {
			mask[i][c]--;
		}
		for (int i = 0; i < 9; i++) {
			mask[v][i]--;
		}
		// for (int i : rowNeighbors[v]) {
			// rowMask[i][c]--;
		// }
		// for (int i : colNeighbors[v]) {
			// colMask[i][c]--;
		// }
		// for (int i : boxNeighbors[v]) {
			// boxMask[i][c]--;
		// }
	}


	private void disablea(int v, int c) {
		for (int i = 0; i < V; i++) {
			if (graph[v][i] == 1) {
				disables++;
				mask[i][c]++;
			}
		}
	}

	private void enablea(int v, int c) {
		for (int i = 0; i < V; i++) {
			if (graph[v][i] == 1) {
				enables++;
				mask[i][c]--;
			}
		}
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
		graph = new int[81][81];
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
		int[][] cells = {{0 ,1 ,2 ,9 ,10,11,18,19,20},
						 {3 ,4 ,5 ,12,13,14,21,22,23},
						 {6 ,7 ,8 ,15,16,17,24,25,26},
						 {27,28,29,36,37,38,45,46,47},
						 {30,31,32,39,40,41,48,49,50},
						 {33,34,35,42,43,44,51,52,53},
						 {54,55,56,63,64,65,72,73,74},
						 {57,58,59,66,67,68,75,76,77},
						 {60,61,62,69,70,71,78,79,80}};

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
					if (!contains(neighbors[i], j)) {
						neighbors[i][n_count[i]++] = j;
					}
					// if (!contains(neighbors[j], i)) {
						// neighbors[j][n_count[j]++] = i;
					// }
					rowNeighbors[i][n_rowCount[i]++] = j;
					// rowNeighbors[j][n_rowCount[j]++] = i;
				}
			}
			for (int j = i%9; j < 81; j += 9) {
				if (i != j) {
					graph[i][j] = graph[j][i] = 1;
					if (!contains(neighbors[i], j)) {
						neighbors[i][n_count[i]++] = j;
					}
					// if (!contains(neighbors[j], i)) {
						// neighbors[j][n_count[j]++] = i;
					// }
					colNeighbors[i][n_colCount[i]++] = j;
					// colNeighbors[j][n_colCount[j]++] = i;
				}
			}
			for (int j : map.get(i)) {
				if (i != j) {
					graph[i][j] = graph[j][i] = 1;
					if (!contains(neighbors[i], j)) {
						neighbors[i][n_count[i]++] = j;
					}
					// if (!contains(neighbors[j], i)) {
						// neighbors[j][n_count[j]++] = i;
					// }
					boxNeighbors[i][n_boxCount[i]++] = j;
					// boxNeighbors[j][n_boxCount[j]++] = i;
				}
			}
		}
		// System.out.println(Arrays.toString(n_count));
		// System.out.println(Arrays.toString(rowNeighbors[5]));
		// System.out.println(Arrays.toString(neighbors[1]));
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
			if (tempTime - gc.lastPrint > 10_000_000) {
				gc.shouldPrint = true;
				gc.lastPrint = tempTime;
				System.out.print(String.format(
					"\r(%7d/%7d) ", i+1, board.length));
			}
			gc.graphColor(9, board[i], gc.clues[i]);
			p.println(gc.getSolution());
			// break;
			// if (i == 1) {
				// break;
			// }
		}
		p.close();
		System.out.println();
		System.out.println(String.format("disables: %d\nenables: %d\nchecks: %d\noneCount: %d\noneSuccess: %d\noneCancel: %d", 
			gc.disables, gc.enables, gc.checks, gc.oneCount, gc.oneSuccess, gc.oneCancel));
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
