# g++ -I../include -O3 -fopenmp -shared -Wl,-soname,sudoku_solver -o sudoku_solver.so -fPIC sudoku.cpp
g++ -O3 -fopenmp -shared -Wl,-soname,sudoku_solver -o sudoku_solver.so -fPIC sudoku.cpp