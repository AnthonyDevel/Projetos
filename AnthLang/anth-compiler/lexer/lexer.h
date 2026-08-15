#ifndef LEXER_H
#define LEXER_H

#include "../include/token.h"

typedef struct
{
    char *source;

    int position;

    int line;

    int column;

} Lexer;

// Só depois vêm os protótipos

void lexer_init(Lexer *lexer, char *source);

char lexer_current(Lexer *lexer);
char lexer_peek(Lexer *lexer);
void lexer_advance(Lexer *lexer);

void lexer_skip_whitespace(Lexer *lexer);

void lexer_skip_line_comment(Lexer *lexer);

void lexer_skip_block_comment(Lexer *lexer);

Token lexer_char(Lexer *lexer);

Token lexer_identifier(Lexer *lexer);

Token lexer_next_token(Lexer *lexer);

Token lexer_string(Lexer *lexer);

Token lexer_make_token(Lexer *lexer, TokenType type, const char *lexeme);

Token lexer_number(Lexer *lexer);

#endif  

