#include "ast.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

ASTNode *ast_create(ASTNodeType type, const char *value)
{
    ASTNode *node = malloc(sizeof(ASTNode));

    if (node == NULL)
        return NULL;

    node->type = type;

    strcpy(node->value, value);

    node->child_count = 0;

    for (int i = 0; i < AST_MAX_CHILDREN; i++)
        node->children[i] = NULL;

    return node;
}

void ast_add_child(ASTNode *parent, ASTNode *child)
{
    if (parent == NULL || child == NULL)
        return;

    if (parent->child_count < AST_MAX_CHILDREN)
    {
        parent->children[parent->child_count] = child;
        parent->child_count++;
    }
}

void ast_print(ASTNode *node, int level)
{
    if (node == NULL)
        return;

    for (int i = 0; i < level; i++)
        printf("    ");

    printf("%s\n", node->value);

    for (int i = 0; i < node->child_count; i++)
    {
        ast_print(node->children[i], level + 1);
    }
}

void ast_destroy(ASTNode *node)
{
    if (node == NULL)
        return;

    for (int i = 0; i < node->child_count; i++)
    {
        ast_destroy(node->children[i]);
    }

    free(node);
}
