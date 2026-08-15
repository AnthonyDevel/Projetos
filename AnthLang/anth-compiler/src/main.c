#include <stdio.h>
#include <stdlib.h>

#include "../lexer/lexer.h"
#include "../parser/parser.h"
#include "../ast/ast.h"

char *load_file(const char *filename)
{
    FILE *file = fopen(filename, "rb");

    if(file == NULL)
    {
        printf("Erro: não foi possível abrir '%s'\n", filename);
        return NULL;
    }

    fseek(file, 0, SEEK_END);
    long size = ftell(file);
    rewind(file);

    char *buffer = malloc(size + 1);

    if(buffer == NULL)
    {
        printf("Erro: memória insuficiente.\n");
        fclose(file);
        return NULL;
    }

    fread(buffer, 1, size, file);

    buffer[size] = '\0';

    fclose(file);

    return buffer;
}

int main(int argc, char *argv[])
{
    printf("=================================\n");
    printf(" AnthLang Compiler 0.0.1\n");
    printf(" Semântica, desempenho e confiança.\n");
    printf("=================================\n\n");

    if(argc < 2)
    {
        printf("Uso:\n");
        printf("./anthc arquivo.anth\n");
        return 1;
    }

    printf("Arquivo recebido: %s\n\n", argv[1]);

    char *source = load_file(argv[1]);

    if(source == NULL)
        return 1;

    Lexer lexer;

    lexer_init(&lexer, source);
    
    Token token;
    
    Parser parser;

    parser_init(&parser, source);

    ASTNode *tree = parse_program(&parser);

    ast_print(tree, 0);
    
    printf("============= TOKENS =============");
    
    do{
    	token = lexer_next_token(&lexer);
    	printf("%d\t%s\n", token.type, token.lexeme);
    		
    
    }
    while (token.type != TOKEN_EOF);

    printf("Arquivo carregado com sucesso!\n");

    free(source);

    return 0;
}
