#ifndef TOKEN_H
#define TOKEN_H

typedef enum
{
    // ===============================
    // Especiais
    // ===============================
    TOKEN_UNKNOWN,
    TOKEN_EOF,

    // ===============================
    // Palavras-chave
    // ===============================
    TOKEN_LIBRARY,
    TOKEN_FUNC,
    TOKEN_CLASS,
    TOKEN_STRUCT,
    TOKEN_INTERFACE,
    
    // Primitive Types
    TOKEN_INT,
    TOKEN_FLOAT,
    TOKEN_DOUBLE,
    TOKEN_BOOL,
    TOKEN_CHAR_TYPE,
    TOKEN_STRING_TYPE,
    TOKEN_LONG,
    TOKEN_SHORT,
    TOKEN_BYTE,

    TOKEN_PUBLIC,
    TOKEN_PRIVATE,
    TOKEN_PROTECTED,

    TOKEN_STATIC,
    TOKEN_FINAL,
    TOKEN_ABSTRACT,
    TOKEN_MODIFICATION,

    TOKEN_ASYNC,

    TOKEN_IF,
    TOKEN_ELSE,
    TOKEN_FOR,
    TOKEN_WHILE,
    TOKEN_DO,
    TOKEN_SWITCH,
    TOKEN_CASE,
    TOKEN_DEFAULT,
    TOKEN_BREAK,
    TOKEN_CONTINUE,
    TOKEN_RETURN,

    TOKEN_TRY,
    TOKEN_CATCH,
    TOKEN_THROW,

    TOKEN_DEDUCT,
    TOKEN_CONCLUDE,

    TOKEN_TRUE,
    TOKEN_FALSE,
    TOKEN_NULL,

    // ===============================
    // Literais
    // ===============================
    TOKEN_IDENTIFIER,
    TOKEN_NUMBER,
    TOKEN_STRING,
    TOKEN_CHAR,

    // ===============================
    // Operadores aritméticos
    // ===============================
    TOKEN_PLUS,          // +
    TOKEN_MINUS,         // -
    TOKEN_STAR,          // *
    TOKEN_SLASH,         // /
    TOKEN_PERCENT,       // %

    TOKEN_INCREMENT,     // ++
    TOKEN_DECREMENT,     // --

    // ===============================
    // Operadores de atribuição
    // ===============================
    TOKEN_ASSIGN,            // =
    TOKEN_PLUS_ASSIGN,       // +=
    TOKEN_MINUS_ASSIGN,      // -=
    TOKEN_STAR_ASSIGN,       // *=
    TOKEN_SLASH_ASSIGN,      // /=
    TOKEN_PERCENT_ASSIGN,    // %=

    // ===============================
    // Operadores relacionais
    // ===============================
    TOKEN_EQUAL,             // ==
    TOKEN_NOT_EQUAL,         // !=

    TOKEN_GREATER,           // >
    TOKEN_GREATER_EQUAL,     // >=

    TOKEN_LESS,              // <
    TOKEN_LESS_EQUAL,        // <=

    // ===============================
    // Operadores lógicos
    // ===============================
    TOKEN_AND,               // &&
    TOKEN_OR,                // ||
    TOKEN_NOT,               // !

    // ===============================
    // Operadores bit a bit
    // ===============================
    TOKEN_BIT_AND,           // &
    TOKEN_BIT_OR,            // |
    TOKEN_BIT_XOR,           // ^
    TOKEN_BIT_NOT,           // ~

    TOKEN_SHIFT_LEFT,        // <<
    TOKEN_SHIFT_RIGHT,       // >>

    // ===============================
    // Delimitadores
    // ===============================
    TOKEN_LPAREN,            // (
    TOKEN_RPAREN,            // )

    TOKEN_LBRACE,            // {
    TOKEN_RBRACE,            // }

    TOKEN_LBRACKET,          // [
    TOKEN_RBRACKET,          // ]

    // ===============================
    // Separadores
    // ===============================
    TOKEN_COMMA,             // ,
    TOKEN_DOT,               // .
    TOKEN_COLON,             // :
    TOKEN_DOUBLE_COLON,      // ::
    TOKEN_SEMICOLON,         // ;
    TOKEN_ARROW,             // ->
    TOKEN_AT,                // @

} TokenType;

typedef struct
{
    TokenType type;

    char lexeme[256];

    int line;
    int column;

} Token;

#endif
