package lambdaflat;

enum TokenType {
    NUMBER, STRING, IDENTIFIER,
    FUNCTION, RETURN, IF, ELSE, ELIF, WHILE, FOR,
    TRUE, FALSE, NIL,
    AND, OR, NOT,
    PUBLIC, PRIVATE,
    LEFT_PAREN, RIGHT_PAREN,
    LEFT_BRACE, RIGHT_BRACE,
    LEFT_BRACKET, RIGHT_BRACKET,
    COMMA, SEMICOLON, DOT,
    PLUS, MINUS, STAR, SLASH, PERCENT,
    STAR_STAR, SLASH_SLASH, PLUS_PLUS, MINUS_MINUS, DOT_DOT,
    BANG, BANG_EQUAL, TILDE, BANG_TILDE,
    BANG_LESS, BANG_GREATER,
    BANG_LESS_EQUAL, BANG_GREATER_EQUAL,
    BANG_EXTREME_LESS, BANG_EXTREME_GREATER,
    BREAK, CONTINUE, IN, BETWEEN,
    SWITCH, CASE, DEFAULT,
    SCHEDULE, EVERY, ON,
    CLASS, EXTENDS, SELF, SUPER,
    FSTRING,
    PLUS_EQUAL, MINUS_EQUAL, STAR_EQUAL, SLASH_EQUAL, PERCENT_EQUAL, XOR,
    EQUAL,
    GREATER, GREATER_EQUAL,
    LESS, LESS_EQUAL,
    EXTREME_GREATER, EXTREME_LESS,
    EOF
}

class Token {
    TokenType type;
    String lexeme;
    Object literal;
    int line;

    Token(TokenType type, String lexeme, Object literal, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }

    @Override
    public String toString() {
        return lexeme;
    }
}
