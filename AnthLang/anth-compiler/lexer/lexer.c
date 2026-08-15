#include "lexer.h"
#include <ctype.h>
#include <string.h>

#include "../include/keywords.h"

void lexer_init(Lexer *lexer, char *source)
{
    lexer->source = source;
    lexer->position = 0;
    lexer->line = 1;
    lexer->column = 1;
}

char lexer_current(Lexer *lexer)
{
    return lexer->source[lexer->position];
}

char lexer_peek(Lexer *lexer)
{
    return lexer->source[lexer->position + 1];
}

void lexer_advance(Lexer *lexer)
{
    if (lexer->source[lexer->position] == '\n')
    {
        lexer->line++;
        lexer->column = 1;
    }
    else
    {
        lexer->column++;
    }

    lexer->position++;
}

void lexer_skip_whitespace(Lexer *lexer)
{
    while (lexer_current(lexer) == ' ' ||
           lexer_current(lexer) == '\t' ||
           lexer_current(lexer) == '\n' ||
           lexer_current(lexer) == '\r')
    {
        lexer_advance(lexer);
    }
}

void lexer_skip_line_comment(Lexer *lexer)
{
    while (lexer_current(lexer) != '\n' &&
           lexer_current(lexer) != '\0')
    {
        lexer_advance(lexer);
    }
}

void lexer_skip_block_comment(Lexer *lexer)
{
    lexer_advance(lexer); // *
    
    while (lexer_current(lexer) != '\0')
    {
        if (lexer_current(lexer) == '*' &&
            lexer_peek(lexer) == '/')
        {
            lexer_advance(lexer);
            lexer_advance(lexer);
            break;
        }

        lexer_advance(lexer);
    }
}



Token lexer_identifier(Lexer *lexer)
{
    Token token;

    int i = 0;

    while (isalnum(lexer_current(lexer)) || lexer_current(lexer) == '_')
    {
        token.lexeme[i++] = lexer_current(lexer);
        lexer_advance(lexer);
    }

    token.lexeme[i] = '\0';

    token.line = lexer->line;
    token.column = lexer->column;

    token.type = TOKEN_IDENTIFIER;

    for (int i = 0; i < keyword_count; i++)
    {
        if (strcmp(token.lexeme, keywords[i].word) == 0)
        {
            token.type = keywords[i].token;
            break;
        }
    }

    return token;
}

Token lexer_string(Lexer *lexer)
{
    Token token;

    lexer_advance(lexer);

    int i = 0;

    while (lexer_current(lexer) != '"' &&
           lexer_current(lexer) != '\0')
    {
        token.lexeme[i++] = lexer_current(lexer);
        lexer_advance(lexer);
    }

    token.lexeme[i] = '\0';

    if (lexer_current(lexer) == '"')
        lexer_advance(lexer);

    token.type = TOKEN_STRING;
    token.line = lexer->line;
    token.column = lexer->column;

    return token;
}

Token lexer_char(Lexer *lexer)
{
    Token token;

    lexer_advance(lexer); // pula '

    token.lexeme[0] = lexer_current(lexer);
    token.lexeme[1] = '\0';

    lexer_advance(lexer);

    if (lexer_current(lexer) == '\'')
        lexer_advance(lexer);

    token.type = TOKEN_CHAR;
    token.line = lexer->line;
    token.column = lexer->column;

    return token;
}

Token lexer_number(Lexer *lexer)
{
    Token token;

    int i = 0;

    while (isdigit(lexer_current(lexer)))
    {
        token.lexeme[i++] = lexer_current(lexer);
        lexer_advance(lexer);
    }

    token.lexeme[i] = '\0';

    token.type = TOKEN_NUMBER;
    token.line = lexer->line;
    token.column = lexer->column;

    return token;
}

