CC = gcc

CFLAGS = -Wall -Wextra -std=c17

SRC = \
src/main.c \
lexer/lexer.c \
parser/parser.c \
ast/ast.c \
src/keywords.c

TARGET = anthc

all:
	$(CC) $(CFLAGS) $(SRC) -o $(TARGET)

clean:
	rm -f $(TARGET)
