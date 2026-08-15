#include "../include/tokens_names.h"

const char *token_name(TokenType type){

	switch(type){
		case TOKEN_LIBRABRY: return "TOKEN_LIBRABRY";
		case TOKEN_FUNC: return "TOKEN_FUNC";
		case TOKEN_CLASS: return "TOKENS_CLASS";

		case TOKEN_IDENTIFIER: return "TOKEN_IDENTIFIER";
		case TOKEN_STRING: return "TOKEN_STRING";
		case TOKEN_NUMBER: return "TOKEN_NUMBER";
		case TOKEN_EOF: return "TOKEN_EOF";

		default:
			return "TOKEN_UNKNOWN";
	}
}