Token lexer_next_token(Lexer *lexer)
{
    lexer_skip_whitespace(lexer);

    if (isalpha(lexer_current(lexer)) || lexer_current(lexer) == '_')
        return lexer_identifier(lexer);

    if (isdigit(lexer_current(lexer)))
        return lexer_number(lexer);

    if (lexer_current(lexer) == '"')
        return lexer_string(lexer);
        
    if (lexer_current(lexer) == '\'')
    return lexer_char(lexer);
        
    if (lexer_current(lexer) == '/' &&
    lexer_peek(lexer) == '/')
{
    lexer_advance(lexer);
    lexer_advance(lexer);

    lexer_skip_line_comment(lexer);

    return lexer_next_token(lexer);
}

if (lexer_current(lexer) == '/' &&
    lexer_peek(lexer) == '*')
{
    lexer_advance(lexer);
    lexer_advance(lexer);

    lexer_skip_block_comment(lexer);

    return lexer_next_token(lexer);
}

    switch (lexer_current(lexer))
    {
        case '(':
            lexer_advance(lexer);
            return lexer_make_token(lexer, TOKEN_LPAREN, "(");

        case ')':
            lexer_advance(lexer);
            return lexer_make_token(lexer, TOKEN_RPAREN, ")");

        case '{':
            lexer_advance(lexer);
            return lexer_make_token(lexer, TOKEN_LBRACE, "{");

        case '}':
            lexer_advance(lexer);
            return lexer_make_token(lexer, TOKEN_RBRACE, "}");

        case '[':
            lexer_advance(lexer);
            return lexer_make_token(lexer, TOKEN_LBRACKET, "[");

        case ']':
            lexer_advance(lexer);
            return lexer_make_token(lexer, TOKEN_RBRACKET, "]");

        case '.':
            lexer_advance(lexer);
            return lexer_make_token(lexer, TOKEN_DOT, ".");

        case ',':
            lexer_advance(lexer);
            return lexer_make_token(lexer, TOKEN_COMMA, ",");

        case ';':
            lexer_advance(lexer);
            return lexer_make_token(lexer, TOKEN_SEMICOLON, ";");

        case '=':

	    if (lexer_peek(lexer) == '=')
	    {
		lexer_advance(lexer);
		lexer_advance(lexer);
		return lexer_make_token(lexer, TOKEN_EQUAL, "==");
	    }

	    lexer_advance(lexer);
    	    return lexer_make_token(lexer, TOKEN_ASSIGN, "=");
    	    
    	case '!':

	    if (lexer_peek(lexer) == '=')
	    {
		lexer_advance(lexer);
		lexer_advance(lexer);
		return lexer_make_token(lexer, TOKEN_NOT_EQUAL, "!=");
	    }

	    lexer_advance(lexer);
	    return lexer_make_token(lexer, TOKEN_NOT, "!");
	    
	case '<':

	    if (lexer_peek(lexer) == '=')
	    {
		lexer_advance(lexer);
		lexer_advance(lexer);
		return lexer_make_token(lexer, TOKEN_LESS_EQUAL, "<=");
	    }

	    lexer_advance(lexer);
	    return lexer_make_token(lexer, TOKEN_LESS, "<");
    	
    	case '>':

	    if (lexer_peek(lexer) == '=')
	    {
		lexer_advance(lexer);
		lexer_advance(lexer);
		return lexer_make_token(lexer, TOKEN_GREATER_EQUAL, ">=");
	    }

	    lexer_advance(lexer);
	    return lexer_make_token(lexer, TOKEN_GREATER, ">");
	
	case '+':

	    if (lexer_peek(lexer) == '+')
	    {
		lexer_advance(lexer);
		lexer_advance(lexer);
		return lexer_make_token(lexer, TOKEN_INCREMENT, "++");
	    }

	    if (lexer_peek(lexer) == '=')
	    {
		lexer_advance(lexer);
		lexer_advance(lexer);
		return lexer_make_token(lexer, TOKEN_PLUS_ASSIGN, "+=");
	    }

	    lexer_advance(lexer);
	    return lexer_make_token(lexer, TOKEN_PLUS, "+");
	
	case '-':

	    if (lexer_peek(lexer) == '-')
	    {
		lexer_advance(lexer);
		lexer_advance(lexer);
		return lexer_make_token(lexer, TOKEN_DECREMENT, "--");
	    }

	    if (lexer_peek(lexer) == '=')
	    {
		lexer_advance(lexer);
		lexer_advance(lexer);
		return lexer_make_token(lexer, TOKEN_MINUS_ASSIGN, "-=");
	    }

	    lexer_advance(lexer);
	    return lexer_make_token(lexer, TOKEN_MINUS, "-");
	    
	case '*':

	    if (lexer_peek(lexer) == '=')
	    {
		lexer_advance(lexer);
		lexer_advance(lexer);
		return lexer_make_token(lexer, TOKEN_STAR_ASSIGN, "*=");
	    }

	    lexer_advance(lexer);
	    return lexer_make_token(lexer, TOKEN_STAR, "*");
	
	case '/':

	    if (lexer_peek(lexer) == '=')
	    {
		lexer_advance(lexer);
		lexer_advance(lexer);
		return lexer_make_token(lexer, TOKEN_SLASH_ASSIGN, "/=");
	    }

	    lexer_advance(lexer);
	    return lexer_make_token(lexer, TOKEN_SLASH, "/");
    	
    }

    if (lexer_current(lexer) == '\0')
    {
        return lexer_make_token(lexer, TOKEN_EOF, "EOF");
    }

    lexer_advance(lexer);
    return lexer_make_token(lexer, TOKEN_UNKNOWN, "UNKNOWN");
}

Token lexer_make_token(Lexer *lexer, TokenType type, const char *lexeme)
{
    Token token;

    token.type = type;
    token.line = lexer->line;
    token.column = lexer->column;

    strcpy(token.lexeme, lexeme);

    return token;
}
