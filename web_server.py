import ctypes
from typing import List
from pydantic import BaseModel, Field, validator
from fastapi import FastAPI
# import multiprocessing

lib = ctypes.CDLL('./sudoku_solver.so')
lib.Sudoku_new.restype = ctypes.c_void_p
lib.ParallelSolver_new.restype = ctypes.c_void_p
lib.solve_sudokus.argtypes = (
	ctypes.c_void_p, ctypes.POINTER(ctypes.c_char_p), ctypes.c_int)
lib.solve_sudokus.restype = ctypes.c_void_p

lib.solve_sudokus_parallel.argtypes = (
	ctypes.c_void_p, ctypes.POINTER(ctypes.c_char_p), ctypes.c_int)
lib.solve_sudokus_parallel.restype = ctypes.c_void_p


class Sudoku(object):
	def __init__(self):

		self.obj = lib.Sudoku_new()
		# threads = multiprocessing.cpu_count()
		# self.obj = lib.ParallelSolver_new(threads)

	def load_files(self):
		lib.load_files(self.obj)

	def solve(self, strs):
		byte_strs = list(map(lambda s: s.encode("utf-8"), strs))
		string_pointers = (ctypes.c_char_p * len(byte_strs))(*byte_strs)
		lib.solve_sudokus(self.obj, string_pointers, len(strs))
		# lib.solve_sudokus_parallel(self.obj, string_pointers, len(strs))
		return list(map(lambda b: b.decode("utf-8"), byte_strs))

	def free_pointer(self, p):
		lib.free_pointer(p)

searcher = Sudoku()


class SolveSudokuInput(BaseModel):
	sudokus : List[str] = Field(example = [
		("0020000090300025000061003700000000002"
			"00400130007006040001800000760005400009007600"),
		("0300000000000010087005800000000240500"
			"40873900003600000900000002005002091200000704")
		], 
		description = ("A list of unsolved sudokus.")
	)

	@validator('sudokus', each_item=True)
	def check_names_not_empty(cls, v):
		if len(v) != 81:
			raise ValueError("Length of sudoku string must be exactly 81")
		return v

class SolveSudokuOutput(BaseModel):
	solved_sudokus : List[str] = Field(example = [
		("4726538191387925649561483726945312872"
			"85479136317286945521864793763915428849327651"),
		("4382971656594312787215863491679248535"
			"42873916893615427974168532385742691216359784")
	  	],
		description = "A list of solved sudokus.")


class HelloWorldOutput(BaseModel):
	message : str = Field(example = "hello, world!",
		description = "Simple 'hello, world!' message.")

app = FastAPI()

@app.get('/', response_model = HelloWorldOutput)
def hello() -> HelloWorldOutput:
	"""
		Root endpoint for testing if the API is running. Always returns a
		simple "hello, world!" response. 
	"""
	return HelloWorldOutput(message = "hello, world!")

@app.post('/solve-sudokus', 
	response_model = SolveSudokuOutput)
def solve_sudokus(input_model : SolveSudokuInput,
		) -> SolveSudokuOutput:
	"""
		Get all memberships for a single golf club, along with statistical
		measurements related to the players in the membership.
	"""
	sudokus = input_model.sudokus
	res = searcher.solve(sudokus)
	output_model = SolveSudokuOutput(solved_sudokus = res)
	return output_model

