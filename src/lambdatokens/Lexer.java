package lambdatokens;

import static lambdatokens.TokenType.*;

import java.util.ArrayList;
import java.util.List;

class Lexer {
    private String source;
    private int start = 0;
    private int current = 0;
    private int line = 1;
    private List<Token> tokens = new ArrayList<>();

    Lexer(String source) {
        this.source = source;
    }

    List<Token> tokenize() {
        while (!isAtEnd()) {
            start = current;
            char c = advance();
            switch (c) {
                case ' ', '\t', '\r' -> {}
                case '\n' -> line++;
                case '#' -> skipComment();
                case '"' -> string();
                case '(' -> addToken(LEFT_PAREN);
                case ')' -> addToken(RIGHT_PAREN);
                case '{' -> addToken(LEFT_BRACE);
                case '}' -> addToken(RIGHT_BRACE);
                case '[' -> addToken(LEFT_BRACKET);
                case ']' -> addToken(RIGHT_BRACKET);
                case ',' -> addToken(COMMA);
                case ';' -> addToken(SEMICOLON);
                case '.' -> addToken(DOT);
                case '+' -> addToken(PLUS);
                case '-' -> addToken(MINUS);
                case '*' -> addToken(STAR);
                case '/' -> addToken(SLASH);
                case '=' -> addToken(EQUAL);
                default -> {
                    if (isDigit(c)) { number(); }
                    else if (isAlpha(c)) { identifier(); }
                    else { unknown(); }
                }
            }
        }
        tokens.add(new Token(EOF, "", line));
        return tokens;
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private char advance() {
        return source.charAt(current++);
    }

    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private void addToken(TokenType type) {
        tokens.add(new Token(type, source.substring(start, current), line));
    }

    private void skipComment() {
        while (peek() != '\n' && !isAtEnd()) advance();
    }

    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++;
            advance();
        }
        if (isAtEnd()) {
            tokens.add(new Token(UNKNOWN, source.substring(start, current), line));
            return;
        }
        advance();
        addToken(STRING);
    }

    private void number() {
        while (isDigit(peek())) advance();
        if (peek() == '.' && isDigit(peek(1))) {
            advance();
            while (isDigit(peek())) advance();
        }
        addToken(NUMBER);
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) advance();
        String text = source.substring(start, current);

        if (text.equals("print")) {
            addToken(PRINT);
            return;
        }

        int la = current;
        while (la < source.length()) {
            char c = source.charAt(la);
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') { la++; }
            else if (c == '#') {
                while (la < source.length() && source.charAt(la) != '\n') la++;
            } else { break; }
        }
        if (la < source.length() && source.charAt(la) == '=') {
            addToken(VAR_DECL);
        } else {
            addToken(IDENTIFIER);
        }
    }

    private void unknown() {
        addToken(UNKNOWN);
    }

    private char peek(int offset) {
        int pos = current + offset;
        if (pos >= source.length()) return '\0';
        return source.charAt(pos);
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }
}
