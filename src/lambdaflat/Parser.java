package lambdaflat;

import java.util.ArrayList;
import java.util.List;

class ParseError extends RuntimeException {
    int line;

    ParseError(String msg, int line) {
        super(msg);
        this.line = line;
    }
}

class Parser {
    private List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            try {
                Stmt stmt = declaration();
                if (stmt != null) {
                    statements.add(stmt);
                }
            } catch (ParseError e) {
                System.err.println("Parse error [line " + e.line + "]: " + e.getMessage());
                synchronize();
            }
        }
        return statements;
    }

    // --- Token helpers ---

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    private ParseError error(Token token, String message) {
        throw new ParseError(message, token.line);
    }

    private TokenType peekTypeAt(int offset) {
        int idx = current + offset;
        if (idx >= tokens.size()) return TokenType.EOF;
        return tokens.get(idx).type;
    }

    private void semicolon() {
        if (check(TokenType.SEMICOLON)) advance();
    }

    private void synchronize() {
        advance();
        while (!isAtEnd()) {
            if (previous().type == TokenType.SEMICOLON) return;
            switch (peek().type) {
                case FUNCTION:
                case IF:
                case WHILE:
                case FOR:
                case RETURN:
                case PUBLIC:
                case PRIVATE:
                case BREAK:
                case CONTINUE:
                case SWITCH:
                case CASE:
                case DEFAULT:
                case SCHEDULE:
                case EVERY:
                case ON:
                case CLASS:
                case EXTENDS:
                case SELF:
                case SUPER:
                    return;
                default:
                    advance();
            }
        }
    }

    // --- Declarations ---

    private Stmt declaration() {
        if (match(TokenType.PUBLIC)) {
            Token mod = previous();
            if (match(TokenType.FUNCTION)) return funcDecl(mod);
            return varDecl(mod);
        }
        if (match(TokenType.PRIVATE)) {
            Token mod = previous();
            if (match(TokenType.FUNCTION)) return funcDecl(mod);
            return varDecl(mod);
        }
        if (match(TokenType.FUNCTION)) {
            Token fakePublic = new Token(TokenType.PUBLIC, "public", null, previous().line);
            return funcDecl(fakePublic);
        }
        if (match(TokenType.CLASS)) return classDecl();
        return statement();
    }

    private Stmt varDecl(Token accessMod) {
        Token name = consume(TokenType.IDENTIFIER, "Expected variable name.");
        consume(TokenType.EQUAL, "Expected '=' after variable name.");
        Expr value = expression();
        semicolon();

        boolean isPublic = accessMod.type == TokenType.PUBLIC;
        return new VarDecl(name, value, isPublic);
    }

    private Stmt funcDecl(Token accessMod) {
        Token name = consume(TokenType.IDENTIFIER, "Expected function name.");
        consume(TokenType.LEFT_PAREN, "Expected '(' after function name.");

        List<Token> params = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                params.add(consume(TokenType.IDENTIFIER, "Expected parameter name."));
            } while (match(TokenType.COMMA));
        }
        consume(TokenType.RIGHT_PAREN, "Expected ')' after parameters.");

        consume(TokenType.LEFT_BRACE, "Expected '{' before function body.");
        BlockStmt body = block();
        boolean isPublic = accessMod.type == TokenType.PUBLIC;
        return new FuncDecl(name, params, body, isPublic);
    }

    private Stmt classDecl() {
        Token name = consume(TokenType.IDENTIFIER, "Expected class name.");

        Token superclass = null;
        if (match(TokenType.EXTENDS)) {
            superclass = consume(TokenType.IDENTIFIER, "Expected superclass name.");
        }

        consume(TokenType.LEFT_BRACE, "Expected '{' before class body.");
        List<FuncDecl> methods = new ArrayList<>();

        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            if (match(TokenType.FUNCTION)) {
                Token fakePublic = new Token(TokenType.PUBLIC, "public", null, previous().line);
                methods.add((FuncDecl) funcDecl(fakePublic));
            } else {
                throw error(peek(), "Expected function declaration in class body.");
            }
        }
        consume(TokenType.RIGHT_BRACE, "Expected '}' after class body.");
        return new ClassDecl(name, superclass, methods);
    }

    // --- Statements ---

    private Stmt statement() {
        if (match(TokenType.IF)) return ifStatement();
        if (match(TokenType.WHILE)) return whileStatement();
        if (match(TokenType.FOR)) return forStatement();
        if (match(TokenType.RETURN)) return returnStatement();
        if (match(TokenType.BREAK)) { semicolon(); return new BreakStmt(); }
        if (match(TokenType.CONTINUE)) { semicolon(); return new ContinueStmt(); }
        if (match(TokenType.SWITCH)) return switchStatement();

        if (match(TokenType.SCHEDULE)) return scheduleStatement();
        if (match(TokenType.EVERY)) return everyStatement();
        if (match(TokenType.ON)) return onStatement();
        if (match(TokenType.LEFT_BRACE)) {
            return block();
        }
        Expr expr = expression();
        semicolon();
        expr = maybeConvertAssignment(expr);
        return new ExpressionStmt(expr);
    }

    private BlockStmt block() {
        List<Stmt> statements = new ArrayList<>();
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }
        consume(TokenType.RIGHT_BRACE, "Expected '}' after block.");
        return new BlockStmt(statements);
    }

    private Stmt ifStatement() {
        consume(TokenType.LEFT_PAREN, "Expected '(' after 'if'.");
        Expr condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expected ')' after condition.");

        consume(TokenType.LEFT_BRACE, "Expected '{' after 'if' condition.");
        BlockStmt thenBranch = block();

        BlockStmt elseBranch = null;
        if (match(TokenType.ELSE)) {
            if (match(TokenType.IF)) {
                elseBranch = parseElifChain();
            } else {
                consume(TokenType.LEFT_BRACE, "Expected '{' after 'else'.");
                elseBranch = block();
            }
        } else if (match(TokenType.ELIF)) {
            elseBranch = parseElifChain();
        }

        return new IfStmt(condition, thenBranch, elseBranch);
    }

    private BlockStmt parseElifChain() {
        Stmt inner = ifStatement();
        List<Stmt> stmts = new ArrayList<>();
        stmts.add(inner);
        return new BlockStmt(stmts);
    }

    private Stmt whileStatement() {
        consume(TokenType.LEFT_PAREN, "Expected '(' after 'while'.");
        Expr condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expected ')' after condition.");
        consume(TokenType.LEFT_BRACE, "Expected '{' after 'while' condition.");
        BlockStmt body = block();
        return new WhileStmt(condition, body);
    }

    private Stmt forStatement() {
        consume(TokenType.LEFT_PAREN, "Expected '(' after 'for'.");

        // for (item in arr)
        if (peekTypeAt(0) == TokenType.IDENTIFIER && peekTypeAt(1) == TokenType.IN) {
            Token itemVar = advance();
            advance(); // consume 'in'
            Expr iterable = expression();
            consume(TokenType.RIGHT_PAREN, "Expected ')' after for-in iterable.");
            consume(TokenType.LEFT_BRACE, "Expected '{' after for-in.");
            BlockStmt body = block();
            return new ForInStmt(null, itemVar, iterable, body);
        }

        // for (i, item in arr)
        if (peekTypeAt(0) == TokenType.IDENTIFIER && peekTypeAt(1) == TokenType.COMMA
                && peekTypeAt(2) == TokenType.IDENTIFIER && peekTypeAt(3) == TokenType.IN) {
            Token indexVar = advance();
            advance(); // consume ','
            Token itemVar = advance();
            advance(); // consume 'in'
            Expr iterable = expression();
            consume(TokenType.RIGHT_PAREN, "Expected ')' after for-in iterable.");
            consume(TokenType.LEFT_BRACE, "Expected '{' after for-in.");
            BlockStmt body = block();
            return new ForInStmt(indexVar, itemVar, iterable, body);
        }

        Stmt initializer;
        if (match(TokenType.SEMICOLON)) {
            initializer = null;
        } else if (match(TokenType.PUBLIC, TokenType.PRIVATE)) {
            Token mod = previous();
            initializer = varDecl(mod);
        } else {
            Expr expr = expression();
            consume(TokenType.SEMICOLON, "Expected ';' after for init.");
            initializer = new ExpressionStmt(maybeConvertAssignment(expr));
        }

        Expr condition = null;
        if (!check(TokenType.SEMICOLON)) {
            condition = expression();
        }
        consume(TokenType.SEMICOLON, "Expected ';' after for condition.");

        Expr increment = null;
        if (!check(TokenType.RIGHT_PAREN)) {
            increment = expression();
            increment = maybeConvertAssignment(increment);
        }
        consume(TokenType.RIGHT_PAREN, "Expected ')' after for clauses.");

        consume(TokenType.LEFT_BRACE, "Expected '{' after for clauses.");
        BlockStmt body = block();

        return new ForStmt(initializer, condition, increment, body);
    }

    private Stmt switchStatement() {
        consume(TokenType.LEFT_PAREN, "Expected '(' after 'switch'.");
        Expr value = expression();
        consume(TokenType.RIGHT_PAREN, "Expected ')' after switch value.");
        consume(TokenType.LEFT_BRACE, "Expected '{' before switch body.");

        List<CaseClause> cases = new ArrayList<>();
        BlockStmt defaultBlock = null;

        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            if (match(TokenType.CASE)) {
                Expr caseValue = expression();
                consume(TokenType.LEFT_BRACE, "Expected '{' after case value.");
                BlockStmt body = block();
                cases.add(new CaseClause(caseValue, body));
            } else if (match(TokenType.DEFAULT)) {
                consume(TokenType.LEFT_BRACE, "Expected '{' after 'default'.");
                defaultBlock = block();
            } else {
                throw error(peek(), "Expected 'case' or 'default'.");
            }
        }
        consume(TokenType.RIGHT_BRACE, "Expected '}' after switch body.");
        return new SwitchStmt(value, cases, defaultBlock);
    }

    private Stmt scheduleStatement() {
        consume(TokenType.LEFT_PAREN, "Expected '(' after 'schedule'.");
        Expr seconds = expression();
        consume(TokenType.RIGHT_PAREN, "Expected ')' after schedule time.");
        consume(TokenType.LEFT_BRACE, "Expected '{' after schedule.");
        BlockStmt body = block();
        return new ScheduleStmt(seconds, body);
    }

    private Stmt everyStatement() {
        consume(TokenType.LEFT_PAREN, "Expected '(' after 'every'.");
        Expr seconds = expression();
        consume(TokenType.RIGHT_PAREN, "Expected ')' after every interval.");
        consume(TokenType.LEFT_BRACE, "Expected '{' after every.");
        BlockStmt body = block();
        return new EveryStmt(seconds, body);
    }

    private Stmt onStatement() {
        Token keyword = previous();
        consume(TokenType.LEFT_PAREN, "Expected '(' after 'on'.");
        Expr eventName = expression();
        consume(TokenType.RIGHT_PAREN, "Expected ')' after event name.");
        consume(TokenType.LEFT_BRACE, "Expected '{' after on.");
        BlockStmt body = block();
        return new EventStmt(eventName, body, keyword.line);
    }

    private Stmt returnStatement() {
        Token keyword = previous();
        Expr value = null;
        if (!check(TokenType.SEMICOLON) && !check(TokenType.RIGHT_BRACE)) {
            value = expression();
        }
        semicolon();
        return new ReturnStmt(keyword, value);
    }

    // --- Expressions (precedence climbing) ---

    private Expr maybeConvertAssignment(Expr expr) {
        if (expr instanceof BinaryExpr) {
            BinaryExpr b = (BinaryExpr) expr;
            if (b.op.type == TokenType.EQUAL) {
                if (b.left instanceof VariableExpr) {
                    return new AssignExpr(((VariableExpr) b.left).name, b.right);
                }
                if (b.left instanceof IndexExpr) {
                    IndexExpr ix = (IndexExpr) b.left;
                    return new IndexAssignExpr(ix.object, ix.index, ix.bracket, b.right);
                }
            }
        }
        return expr;
    }

    private Expr expression() {
        return assignment();
    }

    private Expr assignment() {
        Expr expr = logicOr();

        if (match(TokenType.PLUS_EQUAL, TokenType.MINUS_EQUAL, TokenType.STAR_EQUAL, TokenType.SLASH_EQUAL, TokenType.PERCENT_EQUAL)) {
            Token op = previous();
            Expr value = assignment();
            if (expr instanceof VariableExpr) {
                VariableExpr var = (VariableExpr) expr;
                return new CompoundAssignExpr(var.name, op, value);
            }
            error(op, "Invalid assignment target.");
        }

        return expr;
    }

    private Expr logicOr() {
        Expr expr = logicXor();
        while (match(TokenType.OR)) {
            Token op = previous();
            Expr right = logicXor();
            expr = new BinaryExpr(expr, op, right);
        }
        return expr;
    }

    private Expr logicXor() {
        Expr expr = logicAnd();
        while (match(TokenType.XOR)) {
            Token op = previous();
            Expr right = logicAnd();
            expr = new BinaryExpr(expr, op, right);
        }
        return expr;
    }

    private Expr logicAnd() {
        Expr expr = equality();
        while (match(TokenType.AND)) {
            Token op = previous();
            Expr right = equality();
            expr = new BinaryExpr(expr, op, right);
        }
        return expr;
    }

    private Expr equality() {
        Expr expr = comparison();
        while (match(TokenType.BANG_EQUAL, TokenType.EQUAL, TokenType.TILDE, TokenType.BANG_TILDE)) {
            Token op = previous();
            Expr right = comparison();
            expr = new BinaryExpr(expr, op, right);
        }
        return expr;
    }

    private Expr comparison() {
        Expr expr = membership();
        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL, TokenType.EXTREME_GREATER, TokenType.EXTREME_LESS, TokenType.BANG_GREATER, TokenType.BANG_GREATER_EQUAL, TokenType.BANG_LESS, TokenType.BANG_LESS_EQUAL, TokenType.BANG_EXTREME_GREATER, TokenType.BANG_EXTREME_LESS)) {
            Token op = previous();
            Expr right = membership();
            expr = new BinaryExpr(expr, op, right);
        }
        return expr;
    }

    private Expr membership() {
        Expr expr = range();
        while (match(TokenType.IN, TokenType.BETWEEN)) {
            Token op = previous();
            Expr right = range();
            expr = new BinaryExpr(expr, op, right);
        }
        return expr;
    }

    private Expr range() {
        Expr expr = term();
        if (match(TokenType.DOT_DOT)) {
            Token op = previous();
            Expr right = term();
            expr = new BinaryExpr(expr, op, right);
        }
        return expr;
    }

    private Expr term() {
        Expr expr = factor();
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            Token op = previous();
            Expr right = factor();
            expr = new BinaryExpr(expr, op, right);
        }
        return expr;
    }

    private Expr factor() {
        Expr expr = power();
        while (match(TokenType.STAR, TokenType.SLASH, TokenType.SLASH_SLASH, TokenType.PERCENT)) {
            Token op = previous();
            Expr right = power();
            expr = new BinaryExpr(expr, op, right);
        }
        return expr;
    }

    private Expr power() {
        Expr expr = unary();
        if (match(TokenType.STAR_STAR)) {
            Token op = previous();
            Expr right = power();
            expr = new BinaryExpr(expr, op, right);
        }
        return expr;
    }

    private Expr unary() {
        if (match(TokenType.MINUS, TokenType.NOT, TokenType.BANG)) {
            Token op = previous();
            Expr right = unary();
            return new UnaryExpr(op, right);
        }
        return call();
    }

    private Expr call() {
        Expr expr = primary();

        while (true) {
            if (match(TokenType.LEFT_PAREN)) {
                Token paren = previous();
                List<Expr> args = parseArguments(paren);
                CallExpr callExpr = new CallExpr(expr, paren);
                callExpr.arguments = args;
                expr = callExpr;
            } else if (match(TokenType.LEFT_BRACKET)) {
                Token bracket = previous();
                Expr index = expression();
                consume(TokenType.RIGHT_BRACKET, "Expected ']' after index.");
                expr = new IndexExpr(expr, index, bracket);
            } else if (match(TokenType.DOT)) {
                Token dot = previous();
                Token name = consume(TokenType.IDENTIFIER, "Expected identifier after '.'.");
                if (match(TokenType.LEFT_PAREN)) {
                    Token paren = previous();
                    List<Expr> args = parseArguments(paren);
                    DotCallExpr dotCall = new DotCallExpr(expr, dot, name);
                    dotCall.arguments = args;
                    expr = dotCall;
                } else {
                    Token bracketToken = new Token(TokenType.LEFT_BRACKET, "[", null, name.line);
                    Expr indexExpr = new LiteralExpr(
                        new Token(TokenType.STRING, name.lexeme, name.lexeme, name.line)
                    );
                    expr = new IndexExpr(expr, indexExpr, bracketToken);
                }
            } else if (match(TokenType.PLUS_PLUS, TokenType.MINUS_MINUS)) {
                Token op = previous();
                if (expr instanceof VariableExpr) {
                    expr = new PostfixExpr(((VariableExpr) expr).name, op);
                } else {
                    throw error(op, "Invalid increment/decrement target.");
                }
            } else {
                break;
            }
        }

        return expr;
    }

    private Expr primary() {
        if (match(TokenType.NUMBER, TokenType.STRING, TokenType.TRUE, TokenType.FALSE, TokenType.NIL)) {
            return new LiteralExpr(previous());
        }

        if (match(TokenType.IDENTIFIER)) {
            return new VariableExpr(previous());
        }

        if (match(TokenType.SELF)) {
            return new SelfExpr(previous());
        }

        if (match(TokenType.SUPER)) {
            return new SuperExpr(previous());
        }

        if (match(TokenType.FSTRING)) {
            return parseInterpolatedString(previous());
        }

        if (match(TokenType.LEFT_PAREN)) {
            Token leftParen = previous();
            Expr expr = expression();
            consume(TokenType.RIGHT_PAREN, "Expected ')' after expression.");
            return expr;
        }

        if (match(TokenType.LEFT_BRACKET)) {
            Token bracket = previous();
            List<Expr> elements = new ArrayList<>();
            if (!check(TokenType.RIGHT_BRACKET)) {
                do {
                    elements.add(expression());
                } while (match(TokenType.COMMA));
            }
            consume(TokenType.RIGHT_BRACKET, "Expected ']' after array literal.");
            ArrayExpr arr = new ArrayExpr(bracket);
            arr.elements = elements;
            return arr;
        }

        throw error(peek(), "Expected expression.");
    }

    private InterpolatedStringExpr parseInterpolatedString(Token token) {
        String content = (String) token.literal;
        List<String> textParts = new ArrayList<>();
        List<Expr> exprParts = new ArrayList<>();

        int i = 0;
        StringBuilder text = new StringBuilder();
        while (i < content.length()) {
            char c = content.charAt(i);
            if (c == '{') {
                int depth = 1;
                int j = i + 1;
                while (j < content.length() && depth > 0) {
                    char ch = content.charAt(j);
                    if (ch == '{') depth++;
                    else if (ch == '}') depth--;
                    if (depth > 0) j++;
                    else break;
                }
                if (depth != 0) throw new ParseError("Unclosed '{' in f-string.", token.line);
                textParts.add(text.toString());
                text = new StringBuilder();
                String exprSrc = content.substring(i + 1, j);
                List<Token> inner = new Lexer(exprSrc).tokenize();
                exprParts.add(new Parser(inner).expression());
                i = j + 1;
            } else {
                text.append(c);
                i++;
            }
        }
        textParts.add(text.toString());
        return new InterpolatedStringExpr(token, textParts, exprParts);
    }

    private List<Expr> parseArguments(Token leftParen) {
        List<Expr> args = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                args.add(expression());
            } while (match(TokenType.COMMA));
        }
        consume(TokenType.RIGHT_PAREN, "Expected ')' after arguments.");
        return args;
    }
}
