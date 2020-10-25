import ctypes
from typing import List, Optional
from pydantic import BaseModel, Field, validator
from fastapi import FastAPI, Query
from fastapi.responses import HTMLResponse
# import multiprocessing
import threading


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
		self.lock = threading.Lock()
		# threads = multiprocessing.cpu_count()
		# self.obj = lib.ParallelSolver_new(threads)

	def load_files(self):
		lib.load_files(self.obj)

	def solve(self, strs):
		byte_strs = list(map(lambda s: s.encode("utf-8"), strs))
		string_pointers = (ctypes.c_char_p * len(byte_strs))(*byte_strs)

		self.lock.acquire()
		try:
			lib.solve_sudokus(self.obj, string_pointers, len(strs))
		finally:
			self.lock.release()

		# lib.solve_sudokus_parallel(self.obj, string_pointers, len(strs))
		return list(map(lambda b: b.decode("utf-8"), byte_strs))

	def free_pointer(self, p):
		lib.free_pointer(p)

searcher = Sudoku()


class SolveSudokusInput(BaseModel):
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

class SolveSudokusOutput(BaseModel):
	solved_sudokus : List[str] = Field(example = [
		("4726538191387925649561483726945312872"
			"85479136317286945521864793763915428849327651"),
		("4382971656594312787215863491679248535"
			"42873916893615427974168532385742691216359784")
	  	],
		description = "A list of solved sudokus.")

class SolveSudokuInput(BaseModel):
	sudoku : str = Field(
		example = ("0020000090300025000061003700000000002"
			"00400130007006040001800000760005400009007600"),
		description = ("A list of unsolved sudokus.")
	)

	@validator('sudoku')
	def check_names_not_empty(cls, v):
		if len(v) != 81:
			raise ValueError("Length of sudoku string must be exactly 81")
		return v

class SolveSudokuOutput(BaseModel):
	solved_sudoku : str = Field(
		example = ("4726538191387925649561483726945312872"
			"85479136317286945521864793763915428849327651"),
		description = "A solved sudokus.")


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

@app.post('/solve-sudokus/', 
	response_model = SolveSudokusOutput)
def solve_sudokus(input_model : SolveSudokusInput,
		) -> SolveSudokusOutput:
	"""
		Solve a list of sudokus, passed as a json body.
	"""
	sudokus = input_model.sudokus
	res = searcher.solve(sudokus)
	output_model = SolveSudokusOutput(solved_sudokus = res)
	return output_model


def generate_table(sudoku):
	row = """<tr>
	    <th class="tg-5kwd">%c</th>
	    <th class="tg-5kwd">%c</th>
	    <th class="tg-5kwd">%c</th>
	    <th class="tg-cly1">%c</th>
	    <th class="tg-cly1">%c</th>
	    <th class="tg-cly1">%c</th>
	    <th class="tg-cly1">%c</th>
	    <th class="tg-cly1">%c</th>
	    <th class="tg-cly1">%c</th>
	  </tr>
	  """
	s = """
	<style type="text/css">
	.tg  {border-collapse:collapse;border-spacing:0;}
	.tg td{border-color:black;border-style:solid;border-width:1px;font-family:Arial, sans-serif;font-size:14px;
	  overflow:hidden;padding:5px 5px;word-break:normal;}
	.tg th{border-color:black;border-style:solid;border-width:1px;font-family:Arial, sans-serif;font-size:14px;
	  font-weight:normal;overflow:hidden;padding:5px 5px;word-break:normal;}
	.tg .tg-cly1{text-align:left;vertical-align:middle}
	.tg .tg-5kwd{border-color:inherit;font-family:"Courier New", Courier, monospace !important;;text-align:center;vertical-align:middle}
	</style>
	<table class="tg">
	<thead>
		%s
	</thead>
	<tbody>
		%s
		%s
		%s
		%s
		%s
		%s
		%s
		%s
	</tbody>
	</table>
	"""
	sudoku_rows = [sudoku[9*i:9*(i+1)] for i in range(9)]
	table_rows = list(map(lambda r: row % tuple(r), sudoku_rows))
	html_table = s % tuple(table_rows)
	return f"<html>{html_table}</html>"

@app.get('/solve-sudoku/', response_class = HTMLResponse)
async def solve_sudoku(
		sudoku : Optional[str] = Query(None, regex="^[0-9]{81}$"),
		json : Optional[str] = Query(None)):
	"""
		Solve a single sudoku, passed as a query parameter.
	"""
	sudokus = [sudoku]
	res = searcher.solve(sudokus)
	if json:
		output_model = SolveSudokuOutput(solved_sudoku = res[0])
		return output_model
	else:
		return generate_table(res[0])

