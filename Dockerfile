FROM python:3.7

RUN pip install fastapi uvicorn

EXPOSE 8080

COPY ./sudoku_solver.so /sudoku_solver.so
COPY ./web_server.py /app/web_server.py

CMD ["uvicorn", "app.web_server:app", "--host=0.0.0.0", "--port=8080", "--reload"]
