#include "parser.h"

#include <stdio.h>
#include <stdlib.h>

void parser_expect(Parser *parser, TokenType type)
{
    if (parser->current.type != type)
    {
        printf("Erro de sintaxe!\n");
        printf("Esperado token %d\n", type);
        printf("Recebido token %d\n", parser->current.type);
        exit(EXIT_FAILURE);
    }

    parser_advance(parser);
}

void parser_init(Parser *parser, char *source)
{
    lexer_init(&parser->lexer, source);

    parser->current = lexer_next_token(&parser->lexer);
}

void parser_advance(Parser *parser)
{
    parser->current = lexer_next_token(&parser->lexer);
}

int parser_is_type(TokenType type)
{
    switch(type)
    {
        case TOKEN_INT:
        case TOKEN_FLOAT:
        case TOKEN_DOUBLE:
        case TOKEN_BOOL:
        case TOKEN_CHAR_TYPE:
        case TOKEN_STRING_TYPE:
        case TOKEN_LONG:
        case TOKEN_SHORT:
        case TOKEN_BYTE:
            return 1;

        default:
            return 0;
    }
}

ASTNode *parse_library(Parser *parser)
{
    parser_expect(parser, TOKEN_LIBRARY);

    ASTNode *library = ast_create(AST_LIBRARY, parser->current.lexeme);

    parser_expect(parser, TOKEN_STRING);

    return library;
}

ASTNode *parse_function(Parser *parser)
{
    parser_expect(parser, TOKEN_FUNC);

    ASTNode *function = ast_create(AST_FUNCTION, parser->current.lexeme);

    parser_expect(parser, TOKEN_IDENTIFIER);

    parser_expect(parser, TOKEN_LPAREN);
    parser_expect(parser, TOKEN_RPAREN);

    ASTNode *block = parse_block(parser);

    ast_add_child(function, block);

    return function;
}

ASTNode *parse_program(Parser *parser)
{
    ASTNode *program = ast_create(AST_PROGRAM, "Program");

    ASTNode *library = parse_library(parser);

    ast_add_child(program, library);

    ASTNode *function = parse_function(parser);

    ast_add_child(program, function);

    return program;
}

ASTNode *parse_block(Parser *parser)
{
    ASTNode *block = ast_create(AST_BLOCK, "Block");

    parser_expect(parser, TOKEN_LBRACE);

    while(parser->current.type != TOKEN_RBRACE &&
          parser->current.type != TOKEN_EOF)
    {
        ASTNode *statement = parse_statement(parser);

        ast_add_child(block, statement);
    }

    parser_expect(parser, TOKEN_RBRACE);

    return block;
}

ASTNode *parse_variable_internal(Parser *parser,
                                 int expectSemicolon)
{
    ASTNode *variable =
        ast_create(AST_VARIABLE_DECLARATION, "Variable");

    /* Tipo */

    ASTNode *type =
        ast_create(AST_TYPE, parser->current.lexeme);

    ast_add_child(variable, type);

    parser_advance(parser);

    /* Nome */

    if(parser->current.type != TOKEN_IDENTIFIER)
    {
        printf("Erro: esperado identificador.\n");
        exit(EXIT_FAILURE);
    }

    ASTNode *name =
        ast_create(AST_IDENTIFIER, parser->current.lexeme);

    ast_add_child(variable, name);

    parser_advance(parser);

    /* = */

    parser_expect(parser, TOKEN_ASSIGN);

    /* Valor */

    ASTNode *value = parse_expression(parser);

    ast_add_child(variable, value);

    /* ; é opcional */

    if(expectSemicolon)
    {
        parser_expect(parser, TOKEN_SEMICOLON);
    }

    return variable;
}

ASTNode *parse_variable(Parser *parser)
{
    return parse_variable_internal(parser, 1);
}

ASTNode *parse_statement(Parser *parser)
{
    if(parser_is_type(parser->current.type))
    {
        return parse_variable(parser);
    }
    
    if (parser->current.type == TOKEN_IF)
    {	
    	return parse_if(parser);
    }
    
    if (parser->current.type == TOKEN_WHILE)
    {
    	return parse_while(parser);
    }
    
    if (parser->current.type == TOKEN_FOR)
    {
    	return parse_for(parser);
    }


    if(parser->current.type == TOKEN_IDENTIFIER)
    {
        return parse_print(parser);
    }

    printf("Erro: instrução desconhecida.\n");
    exit(EXIT_FAILURE);
}

