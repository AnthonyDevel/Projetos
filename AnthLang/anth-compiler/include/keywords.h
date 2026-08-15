#ifndef KEYWORDS_H
#define KEYWORDS_H

#include "token.h"

typedef struct
{
    const char *word;

    TokenType token;

} Keyword;

extern Keyword keywords[];

extern const int keyword_count;

#endif
