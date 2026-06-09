package lambdaflat;

import com.redcraft.text.Text;
import com.redcraft.text.lexer.GreedyLexer;
import com.redcraft.text.rule.LexerRule;
import com.redcraft.text.token.SourceInfo;
import com.redcraft.text.token.Token;
import com.redcraft.text.token.TokenType;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Lexer {

    // --- Library TokenType constants (identify library tokens during conversion) ---

    private static final TokenType<String> LT_NUMBER = new TokenType<>(String.class, "NUMBER");
    private static final TokenType<String> LT_STRING = new TokenType<>(String.class, "STRING");
    private static final TokenType<String> LT_FSTRING = new TokenType<>(String.class, "FSTRING");
    private static final TokenType<String> LT_IDENTIFIER = new TokenType<>(String.class, "IDENTIFIER");
    private static final TokenType<String> LT_LINE_COMMENT = new TokenType<>(String.class, "LINE_COMMENT");
    private static final TokenType<String> LT_BLOCK_COMMENT = new TokenType<>(String.class, "BLOCK_COMMENT");

    private static final TokenType<Void> LT_WS = TokenType.newVoidType("WS");
    private static final TokenType<Void> LT_NL = TokenType.newVoidType("NL");

    private static final Map<String, lambdaflat.TokenType> keywordTypes = new HashMap<>();

    static {
        keywordTypes.put("function", lambdaflat.TokenType.FUNCTION);
        keywordTypes.put("return", lambdaflat.TokenType.RETURN);
        keywordTypes.put("if", lambdaflat.TokenType.IF);
        keywordTypes.put("else", lambdaflat.TokenType.ELSE);
        keywordTypes.put("elif", lambdaflat.TokenType.ELIF);
        keywordTypes.put("while", lambdaflat.TokenType.WHILE);
        keywordTypes.put("for", lambdaflat.TokenType.FOR);
        keywordTypes.put("true", lambdaflat.TokenType.TRUE);
        keywordTypes.put("false", lambdaflat.TokenType.FALSE);
        keywordTypes.put("nil", lambdaflat.TokenType.NIL);
        keywordTypes.put("and", lambdaflat.TokenType.AND);
        keywordTypes.put("or", lambdaflat.TokenType.OR);
        keywordTypes.put("not", lambdaflat.TokenType.NOT);
        keywordTypes.put("public", lambdaflat.TokenType.PUBLIC);
        keywordTypes.put("private", lambdaflat.TokenType.PRIVATE);
        keywordTypes.put("break", lambdaflat.TokenType.BREAK);
        keywordTypes.put("continue", lambdaflat.TokenType.CONTINUE);
        keywordTypes.put("switch", lambdaflat.TokenType.SWITCH);
        keywordTypes.put("case", lambdaflat.TokenType.CASE);
        keywordTypes.put("default", lambdaflat.TokenType.DEFAULT);

        keywordTypes.put("schedule", lambdaflat.TokenType.SCHEDULE);
        keywordTypes.put("every", lambdaflat.TokenType.EVERY);
        keywordTypes.put("on", lambdaflat.TokenType.ON);
        keywordTypes.put("in", lambdaflat.TokenType.IN);
        keywordTypes.put("between", lambdaflat.TokenType.BETWEEN);

        keywordTypes.put("class", lambdaflat.TokenType.CLASS);
        keywordTypes.put("extends", lambdaflat.TokenType.EXTENDS);
        keywordTypes.put("self", lambdaflat.TokenType.SELF);
        keywordTypes.put("super", lambdaflat.TokenType.SUPER);
    }

    private static final Map<String, lambdaflat.TokenType> punctuationTypes = new HashMap<>();

    static {
        punctuationTypes.put("(", lambdaflat.TokenType.LEFT_PAREN);
        punctuationTypes.put(")", lambdaflat.TokenType.RIGHT_PAREN);
        punctuationTypes.put("{", lambdaflat.TokenType.LEFT_BRACE);
        punctuationTypes.put("}", lambdaflat.TokenType.RIGHT_BRACE);
        punctuationTypes.put("[", lambdaflat.TokenType.LEFT_BRACKET);
        punctuationTypes.put("]", lambdaflat.TokenType.RIGHT_BRACKET);
        punctuationTypes.put(",", lambdaflat.TokenType.COMMA);
        punctuationTypes.put(";", lambdaflat.TokenType.SEMICOLON);
        punctuationTypes.put(".", lambdaflat.TokenType.DOT);
        punctuationTypes.put("+", lambdaflat.TokenType.PLUS);
        punctuationTypes.put("-", lambdaflat.TokenType.MINUS);
        punctuationTypes.put("*", lambdaflat.TokenType.STAR);
        punctuationTypes.put("/", lambdaflat.TokenType.SLASH);
        punctuationTypes.put("%", lambdaflat.TokenType.PERCENT);
        punctuationTypes.put("!", lambdaflat.TokenType.BANG);
        punctuationTypes.put("!=", lambdaflat.TokenType.BANG_EQUAL);
        punctuationTypes.put("!>>", lambdaflat.TokenType.BANG_EXTREME_GREATER);
        punctuationTypes.put("!>=", lambdaflat.TokenType.BANG_GREATER_EQUAL);
        punctuationTypes.put("!>", lambdaflat.TokenType.BANG_GREATER);
        punctuationTypes.put("!<<", lambdaflat.TokenType.BANG_EXTREME_LESS);
        punctuationTypes.put("!<=", lambdaflat.TokenType.BANG_LESS_EQUAL);
        punctuationTypes.put("!<", lambdaflat.TokenType.BANG_LESS);
        punctuationTypes.put("=", lambdaflat.TokenType.EQUAL);
        punctuationTypes.put(">", lambdaflat.TokenType.GREATER);
        punctuationTypes.put(">=", lambdaflat.TokenType.GREATER_EQUAL);
        punctuationTypes.put(">>", lambdaflat.TokenType.EXTREME_GREATER);
        punctuationTypes.put("<<", lambdaflat.TokenType.EXTREME_LESS);
        punctuationTypes.put("<", lambdaflat.TokenType.LESS);
        punctuationTypes.put("<=", lambdaflat.TokenType.LESS_EQUAL);
        punctuationTypes.put("+=", lambdaflat.TokenType.PLUS_EQUAL);
        punctuationTypes.put("-=", lambdaflat.TokenType.MINUS_EQUAL);
        punctuationTypes.put("*=", lambdaflat.TokenType.STAR_EQUAL);
        punctuationTypes.put("/=", lambdaflat.TokenType.SLASH_EQUAL);
        punctuationTypes.put("%=", lambdaflat.TokenType.PERCENT_EQUAL);
        punctuationTypes.put("**", lambdaflat.TokenType.STAR_STAR);
        punctuationTypes.put("//", lambdaflat.TokenType.SLASH_SLASH);
        punctuationTypes.put("++", lambdaflat.TokenType.PLUS_PLUS);
        punctuationTypes.put("--", lambdaflat.TokenType.MINUS_MINUS);
        punctuationTypes.put("..", lambdaflat.TokenType.DOT_DOT);
        punctuationTypes.put("&", lambdaflat.TokenType.AND);
        punctuationTypes.put("|", lambdaflat.TokenType.OR);
        punctuationTypes.put("||", lambdaflat.TokenType.XOR);
        punctuationTypes.put("~", lambdaflat.TokenType.TILDE);
        punctuationTypes.put("!~", lambdaflat.TokenType.BANG_TILDE);
    }

    // --- LexerRule definitions ---

    private static final Field sourceLineField = getSourceLineField();

    private static Field getSourceLineField() {
        try {
            Field f = SourceInfo.class.getDeclaredField("line");
            f.setAccessible(true);
            return f;
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Cannot access SourceInfo.line", e);
        }
    }

    private static final LexerRule[] rules = buildRules();

    private static LexerRule[] buildRules() {
        LexerRule[] r = new LexerRule[100];
        int i = 0;

        // Whitespace & newlines (skipped)
        r[i++] = LexerRule.of("^[ \t]+$", s -> new Token<>(null, LT_WS), LexerRule.LOW_PRIORITY);
        r[i++] = LexerRule.ofKeyword("\n", () -> new Token<>(null, LT_NL));

        // Comments (skipped) — # to end of line
        r[i++] = LexerRule.of("^#[^\n]*$", s -> new Token<>(s, LT_LINE_COMMENT), LexerRule.HIGH_PRIORITY);
        r[i++] = LexerRule.of("^/\\*[\\s\\S]*\\*/$", s -> new Token<>(s, LT_BLOCK_COMMENT), LexerRule.HIGH_PRIORITY);

        // F-string literals  f"..."  (must come before plain strings)
        r[i++] = LexerRule.of(
            s -> s.startsWith("f\"") && s.endsWith("\"") && s.length() >= 3 && !s.substring(2, s.length() - 1).contains("\""),
            s -> s.equals("f") || (s.startsWith("f\"") && !s.substring(2).contains("\"")),
            s -> new Token<>(s, LT_FSTRING),
            LexerRule.HIGH_PRIORITY
        );

        // String literals
        r[i++] = LexerRule.of(
            s -> s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2 && s.substring(1, s.length() - 1).indexOf('"') == -1,
            s -> s.startsWith("\"") && !s.substring(1).contains("\""),
            s -> new Token<>(s, LT_STRING),
            LexerRule.HIGH_PRIORITY
        );

        // Number literals (decimal point requires at least one digit after it)
        r[i++] = LexerRule.of("^-?[0-9]+(\\.[0-9]+)$|^-?[0-9]+$", s -> new Token<>(s, LT_NUMBER), LexerRule.HIGH_PRIORITY);

        // Keywords
        r[i++] = LexerRule.ofKeyword("function", () -> new Token<>(null, kw("function")), LexerRule.HIGH_PRIORITY);
        r[i++] = LexerRule.ofKeyword("return", () -> new Token<>(null, kw("return")), LexerRule.HIGH_PRIORITY);
        r[i++] = LexerRule.ofKeyword("if", () -> new Token<>(null, kw("if")), LexerRule.HIGH_PRIORITY);
        r[i++] = LexerRule.ofKeyword("else", () -> new Token<>(null, kw("else")), LexerRule.HIGH_PRIORITY);
        r[i++] = LexerRule.ofKeyword("elif", () -> new Token<>(null, kw("elif")), LexerRule.HIGH_PRIORITY);
        r[i++] = LexerRule.ofKeyword("while", () -> new Token<>(null, kw("while")), LexerRule.HIGH_PRIORITY);
        r[i++] = LexerRule.ofKeyword("for", () -> new Token<>(null, kw("for")), LexerRule.HIGH_PRIORITY);
        r[i++] = LexerRule.ofKeyword("true", () -> new Token<>(null, kw("true")), LexerRule.HIGH_PRIORITY);
        r[i++] = LexerRule.ofKeyword("false", () -> new Token<>(null, kw("false")), LexerRule.HIGH_PRIORITY);
        r[i++] = LexerRule.ofKeyword("nil", () -> new Token<>(null, kw("nil")), LexerRule.HIGH_PRIORITY);
        r[i++] = LexerRule.ofKeyword("and", () -> new Token<>(null, kw("and")), LexerRule.HIGH_PRIORITY);
        r[i++] = LexerRule.ofKeyword("or", () -> new Token<>(null, kw("or")), LexerRule.HIGH_PRIORITY);
        r[i++] = LexerRule.ofKeyword("not", () -> new Token<>(null, kw("not")), LexerRule.HIGH_PRIORITY);
        r[i++] = LexerRule.ofKeyword("public", () -> new Token<>(null, kw("public")), LexerRule.HIGH_PRIORITY);
        r[i++] = LexerRule.ofKeyword("private", () -> new Token<>(null, kw("private")), LexerRule.HIGH_PRIORITY);
        r[i++] = LexerRule.ofKeyword("break", () -> new Token<>(null, kw("break")), LexerRule.HIGH_PRIORITY);
        r[i++] = LexerRule.ofKeyword("continue", () -> new Token<>(null, kw("continue")), LexerRule.HIGH_PRIORITY);
        r[i++] = LexerRule.ofKeyword("switch", () -> new Token<>(null, kw("switch")), LexerRule.HIGH_PRIORITY);
        r[i++] = LexerRule.ofKeyword("case", () -> new Token<>(null, kw("case")), LexerRule.HIGH_PRIORITY);
        r[i++] = LexerRule.ofKeyword("default", () -> new Token<>(null, kw("default")), LexerRule.HIGH_PRIORITY);

        r[i++] = LexerRule.ofKeyword("schedule", () -> new Token<>(null, kw("schedule")), LexerRule.HIGH_PRIORITY);
        r[i++] = LexerRule.ofKeyword("every", () -> new Token<>(null, kw("every")), LexerRule.HIGH_PRIORITY);
        r[i++] = LexerRule.ofKeyword("on", () -> new Token<>(null, kw("on")), LexerRule.HIGH_PRIORITY);
        r[i++] = LexerRule.ofKeyword("in", () -> new Token<>(null, kw("in")), LexerRule.HIGH_PRIORITY);
        r[i++] = LexerRule.ofKeyword("between", () -> new Token<>(null, kw("between")), LexerRule.HIGH_PRIORITY);
        r[i++] = LexerRule.ofKeyword("class", () -> new Token<>(null, kw("class")), LexerRule.HIGH_PRIORITY);
        r[i++] = LexerRule.ofKeyword("extends", () -> new Token<>(null, kw("extends")), LexerRule.HIGH_PRIORITY);
        r[i++] = LexerRule.ofKeyword("self", () -> new Token<>(null, kw("self")), LexerRule.HIGH_PRIORITY);
        r[i++] = LexerRule.ofKeyword("super", () -> new Token<>(null, kw("super")), LexerRule.HIGH_PRIORITY);

        // Identifiers
        r[i++] = LexerRule.of("^[a-zA-Z_][a-zA-Z0-9_]*$", s -> new Token<>(s, LT_IDENTIFIER));

        // Multi-char operators (must come before single-char to win by length)
        r[i++] = LexerRule.ofKeyword("!=", () -> new Token<>(null, op("!=")), LexerRule.HIGH_PRIORITY);
        r[i++] = LexerRule.ofKeyword("**", () -> new Token<>(null, op("**")), LexerRule.HIGH_PRIORITY);
        r[i++] = LexerRule.ofKeyword("//", () -> new Token<>(null, op("//")), LexerRule.HIGH_PRIORITY);
        r[i++] = LexerRule.ofKeyword("++", () -> new Token<>(null, op("++")), LexerRule.HIGH_PRIORITY);
        r[i++] = LexerRule.ofKeyword("--", () -> new Token<>(null, op("--")), LexerRule.HIGH_PRIORITY);
        r[i++] = LexerRule.ofKeyword("..", () -> new Token<>(null, op("..")), LexerRule.HIGH_PRIORITY);
        r[i++] = LexerRule.ofKeyword("!>>", () -> new Token<>(null, op("!>>")), LexerRule.HIGH_PRIORITY);
        r[i++] = LexerRule.ofKeyword("!>=", () -> new Token<>(null, op("!>=")), LexerRule.HIGH_PRIORITY);
        r[i++] = LexerRule.ofKeyword("!>", () -> new Token<>(null, op("!>")), LexerRule.HIGH_PRIORITY);
        r[i++] = LexerRule.ofKeyword("!<<", () -> new Token<>(null, op("!<<")), LexerRule.HIGH_PRIORITY);
        r[i++] = LexerRule.ofKeyword("!<=", () -> new Token<>(null, op("!<=")), LexerRule.HIGH_PRIORITY);
        r[i++] = LexerRule.ofKeyword("!<", () -> new Token<>(null, op("!<")), LexerRule.HIGH_PRIORITY);
        r[i++] = LexerRule.ofKeyword("+=", () -> new Token<>(null, op("+=")), LexerRule.HIGH_PRIORITY);
        r[i++] = LexerRule.ofKeyword("-=", () -> new Token<>(null, op("-=")), LexerRule.HIGH_PRIORITY);
        r[i++] = LexerRule.ofKeyword("*=", () -> new Token<>(null, op("*=")), LexerRule.HIGH_PRIORITY);
        r[i++] = LexerRule.ofKeyword("/=", () -> new Token<>(null, op("/=")), LexerRule.HIGH_PRIORITY);
        r[i++] = LexerRule.ofKeyword("%=", () -> new Token<>(null, op("%=")), LexerRule.HIGH_PRIORITY);
        r[i++] = LexerRule.ofKeyword(">>", () -> new Token<>(null, op(">>")), LexerRule.HIGH_PRIORITY);
        r[i++] = LexerRule.ofKeyword("<<", () -> new Token<>(null, op("<<")), LexerRule.HIGH_PRIORITY);
        r[i++] = LexerRule.ofKeyword(">=", () -> new Token<>(null, op(">=")), LexerRule.HIGH_PRIORITY);
        r[i++] = LexerRule.ofKeyword("<=", () -> new Token<>(null, op("<=")), LexerRule.HIGH_PRIORITY);
        r[i++] = LexerRule.ofKeyword("||", () -> new Token<>(null, op("||")), LexerRule.HIGH_PRIORITY);
        r[i++] = LexerRule.ofKeyword("!~", () -> new Token<>(null, op("!~")), LexerRule.HIGH_PRIORITY);

        // Single-char punctuation & operators
        r[i++] = LexerRule.ofKeyword("(", () -> new Token<>(null, op("(")), LexerRule.LOW_PRIORITY);
        r[i++] = LexerRule.ofKeyword(")", () -> new Token<>(null, op(")")), LexerRule.LOW_PRIORITY);
        r[i++] = LexerRule.ofKeyword("{", () -> new Token<>(null, op("{")), LexerRule.LOW_PRIORITY);
        r[i++] = LexerRule.ofKeyword("}", () -> new Token<>(null, op("}")), LexerRule.LOW_PRIORITY);
        r[i++] = LexerRule.ofKeyword("[", () -> new Token<>(null, op("[")), LexerRule.LOW_PRIORITY);
        r[i++] = LexerRule.ofKeyword("]", () -> new Token<>(null, op("]")), LexerRule.LOW_PRIORITY);
        r[i++] = LexerRule.ofKeyword(",", () -> new Token<>(null, op(",")), LexerRule.LOW_PRIORITY);
        r[i++] = LexerRule.ofKeyword(";", () -> new Token<>(null, op(";")), LexerRule.LOW_PRIORITY);
        r[i++] = LexerRule.ofKeyword(".", () -> new Token<>(null, op(".")), LexerRule.LOW_PRIORITY);
        r[i++] = LexerRule.ofKeyword("+", () -> new Token<>(null, op("+")), LexerRule.LOW_PRIORITY);
        r[i++] = LexerRule.ofKeyword("-", () -> new Token<>(null, op("-")), LexerRule.LOW_PRIORITY);
        r[i++] = LexerRule.ofKeyword("*", () -> new Token<>(null, op("*")), LexerRule.LOW_PRIORITY);
        r[i++] = LexerRule.ofKeyword("/", () -> new Token<>(null, op("/")), LexerRule.LOW_PRIORITY);
        r[i++] = LexerRule.ofKeyword("%", () -> new Token<>(null, op("%")), LexerRule.LOW_PRIORITY);
        r[i++] = LexerRule.ofKeyword("!", () -> new Token<>(null, op("!")), LexerRule.LOW_PRIORITY);
        r[i++] = LexerRule.ofKeyword("=", () -> new Token<>(null, op("=")), LexerRule.LOW_PRIORITY);
        r[i++] = LexerRule.ofKeyword(">", () -> new Token<>(null, op(">")), LexerRule.LOW_PRIORITY);
        r[i++] = LexerRule.ofKeyword("<", () -> new Token<>(null, op("<")), LexerRule.LOW_PRIORITY);
        r[i++] = LexerRule.ofKeyword("&", () -> new Token<>(null, op("&")), LexerRule.LOW_PRIORITY);
        r[i++] = LexerRule.ofKeyword("|", () -> new Token<>(null, op("|")), LexerRule.LOW_PRIORITY);
        r[i++] = LexerRule.ofKeyword("~", () -> new Token<>(null, op("~")), LexerRule.LOW_PRIORITY);

        return java.util.Arrays.copyOf(r, i);
    }

    private static TokenType<Void> kw(String name) {
        return new TokenType<>(Void.class, name);
    }

    private static TokenType<Void> op(String name) {
        return new TokenType<>(Void.class, name);
    }

    // --- Instance ---

    private String source;

    Lexer(String source) {
        this.source = source.replace("\r", "");
    }

    List<lambdaflat.Token> tokenize() {
        // Ensure `..` between two numbers is not consumed as `1.` (decimal)
        Text text = new Text(source.replaceAll("(?<=\\d)\\.\\.(?=\\d)", " .. "));
        GreedyLexer lexer = new GreedyLexer();
        lexer.setup();
        for (LexerRule rule : rules) {
            lexer.addRule(rule);
        }
        lexer.complete();

        List<Token<?>> libTokens = lexer.tokenize(text);

        List<lambdaflat.Token> result = new ArrayList<>();
        for (Token<?> lt : libTokens) {
            if (isSkippable(lt)) continue;
            result.add(convertToken(lt));
        }

        int lastLine = result.isEmpty() ? 1 : result.get(result.size() - 1).line;
        result.add(new lambdaflat.Token(lambdaflat.TokenType.EOF, "", null, lastLine));
        return result;
    }

    private static boolean isSkippable(Token<?> t) {
        TokenType<?> type = t.getType();
        return type == LT_WS || type == LT_NL || type == LT_LINE_COMMENT || type == LT_BLOCK_COMMENT;
    }

    private static int getSourceLine(SourceInfo src) {
        try {
            return (int) sourceLineField.get(src);
        } catch (IllegalAccessException e) {
            return 0;
        }
    }

    private static lambdaflat.Token convertToken(Token<?> t) {
        String typeName = t.getType().getName();
        int line = getSourceLine(t.source()) + 2;

        lambdaflat.TokenType lfType = keywordTypes.get(typeName);
        if (lfType != null) {
            return new lambdaflat.Token(lfType, typeName, null, line);
        }
        lfType = punctuationTypes.get(typeName);
        if (lfType != null) {
            return new lambdaflat.Token(lfType, typeName, null, line);
        }

        if (t.getType() == LT_NUMBER) {
            String lexeme = (String) t.getContent();
            double num = Double.parseDouble(lexeme);
            return new lambdaflat.Token(lambdaflat.TokenType.NUMBER, lexeme, num, line);
        }
        if (t.getType() == LT_STRING) {
            String raw = (String) t.getContent();
            String value = raw.substring(1, raw.length() - 1);
            return new lambdaflat.Token(lambdaflat.TokenType.STRING, raw, value, line);
        }
        if (t.getType() == LT_FSTRING) {
            String raw = (String) t.getContent();
            String value = raw.substring(2, raw.length() - 1); // strip f" prefix and closing "
            return new lambdaflat.Token(lambdaflat.TokenType.FSTRING, raw, value, line);
        }
        if (t.getType() == LT_IDENTIFIER) {
            String lexeme = (String) t.getContent();
            return new lambdaflat.Token(lambdaflat.TokenType.IDENTIFIER, lexeme, null, line);
        }

        throw new RuntimeException("Unknown token type: " + typeName);
    }
}