ASTNode *parse_print(Parser *parser)
{
    parser_expect(parser, TOKEN_IDENTIFIER); // printf

    parser_expect(parser, TOKEN_DOT);

    parser_expect(parser, TOKEN_IDENTIFIER); // out

    parser_expect(parser, TOKEN_LPAREN);

    ASTNode *print = ast_create(AST_PRINT, "Print");

    ASTNode *argument = NULL;

    switch(parser->current.type)
    {
        case TOKEN_IDENTIFIER:
            argument = ast_create(AST_IDENTIFIER,
                                  parser->current.lexeme);
            break;

        case TOKEN_STRING:
            argument = ast_create(AST_STRING,
                                  parser->current.lexeme);
            break;

        case TOKEN_NUMBER:
            argument = ast_create(AST_NUMBER,
                                  parser->current.lexeme);
            break;

        default:
            printf("Erro: argumento inválido para printf.out().\n");
            exit(EXIT_FAILURE);
    }

    parser_advance(parser);

    ast_add_child(print, argument);

    parser_expect(parser, TOKEN_RPAREN);

    parser_expect(parser, TOKEN_SEMICOLON);

    return print;
}

ASTNode *parse_expression(Parser *parser)
{
    return parse_logical_or(parser);
}

ASTNode *parse_primary(Parser *parser)
{
    ASTNode *node = NULL;

    if(parser->current.type == TOKEN_LPAREN)
    {
        parser_advance(parser);

        node = parse_expression(parser);

        parser_expect(parser, TOKEN_RPAREN);

        return node;
    }

    switch(parser->current.type)
    {
        case TOKEN_NUMBER:

            node = ast_create(AST_NUMBER,
                              parser->current.lexeme);
            break;

        case TOKEN_IDENTIFIER:

            node = ast_create(AST_IDENTIFIER,
                              parser->current.lexeme);
            break;

        case TOKEN_STRING:

            node = ast_create(AST_STRING,
                              parser->current.lexeme);
            break;

        default:

            printf("Erro: expressão inválida.\n");
            exit(EXIT_FAILURE);
    }

    parser_advance(parser);

    return node;
}

ASTNode *parse_term(Parser *parser)
{
    return parse_primary(parser);
}


ASTNode *parse_additive(Parser *parser)
{
    ASTNode *left = parse_multiplicative(parser);

    while(parser->current.type == TOKEN_PLUS ||
          parser->current.type == TOKEN_MINUS)
    {
        TokenType op = parser->current.type;

        parser_advance(parser);

        ASTNode *right = parse_multiplicative(parser);

        ASTNode *node =
            ast_create(AST_BINARY_EXPRESSION,
                       op == TOKEN_PLUS ? "+" : "-");

        ast_add_child(node, left);
        ast_add_child(node, right);

        left = node;
    }

    return left;
}

ASTNode *parse_multiplicative(Parser *parser)
{
    ASTNode *left = parse_unary(parser);

    while(parser->current.type == TOKEN_STAR ||
          parser->current.type == TOKEN_SLASH ||
          parser->current.type == TOKEN_PERCENT)
    {
        TokenType op = parser->current.type;

        parser_advance(parser);

        ASTNode *right = parse_unary(parser);

        ASTNode *node;

        switch(op)
        {
            case TOKEN_STAR:
                node = ast_create(AST_BINARY_EXPRESSION, "*");
                break;

            case TOKEN_SLASH:
                node = ast_create(AST_BINARY_EXPRESSION, "/");
                break;

            default:
                node = ast_create(AST_BINARY_EXPRESSION, "%");
                break;
        }

        ast_add_child(node, left);
        ast_add_child(node, right);

        left = node;
    }

    return left;
}

ASTNode *parse_logical_or(Parser *parser)
{
    ASTNode *left = parse_logical_and(parser);

    while(parser->current.type == TOKEN_OR)
    {
        parser_advance(parser);

        ASTNode *right = parse_logical_and(parser);

        ASTNode *node =
            ast_create(AST_BINARY_EXPRESSION, "||");

        ast_add_child(node, left);
        ast_add_child(node, right);

        left = node;
    }

    return left;
}

ASTNode *parse_logical_and(Parser *parser)
{
    ASTNode *left = parse_equality(parser);

    while(parser->current.type == TOKEN_AND)
    {
        parser_advance(parser);

        ASTNode *right = parse_equality(parser);

        ASTNode *node =
            ast_create(AST_BINARY_EXPRESSION, "&&");

        ast_add_child(node, left);
        ast_add_child(node, right);

        left = node;
    }

    return left;
}

