#ifndef PARSER_H
#define PARSER_H

#include "../lexer/lexer.h"
#include "../ast/ast.h"

typedef struct
{
    Lexer lexer;

    Token current;

} Parser;

void parser_init(Parser *parser, char *source);

void parser_advance(Parser *parser);

void parser_expect(Parser *parser, TokenType type);

int parser_is_type(TokenType type);

ASTNode *parse_library(Parser *parser);

ASTNode *parse_program(Parser *parser);

ASTNode *parse_function(Parser *parser);

ASTNode *parse_block(Parser *parser);

ASTNode *parse_variable(Parser *parser);

ASTNode *parse_statement(Parser *parser);

ASTNode *parse_variable(Parser *parser);

ASTNode *parse_print(Parser *parser);

ASTNode *parse_expression(Parser *parser);

ASTNode *parse_term(Parser *parser);

ASTNode *parse_factor(Parser *parser);

ASTNode *parse_expression(Parser *parser);

ASTNode *parse_logical_or(Parser *parser);

ASTNode *parse_logical_and(Parser *parser);

ASTNode *parse_equality(Parser *parser);

ASTNode *parse_relational(Parser *parser);

ASTNode *parse_additive(Parser *parser);

ASTNode *parse_multiplicative(Parser *parser);

ASTNode *parse_unary(Parser *parser);

ASTNode *parse_primary(Parser *parser);

ASTNode *parse_if(Parser *parser);

ASTNode *parse_while(Parser *parser);

ASTNode *parse_for(Parser *parser);

ASTNode *parse_variable_internal(Parser *parser, int expectSemicolon);

#endif
