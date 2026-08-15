#include "../include/keywords.h"

Keyword keywords[] =
{
    {"library", TOKEN_LIBRARY},
    {"func", TOKEN_FUNC},
    {"class", TOKEN_CLASS},
    {"struct", TOKEN_STRUCT},
    {"interface", TOKEN_INTERFACE},
    
    { "int", TOKEN_INT },
    { "float", TOKEN_FLOAT },
    { "double", TOKEN_DOUBLE },
    { "bool", TOKEN_BOOL },
    { "char", TOKEN_CHAR_TYPE },
    { "string", TOKEN_STRING_TYPE },
    { "long", TOKEN_LONG },
    { "short", TOKEN_SHORT },
    { "byte", TOKEN_BYTE },

    {"public", TOKEN_PUBLIC},
    {"private", TOKEN_PRIVATE},
    {"protected", TOKEN_PROTECTED},

    {"static", TOKEN_STATIC},
    {"abstract", TOKEN_ABSTRACT},
    {"final", TOKEN_FINAL},

    {"modification", TOKEN_MODIFICATION},

    {"async", TOKEN_ASYNC},

    {"if", TOKEN_IF},
    {"else", TOKEN_ELSE},

    {"for", TOKEN_FOR},
    {"while", TOKEN_WHILE},

    {"return", TOKEN_RETURN},

    {"try", TOKEN_TRY},
    {"catch", TOKEN_CATCH},

    {"deduct", TOKEN_DEDUCT},
    {"conclude", TOKEN_CONCLUDE},

    {"true", TOKEN_TRUE},
    {"false", TOKEN_FALSE},
    {"null", TOKEN_NULL}
};

const int keyword_count =
sizeof(keywords) / sizeof(keywords[0]);