ASTNode *parse_unary(Parser *parser)
{
    if(parser->current.type == TOKEN_MINUS)
    {
        parser_advance(parser);

        ASTNode *node =
            ast_create(AST_UNARY_EXPRESSION, "-");

        ast_add_child(node,
                      parse_primary(parser));

        return node;
    }

    if(parser->current.type == TOKEN_NOT)
    {
        parser_advance(parser);

        ASTNode *node =
            ast_create(AST_UNARY_EXPRESSION, "!");

        ast_add_child(node,
                      parse_primary(parser));

        return node;
    }

    return parse_primary(parser);
}

ASTNode *parse_equality(Parser *parser)
{
    ASTNode *left = parse_relational(parser);

    while(parser->current.type == TOKEN_EQUAL ||
          parser->current.type == TOKEN_NOT_EQUAL)
    {
        TokenType op = parser->current.type;

        parser_advance(parser);

        ASTNode *right = parse_relational(parser);

        ASTNode *node =
            ast_create(AST_BINARY_EXPRESSION,
                       op == TOKEN_EQUAL ? "==" : "!=");

        ast_add_child(node, left);
        ast_add_child(node, right);

        left = node;
    }

    return left;
}

ASTNode *parse_relational(Parser *parser)
{
    ASTNode *left = parse_additive(parser);

    while(parser->current.type == TOKEN_LESS ||
          parser->current.type == TOKEN_LESS_EQUAL ||
          parser->current.type == TOKEN_GREATER ||
          parser->current.type == TOKEN_GREATER_EQUAL)
    {
        TokenType op = parser->current.type;

        parser_advance(parser);

        ASTNode *right = parse_additive(parser);

        ASTNode *node = NULL;

        switch(op)
        {
            case TOKEN_LESS:
                node = ast_create(AST_BINARY_EXPRESSION, "<");
                break;

            case TOKEN_LESS_EQUAL:
                node = ast_create(AST_BINARY_EXPRESSION, "<=");
                break;

            case TOKEN_GREATER:
                node = ast_create(AST_BINARY_EXPRESSION, ">");
                break;

            case TOKEN_GREATER_EQUAL:
                node = ast_create(AST_BINARY_EXPRESSION, ">=");
                break;

            default:
                break;
        }

        ast_add_child(node, left);
        ast_add_child(node, right);

        left = node;
    }

    return left;
}

ASTNode *parse_if(Parser *parser){

	parser_expect(parser, TOKEN_IF);
	
	ASTNode *ifNode = ast_create(AST_IF, "If");
	
	parser_expect(parser, TOKEN_LPAREN);
	
	ASTNode *condition = parse_expression(parser);
	
	ast_add_child(ifNode, condition);
	
	parser_expect(parser, TOKEN_RPAREN);
	
	ASTNode *block = parse_block(parser);
	
	ast_add_child(ifNode, block);
	
	if (parser->current.type == TOKEN_ELSE)
	{
		parser_advance(parser);

		ASTNode *elseNode = ast_create(AST_ELSE, "Else");

		ASTNode *elseBlock = parse_block(parser);

		ast_add_child(elseNode, elseBlock);

		ast_add_child(ifNode, elseNode);
	
	}
	
	return ifNode;

}

ASTNode *parse_while(Parser *parser){
	parser_expect(parser,TOKEN_WHILE);
	
	ASTNode *whileNode = ast_create(AST_WHILE, "While");
	
	parser_expect(parser, TOKEN_LPAREN);

	ASTNode *condition = parse_expression(parser);

	ast_add_child(whileNode, condition);
	
	parser_expect(parser, TOKEN_RPAREN);

	ASTNode *block = parse_block(parser);

	ast_add_child(whileNode, block);
	
	return whileNode;

}

ASTNode *parse_for(Parser *parser)
{
    parser_expect(parser, TOKEN_FOR);

    ASTNode *forNode = ast_create(AST_FOR, "For");

    parser_expect(parser, TOKEN_LPAREN);

    /* Inicialização */

    ASTNode *init = parse_variable_internal(parser, 0);

    ast_add_child(forNode, init);

    parser_expect(parser, TOKEN_SEMICOLON);

    /* Condição */

    ASTNode *condition = parse_expression(parser);

    ast_add_child(forNode, condition);

    parser_expect(parser, TOKEN_SEMICOLON);

    /* Incremento */

    ASTNode *increment = parse_expression(parser);

    ast_add_child(forNode, increment);

    parser_expect(parser, TOKEN_RPAREN);

    ASTNode *block = parse_block(parser);

    ast_add_child(forNode, block);

    return forNode;
}
