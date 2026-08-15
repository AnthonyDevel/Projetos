#ifndef AST_H
#define AST_H
#define AST_MAX_CHILDREN 32

typedef enum
{

    AST_TYPE,
    AST_VALUE,
    
    AST_CALL,
    AST_ARGUMENT,
    AST_IF,
    AST_ELSE,
    AST_WHILE,
    AST_FOR,
    AST_RETURN,
    AST_EXPRESSION,

    AST_PROGRAM,

    AST_LIBRARY,

    AST_FUNCTION,

    AST_BLOCK,

    AST_VARIABLE_DECLARATION,

    AST_IDENTIFIER,

    AST_NUMBER,

    AST_STRING,

    AST_CHAR,
    
    AST_BINARY_EXPRESSION,
    
    AST_UNARY_EXPRESSION,

    AST_PRINT

} ASTNodeType;

typedef struct ASTNode
{
    ASTNodeType type;

    char value[256];

    struct ASTNode *children[AST_MAX_CHILDREN];

    int child_count;

} ASTNode;

ASTNode *ast_create(ASTNodeType type, const char *value);

void ast_print(ASTNode *node, int level);

void ast_add_child(ASTNode *parent, ASTNode *child);

void ast_destroy(ASTNode *node);

#endif
