#include <vector>
#include <string>

class Sudoku {
private:
	std::vector<char> unsolvedBoard;
	std::vector<char> solvedBoard; 
	std::vector<std::vector<char>> neighbors;
	std::vector<std::vector<char>> cells;
	std::vector<std::vector<char>> mask;
	std::vector<int> formattedMask;
	std::vector<std::vector<char>> placedMask;
	std::vector<std::vector<std::vector<bool>>> lineMask;
	std::vector<bool> lineCounters;
	std::vector<std::vector<char>> sectionCounters;
	std::vector<std::vector<char>> sectionMask;
	std::vector<std::vector<char>> tempRowMask;
	std::vector<std::vector<char>> tempColMask;


	bool isEasy;
	int totEasy;
	int placedNumbers;
	bool solutionFound;
	bool shouldPrint;
	bool isImpossible = false;
	static std::vector<int> clues;

	void solve(int v, int vIndex);
	void resetEasy(int vIndex, int easyIndex);
	void resetLineMask(int vIndex);
	int placeEasy(int vIndex);
	int placeNakedSingles(int vIndex, int easyIndex);
	int placeNakedSingles2(int vIndex, int easyIndex);
	int placeHiddenSingles(int vIndex, int easyIndex);
	int getFormattedMask(int v);
	int getCachedMask(int v);
	void generateFormattedMasks();
	void generateFormattedMasks(std::vector<char> &idxs);
	void checkNakedDoubles(int vIndex);
	void checkNakedTriples(int vIndex);
	void identifyLines(int vIndex);
	bool isPossible(int v, int c);
	void put(int v, int c);
	void disable(int v, int c);
	void unput(int v, int c);
	void enable(int v, int c);
	int getCell(int v);
	bool contains(std::vector<char> &a, int v);
	int getTotEasy();


public:

	long totTime;
	long lastPrint;
	int easySolved;
	int totalSolved;
	long guesses;
	Sudoku();
	void solveSudoku(std::vector<char> &board);
	std::string getString(std::vector<char> &board);
	long getTime();
	void display(std::vector<char> &board);
	void display2(std::vector<char> &board, std::vector<char> &solved);
	void connect();
	std::string getSolution();
	static std::string printTime(long t1, long t2);
	static std::vector<std::vector<char>> getInput(std::string filename);
};