FROM python:3.8-alpine


EXPOSE 8080

COPY ./compile_shared_lib.sh /compile_shared_lib.sh
COPY ./requirements.txt /requirements.txt
RUN chmod +x ./compile_shared_lib.sh
COPY ./sudoku.h /sudoku.h
COPY ./sudoku.cpp /sudoku.cpp
COPY ./web_server.py /web_server.py

RUN apk add build-base
RUN pip install -U setuptools pip
RUN pip install -r requirements.txt

ENV PYTHONFAULTHANDLER=1

# RUN apt update
# RUN apt install g++
RUN ./compile_shared_lib.sh
# RUN g++ --version

CMD ["catchsegv", "uvicorn", "web_server:app", "--host=0.0.0.0", "--port=8080", "--reload"]
