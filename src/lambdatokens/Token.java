package lambdatokens;

enum TokenType {
    PRINT, IDENTIFIER, VAR_DECL, NUMBER, STRING,
    EQUAL, PLUS, MINUS, STAR, SLASH,
    LEFT_PAREN, RIGHT_PAREN,
    LEFT_BRACE, RIGHT_BRACE,
    LEFT_BRACKET, RIGHT_BRACKET,
    COMMA, SEMICOLON, DOT,
    UNKNOWN, EOF
}

class Token {
    TokenType type;
    String lexeme;
    int line;

    Token(TokenType type, String lexeme, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.line = line;
    }

    @Override
    public String toString() {
        return String.format("Line %d: %-12s '%s'", line, type, lexeme);
    }
}
