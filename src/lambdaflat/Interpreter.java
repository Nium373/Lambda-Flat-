package lambdaflat;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

class RuntimeError extends RuntimeException {
    int line;

    RuntimeError(String msg, int line) {
        super(msg);
        this.line = line;
    }
}

class Interpreter {
    private Environment globals;
    private Environment currentEnv;
    private Scanner input = new Scanner(System.in);
    private Value currentSelf;
    private ClassObj currentClass;

    private static class MethodLookup {
        FuncObj func;
        ClassObj owner;
        MethodLookup(FuncObj func, ClassObj owner) { this.func = func; this.owner = owner; }
    }

    Interpreter() {
        globals = new Environment();
        currentEnv = globals;
        registerBuiltins();
    }

    void interpret(List<Stmt> program) {
        currentSelf = null;
        for (Stmt stmt : program) {
            try {
                execute(stmt);
            } catch (BreakException e) {
                throw new RuntimeError("break outside loop", 0);
            } catch (ContinueException e) {
                throw new RuntimeError("continue outside loop", 0);
            }
        }
    }

    // --- Built-in functions ---

    private void registerBuiltins() {
        globals.define("print", new Value((NativeFn) args -> {
            for (int i = 0; i < args.size(); i++) {
                if (i > 0) System.out.print(" ");
                System.out.print(args.get(i).toString());
            }
            System.out.println();
            return new Value();
        }), true);

        globals.define("read", new Value((NativeFn) args -> {
            if (input.hasNextLine()) {
                return new Value(input.nextLine());
            }
            return new Value();
        }), true);

        globals.define("abs", new Value((NativeFn) args -> {
            if (args.size() != 1 || args.get(0).type != ValueType.NUMBER)
                throw new RuntimeException("abs() expects a number.");
            return new Value(Math.abs((Double) args.get(0).data));
        }), true);
        globals.define("sqrt", new Value((NativeFn) args -> {
            if (args.size() != 1 || args.get(0).type != ValueType.NUMBER)
                throw new RuntimeException("sqrt() expects a number.");
            return new Value(Math.sqrt((Double) args.get(0).data));
        }), true);
        globals.define("floor", new Value((NativeFn) args -> {
            if (args.size() != 1 || args.get(0).type != ValueType.NUMBER)
                throw new RuntimeException("floor() expects a number.");
            return new Value(Math.floor((Double) args.get(0).data));
        }), true);
        globals.define("ceil", new Value((NativeFn) args -> {
            if (args.size() != 1 || args.get(0).type != ValueType.NUMBER)
                throw new RuntimeException("ceil() expects a number.");
            return new Value(Math.ceil((Double) args.get(0).data));
        }), true);
        globals.define("round", new Value((NativeFn) args -> {
            if (args.size() != 1 || args.get(0).type != ValueType.NUMBER)
                throw new RuntimeException("round() expects a number.");
            return new Value((double) Math.round((Double) args.get(0).data));
        }), true);
        globals.define("pow", new Value((NativeFn) args -> {
            if (args.size() != 2 || args.get(0).type != ValueType.NUMBER || args.get(1).type != ValueType.NUMBER)
                throw new RuntimeException("pow() expects two numbers.");
            return new Value(Math.pow((Double) args.get(0).data, (Double) args.get(1).data));
        }), true);
        globals.define("log", new Value((NativeFn) args -> {
            if (args.size() != 2 || args.get(0).type != ValueType.NUMBER || args.get(1).type != ValueType.NUMBER)
                throw new RuntimeException("log() expects two numbers.");
            double val = (Double) args.get(0).data;
            double base = (Double) args.get(1).data;
            if (val <= 0 || base <= 0 || base == 1)
                throw new RuntimeException("log() domain error.");
            return new Value(Math.log(val) / Math.log(base));
        }), true);
        globals.define("sin", new Value((NativeFn) args -> {
            if (args.size() != 1 || args.get(0).type != ValueType.NUMBER)
                throw new RuntimeException("sin() expects a number.");
            return new Value(Math.sin((Double) args.get(0).data));
        }), true);
        globals.define("cos", new Value((NativeFn) args -> {
            if (args.size() != 1 || args.get(0).type != ValueType.NUMBER)
                throw new RuntimeException("cos() expects a number.");
            return new Value(Math.cos((Double) args.get(0).data));
        }), true);
        globals.define("tan", new Value((NativeFn) args -> {
            if (args.size() != 1 || args.get(0).type != ValueType.NUMBER)
                throw new RuntimeException("tan() expects a number.");
            return new Value(Math.tan((Double) args.get(0).data));
        }), true);

        globals.define("int", new Value((NativeFn) args -> {
            if (args.size() != 1)
                throw new RuntimeException("int() expects 1 argument.");
            double val;
            if (args.get(0).type == ValueType.NUMBER) {
                val = (Double) args.get(0).data;
            } else if (args.get(0).type == ValueType.STRING) {
                try {
                    val = Double.parseDouble((String) args.get(0).data);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("int() cannot convert '" + args.get(0).data + "' to a number.");
                }
            } else {
                throw new RuntimeException("int() expects a number or string.");
            }
            return new Value((double) (long) val);
        }), true);

        globals.define("type", new Value((NativeFn) args -> {
            if (args.size() != 1)
                throw new RuntimeException("type() expects 1 argument.");
            Value v = args.get(0);
            switch (v.type) {
                case NUMBER:    return new Value("number");
                case STRING:    return new Value("string");
                case BOOL:      return new Value("bool");
                case NIL:       return new Value("nil");
                case ARRAY:     return new Value("array");
                case FUNCTION:
                case NATIVE_FN: return new Value("function");
                case LOG:       return new Value("log");
                case OBJECT: {
                    ObjObj obj = (ObjObj) v.data;
                    return new Value(obj.klass != null ? obj.klass.name : "Map");
                }
                case CLASS:     return new Value("class");
                default:        return new Value("unknown");
            }
        }), true);
        globals.define("str", new Value((NativeFn) args -> {
            if (args.size() != 1)
                throw new RuntimeException("str() expects 1 argument.");
            return new Value(args.get(0).toString());
        }), true);

        globals.define("min", new Value((NativeFn) args -> {
            if (args.isEmpty()) throw new RuntimeException("min() expects at least 1 argument.");
            List<Value> nums = (args.size() == 1 && args.get(0).type == ValueType.ARRAY)
                ? ((ArrayObj) args.get(0).data).elements : args;
            if (nums.isEmpty()) throw new RuntimeException("min() on empty array.");
            double m = Double.POSITIVE_INFINITY;
            for (Value v : nums) {
                if (v.type != ValueType.NUMBER) throw new RuntimeException("min() requires numbers.");
                m = Math.min(m, (Double) v.data);
            }
            return new Value(m);
        }), true);

        globals.define("max", new Value((NativeFn) args -> {
            if (args.isEmpty()) throw new RuntimeException("max() expects at least 1 argument.");
            List<Value> nums = (args.size() == 1 && args.get(0).type == ValueType.ARRAY)
                ? ((ArrayObj) args.get(0).data).elements : args;
            if (nums.isEmpty()) throw new RuntimeException("max() on empty array.");
            double m = Double.NEGATIVE_INFINITY;
            for (Value v : nums) {
                if (v.type != ValueType.NUMBER) throw new RuntimeException("max() requires numbers.");
                m = Math.max(m, (Double) v.data);
            }
            return new Value(m);
        }), true);

        globals.define("random", new Value((NativeFn) args -> {
            if (args.isEmpty()) return new Value(Math.random());
            if (args.size() == 1) {
                if (args.get(0).type != ValueType.NUMBER)
                    throw new RuntimeException("random(n) expects a number.");
                int n = (int) (double) (Double) args.get(0).data;
                if (n <= 0) throw new RuntimeException("random(n) requires n > 0.");
                return new Value((double) (int) (Math.random() * n));
            }
            throw new RuntimeException("random() expects 0 or 1 arguments.");
        }), true);
        globals.define("bool", new Value((NativeFn) args -> {
            if (args.size() != 1)
                throw new RuntimeException("bool() expects 1 argument.");
            return new Value(args.get(0).isTruthy());
        }), true);

        // Logger channels
        globals.define("logger", new Value(new LogObj()), true);

        // Broadcast / receive
        // Map() — create a bare object from key-value pairs
        globals.define("Map", new Value((NativeFn) args -> {
            if (args.size() % 2 != 0)
                throw new RuntimeException("Map() expects an even number of arguments (key-value pairs).");
            ObjObj obj = new ObjObj();
            for (int i = 0; i < args.size(); i += 2) {
                if (args.get(i).type != ValueType.STRING)
                    throw new RuntimeException("Map() keys must be strings.");
                String key = (String) args.get(i).data;
                obj.fields.put(key, args.get(i + 1));
            }
            return new Value(obj);
        }), true);

        globals.define("broadcast", new Value((NativeFn) args -> {
            if (args.size() != 3)
                throw new RuntimeException("broadcast() expects (channel, message, scope).");
            String channel = args.get(0).toString();
            String msg = args.get(1).toString();
            String scope = args.get(2).toString();
            System.out.println("[BROADCAST] [" + scope + "] " + channel + ": " + msg);
            return new Value();
        }), true);

        globals.define("receive", new Value((NativeFn) args -> {
            if (args.size() != 1)
                throw new RuntimeException("receive() expects (channel).");
            return new Value(); // returns nil (placeholder)
        }), true);

        // Placeholder globals for game integration
        globals.define("TRANSMISSION_RANGE", new Value(1000.0), true);
        globals.define("COMPUTER_ID", new Value("computer-0"), true);
        globals.define("NETWORK_ID", new Value("network-0"), true);
    }

    // --- Statement execution ---

    private void execute(Stmt stmt) {
        if (stmt instanceof VarDecl) {
            visitVarDecl((VarDecl) stmt);
        } else if (stmt instanceof FuncDecl) {
            visitFuncDecl((FuncDecl) stmt);
        } else if (stmt instanceof ExpressionStmt) {
            visitExpressionStmt((ExpressionStmt) stmt);
        } else if (stmt instanceof BlockStmt) {
            visitBlockStmt((BlockStmt) stmt, new Environment(currentEnv));
        } else if (stmt instanceof IfStmt) {
            visitIfStmt((IfStmt) stmt);
        } else if (stmt instanceof WhileStmt) {
            visitWhileStmt((WhileStmt) stmt);
        } else if (stmt instanceof ForStmt) {
            visitForStmt((ForStmt) stmt);
        } else if (stmt instanceof ReturnStmt) {
            visitReturnStmt((ReturnStmt) stmt);
        } else if (stmt instanceof BreakStmt) {
            visitBreakStmt((BreakStmt) stmt);
        } else if (stmt instanceof ContinueStmt) {
            visitContinueStmt((ContinueStmt) stmt);
        } else if (stmt instanceof SwitchStmt) {
            visitSwitchStmt((SwitchStmt) stmt);
        } else if (stmt instanceof ClassDecl) {
            visitClassDecl((ClassDecl) stmt);
        } else if (stmt instanceof ForInStmt) {
            visitForInStmt((ForInStmt) stmt);
        } else if (stmt instanceof ScheduleStmt) {
            throw new RuntimeError("'schedule' is not implemented in this runtime.", 0);
        } else if (stmt instanceof EveryStmt) {
            throw new RuntimeError("'every' is not implemented in this runtime.", 0);
        } else if (stmt instanceof EventStmt) {
            throw new RuntimeError("'on' event handlers are not implemented in this runtime.", ((EventStmt) stmt).line);
        }
    }

    private void visitVarDecl(VarDecl stmt) {
        Value value = evaluate(stmt.initializer);
        currentEnv.define(stmt.name.lexeme, value, stmt.isPublic);
    }

    private void visitFuncDecl(FuncDecl stmt) {
        List<String> params = new ArrayList<>();
        for (Token p : stmt.params) {
            params.add(p.lexeme);
        }
        FuncObj func = new FuncObj();
        func.name = stmt.name.lexeme;
        func.params = params;
        func.declaration = stmt;
        func.closureEnv = currentEnv;
        currentEnv.define(stmt.name.lexeme, new Value(func), stmt.isPublic);
    }

    private void visitExpressionStmt(ExpressionStmt stmt) {
        evaluate(stmt.expression);
    }

    private void visitBlockStmt(BlockStmt stmt, Environment env) {
        Environment previous = currentEnv;
        currentEnv = env;
        try {
            for (Stmt s : stmt.statements) {
                execute(s);
            }
        } finally {
            currentEnv = previous;
        }
    }

    private void visitIfStmt(IfStmt stmt) {
        Value condition = evaluate(stmt.condition);
        if (condition.isTruthy()) {
            visitBlockStmt(stmt.thenBranch, new Environment(currentEnv));
        } else if (stmt.elseBranch != null) {
            visitBlockStmt(stmt.elseBranch, new Environment(currentEnv));
        }
    }

    private void visitWhileStmt(WhileStmt stmt) {
        while (evaluate(stmt.condition).isTruthy()) {
            try {
                visitBlockStmt(stmt.body, new Environment(currentEnv));
            } catch (BreakException e) {
                break;
            } catch (ContinueException e) {
                continue;
            }
        }
    }

    private void visitForStmt(ForStmt stmt) {
        Environment loopEnv = new Environment(currentEnv);
        Environment previous = currentEnv;
        currentEnv = loopEnv;

        if (stmt.initializer != null) {
            execute(stmt.initializer);
        }

        while (true) {
            if (stmt.condition != null && !evaluate(stmt.condition).isTruthy()) {
                break;
            }

            try {
                visitBlockStmt(stmt.body, new Environment(currentEnv));
            } catch (BreakException e) {
                break;
            } catch (ContinueException e) {
                // continue to increment
            }

            if (stmt.increment != null) {
                evaluate(stmt.increment);
            }
        }

        currentEnv = previous;
    }

    private void visitReturnStmt(ReturnStmt stmt) {
        Value value = stmt.value != null ? evaluate(stmt.value) : new Value();
        throw new ReturnException(value);
    }

    private void visitBreakStmt(BreakStmt stmt) {
        throw new BreakException();
    }

    private void visitContinueStmt(ContinueStmt stmt) {
        throw new ContinueException();
    }

    private void visitSwitchStmt(SwitchStmt stmt) {
        Value switchValue = evaluate(stmt.value);
        for (CaseClause cc : stmt.cases) {
            Value caseValue = evaluate(cc.value);
            if (switchValue.equals(caseValue)) {
                visitBlockStmt(cc.body, new Environment(currentEnv));
                return;
            }
        }
        if (stmt.defaultBlock != null) {
            visitBlockStmt(stmt.defaultBlock, new Environment(currentEnv));
        }
    }

    private void visitForInStmt(ForInStmt stmt) {
        Value iterable = evaluate(stmt.iterable);
        if (iterable.type != ValueType.ARRAY)
            throw new RuntimeError("for-in requires an array.", 0);
        ArrayObj arr = (ArrayObj) iterable.data;
        for (int i = 0; i < arr.elements.size(); i++) {
            Environment iterEnv = new Environment(currentEnv);
            if (stmt.indexVar != null)
                iterEnv.define(stmt.indexVar.lexeme, new Value((double) i), true);
            iterEnv.define(stmt.itemVar.lexeme, arr.elements.get(i), true);
            try {
                visitBlockStmt(stmt.body, iterEnv);
            } catch (BreakException e) {
                break;
            } catch (ContinueException e) {
                // continue to next element
            }
        }
    }

    private Value visitInterpolatedStringExpr(InterpolatedStringExpr expr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < expr.textParts.size(); i++) {
            sb.append(expr.textParts.get(i));
            if (i < expr.exprParts.size()) {
                sb.append(evaluate(expr.exprParts.get(i)).toString());
            }
        }
        return new Value(sb.toString());
    }

    private void visitClassDecl(ClassDecl stmt) {
        ClassObj superclass = null;
        if (stmt.superclass != null) {
            Value superVal = currentEnv.get(stmt.superclass.lexeme);
            if (superVal.type != ValueType.CLASS) {
                throw new RuntimeError("Superclass must be a class.", stmt.superclass.line);
            }
            superclass = (ClassObj) superVal.data;
        }

        ClassObj klass = new ClassObj(stmt.name.lexeme, superclass);

        for (FuncDecl method : stmt.methods) {
            List<String> params = new ArrayList<>();
            for (Token p : method.params) {
                params.add(p.lexeme);
            }
            FuncObj func = new FuncObj();
            func.name = method.name.lexeme;
            func.params = params;
            func.declaration = method;
            func.closureEnv = currentEnv;
            klass.methods.put(method.name.lexeme, func);
        }

        currentEnv.define(stmt.name.lexeme, new Value(klass), true);
    }

    // --- Expression evaluation ---

    private Value evaluate(Expr expr) {
        if (expr instanceof LiteralExpr) {
            return visitLiteralExpr((LiteralExpr) expr);
        }
        if (expr instanceof VariableExpr) {
            return visitVariableExpr((VariableExpr) expr);
        }
        if (expr instanceof AssignExpr) {
            return visitAssignExpr((AssignExpr) expr);
        }
        if (expr instanceof BinaryExpr) {
            return visitBinaryExpr((BinaryExpr) expr);
        }
        if (expr instanceof UnaryExpr) {
            return visitUnaryExpr((UnaryExpr) expr);
        }
        if (expr instanceof CallExpr) {
            return visitCallExpr((CallExpr) expr);
        }
        if (expr instanceof IndexExpr) {
            return visitIndexExpr((IndexExpr) expr);
        }
        if (expr instanceof DotCallExpr) {
            return visitDotCallExpr((DotCallExpr) expr);
        }
        if (expr instanceof ArrayExpr) {
            return visitArrayExpr((ArrayExpr) expr);
        }
        if (expr instanceof CompoundAssignExpr) {
            return visitCompoundAssignExpr((CompoundAssignExpr) expr);
        }
        if (expr instanceof IndexAssignExpr) {
            return visitIndexAssignExpr((IndexAssignExpr) expr);
        }
        if (expr instanceof PostfixExpr) {
            return visitPostfixExpr((PostfixExpr) expr);
        }
        if (expr instanceof SelfExpr) {
            return visitSelfExpr((SelfExpr) expr);
        }
        if (expr instanceof SuperExpr) {
            throw new RuntimeError("'super' can only be used as super.method(args).", ((SuperExpr) expr).keyword.line);
        }
        if (expr instanceof InterpolatedStringExpr) {
            return visitInterpolatedStringExpr((InterpolatedStringExpr) expr);
        }
        throw new RuntimeException("Unknown expression type.");
    }

    private Value visitLiteralExpr(LiteralExpr expr) {
        switch (expr.token.type) {
            case NUMBER:
                return new Value((Double) expr.token.literal);
            case STRING:
                return new Value((String) expr.token.literal);
            case TRUE:
                return new Value(true);
            case FALSE:
                return new Value(false);
            case NIL:
                return new Value();
            default:
                throw new RuntimeError("Invalid literal.", expr.token.line);
        }
    }

    private Value visitVariableExpr(VariableExpr expr) {
        return currentEnv.get(expr.name.lexeme);
    }

    private Value visitAssignExpr(AssignExpr expr) {
        Value value = evaluate(expr.value);
        currentEnv.assign(expr.name.lexeme, value);
        return value;
    }

    private Value visitIndexAssignExpr(IndexAssignExpr expr) {
        Value object = evaluate(expr.object);
        Value index = evaluate(expr.index);
        Value value = evaluate(expr.value);

        if (object.type == ValueType.OBJECT) {
            ObjObj obj = (ObjObj) object.data;
            if (index.type != ValueType.STRING) {
                throw new RuntimeError("Object index must be a string.", expr.bracket.line);
            }
            obj.fields.put((String) index.data, value);
            return value;
        }

        if (object.type != ValueType.ARRAY) {
            throw new RuntimeError("Can only index arrays.", expr.bracket.line);
        }
        if (index.type != ValueType.NUMBER) {
            throw new RuntimeError("Array index must be a number.", expr.bracket.line);
        }

        ArrayObj arr = (ArrayObj) object.data;
        int i = (int) (double) (Double) index.data;
        if (i < 0 || i >= arr.elements.size()) {
            throw new RuntimeError("Array index out of bounds.", expr.bracket.line);
        }

        arr.elements.set(i, value);
        return value;
    }

    private Value visitBinaryExpr(BinaryExpr expr) {
        Value left = evaluate(expr.left);

        if (expr.op.type == TokenType.AND) {
            if (!left.isTruthy()) return new Value(false);
            return new Value(evaluate(expr.right).isTruthy());
        }
        if (expr.op.type == TokenType.OR) {
            if (left.isTruthy()) return new Value(true);
            return new Value(evaluate(expr.right).isTruthy());
        }

        Value right = evaluate(expr.right);

        switch (expr.op.type) {
            case PLUS: {
                if (left.type == ValueType.NUMBER && right.type == ValueType.NUMBER) {
                    return new Value((Double) left.data + (Double) right.data);
                }
                if (left.type == ValueType.STRING || right.type == ValueType.STRING) {
                    return new Value(left.toString() + right.toString());
                }
                throw new RuntimeError("Operands must be two numbers or two strings for '+'.", expr.op.line);
            }
            case MINUS: {
                checkNumberOperands(expr.op, left, right);
                return new Value((Double) left.data - (Double) right.data);
            }
            case STAR: {
                checkNumberOperands(expr.op, left, right);
                return new Value((Double) left.data * (Double) right.data);
            }
            case STAR_STAR: {
                checkNumberOperands(expr.op, left, right);
                return new Value(Math.pow((Double) left.data, (Double) right.data));
            }
            case SLASH: {
                checkNumberOperands(expr.op, left, right);
                double r = (Double) right.data;
                if (r == 0) throw new RuntimeError("Division by zero.", expr.op.line);
                return new Value((Double) left.data / r);
            }
            case SLASH_SLASH: {
                checkNumberOperands(expr.op, left, right);
                double r = (Double) right.data;
                if (r == 0) throw new RuntimeError("Division by zero.", expr.op.line);
                long l = (long) (double) (Double) left.data;
                long rl = (long) r;
                return new Value((double) (l / rl));
            }
            case PERCENT: {
                checkNumberOperands(expr.op, left, right);
                double r = (Double) right.data;
                if (r == 0) throw new RuntimeError("Modulo by zero.", expr.op.line);
                return new Value((Double) left.data % r);
            }
            case DOT_DOT: {
                checkNumberOperands(expr.op, left, right);
                int start = (int) (double) (Double) left.data;
                int end = (int) (double) (Double) right.data;
                ArrayObj arr = new ArrayObj();
                if (start <= end) {
                    for (int n = start; n <= end; n++) {
                        arr.elements.add(new Value((double) n));
                    }
                } else {
                    for (int n = start; n >= end; n--) {
                        arr.elements.add(new Value((double) n));
                    }
                }
                return new Value(arr);
            }
            case IN: {
                if (right.type != ValueType.ARRAY) {
                    throw new RuntimeError("Right operand of 'in' must be an array.", expr.op.line);
                }
                ArrayObj inArr = (ArrayObj) right.data;
                for (Value element : inArr.elements) {
                    if (left.equals(element)) return new Value(true);
                }
                return new Value(false);
            }
            case BETWEEN: {
                if (right.type != ValueType.ARRAY || ((ArrayObj)right.data).elements.size() < 2) {
                    throw new RuntimeError("Right operand of 'between' must be a range with start and end.", expr.op.line);
                }
                ArrayObj bArr = (ArrayObj) right.data;
                Value low = bArr.elements.get(0);
                Value high = bArr.elements.get(bArr.elements.size() - 1);
                checkNumberOperands(expr.op, left, low);
                checkNumberOperands(expr.op, left, high);
                double lv = (Double) left.data;
                double lo = (Double) low.data;
                double hi = (Double) high.data;
                return new Value(lv >= Math.min(lo, hi) && lv <= Math.max(lo, hi));
            }
            case TILDE:
                if (left.type == ValueType.NUMBER && right.type == ValueType.NUMBER) {
                    double a = (Double) left.data;
                    double b = (Double) right.data;
                    double scale = Math.max(Math.abs(a), Math.abs(b));
                    return new Value(Math.abs(a - b) <= 1e-3 * Math.max(scale, 1e-300));
                }
                return new Value(left.equals(right));
            case BANG_TILDE:
                if (left.type == ValueType.NUMBER && right.type == ValueType.NUMBER) {
                    double a = (Double) left.data;
                    double b = (Double) right.data;
                    double scale = Math.max(Math.abs(a), Math.abs(b));
                    return new Value(!(Math.abs(a - b) <= 1e-3 * Math.max(scale, 1e-300)));
                }
                return new Value(!left.equals(right));
            case EQUAL:
                return new Value(left.equals(right));
            case BANG_EQUAL:
                return new Value(!left.equals(right));
            case XOR:
                return new Value(left.isTruthy() ^ right.isTruthy());
            case GREATER: {
                checkNumberOperands(expr.op, left, right);
                return new Value((Double) left.data > (Double) right.data);
            }
            case GREATER_EQUAL: {
                checkNumberOperands(expr.op, left, right);
                return new Value((Double) left.data >= (Double) right.data);
            }
            case LESS: {
                checkNumberOperands(expr.op, left, right);
                return new Value((Double) left.data < (Double) right.data);
            }
            case LESS_EQUAL: {
                checkNumberOperands(expr.op, left, right);
                return new Value((Double) left.data <= (Double) right.data);
            }
            case EXTREME_GREATER: {
                checkNumberOperands(expr.op, left, right);
                double a = (Double) left.data;
                double b = (Double) right.data;
                if (a <= 0 || b <= 0) return new Value(false);
                return new Value(Math.log10(a) - Math.log10(b) >= 1);
            }
            case EXTREME_LESS: {
                checkNumberOperands(expr.op, left, right);
                double a = (Double) left.data;
                double b = (Double) right.data;
                if (a <= 0 || b <= 0) return new Value(false);
                return new Value(Math.log10(b) - Math.log10(a) >= 1);
            }
            case BANG_GREATER: {
                checkNumberOperands(expr.op, left, right);
                return new Value(!((Double) left.data > (Double) right.data));
            }
            case BANG_GREATER_EQUAL: {
                checkNumberOperands(expr.op, left, right);
                return new Value(!((Double) left.data >= (Double) right.data));
            }
            case BANG_LESS: {
                checkNumberOperands(expr.op, left, right);
                return new Value(!((Double) left.data < (Double) right.data));
            }
            case BANG_LESS_EQUAL: {
                checkNumberOperands(expr.op, left, right);
                return new Value(!((Double) left.data <= (Double) right.data));
            }
            case BANG_EXTREME_GREATER: {
                checkNumberOperands(expr.op, left, right);
                double a = (Double) left.data;
                double b = (Double) right.data;
                if (a <= 0 || b <= 0) return new Value(true);
                return new Value(!(Math.log10(a) - Math.log10(b) >= 1));
            }
            case BANG_EXTREME_LESS: {
                checkNumberOperands(expr.op, left, right);
                double a = (Double) left.data;
                double b = (Double) right.data;
                if (a <= 0 || b <= 0) return new Value(true);
                return new Value(!(Math.log10(b) - Math.log10(a) >= 1));
            }
            default:
                throw new RuntimeError("Unknown binary operator.", expr.op.line);
        }
    }

    private Value visitUnaryExpr(UnaryExpr expr) {
        Value right = evaluate(expr.right);

        switch (expr.op.type) {
            case MINUS: {
                if (right.type != ValueType.NUMBER) {
                    throw new RuntimeError("Operand must be a number for '-'.", expr.op.line);
                }
                return new Value(-(Double) right.data);
            }
            case NOT:
            case BANG:
                return new Value(!right.isTruthy());
            default:
                throw new RuntimeError("Unknown unary operator.", expr.op.line);
        }
    }

    private Value visitCallExpr(CallExpr expr) {
        Value callee = evaluate(expr.callee);

        List<Value> args = new ArrayList<>();
        for (Expr arg : expr.arguments) {
            args.add(evaluate(arg));
        }

        if (callee.type == ValueType.CLASS) {
            return instantiateClass(callee, args);
        }

        if (callee.type == ValueType.FUNCTION) {
            FuncObj func = (FuncObj) callee.data;
            return callFunction(func, args);
        } else if (callee.type == ValueType.NATIVE_FN) {
            NativeFn fn = (NativeFn) callee.data;
            return callNative(fn, args);
        }

        throw new RuntimeError("Can only call functions.", expr.paren.line);
    }

    private Value visitIndexExpr(IndexExpr expr) {
        Value object = evaluate(expr.object);
        Value index = evaluate(expr.index);

        if (object.type == ValueType.OBJECT) {
            ObjObj obj = (ObjObj) object.data;
            if (index.type != ValueType.STRING) {
                throw new RuntimeError("Object index must be a string.", expr.bracket.line);
            }
            String key = (String) index.data;
            Value val = obj.fields.get(key);
            if (val == null) throw new RuntimeError("Undefined field '" + key + "'.", expr.bracket.line);
            return val;
        }

        if (object.type != ValueType.ARRAY) {
            throw new RuntimeError("Can only index arrays.", expr.bracket.line);
        }

        ArrayObj arr = (ArrayObj) object.data;

        if (index.type != ValueType.NUMBER) {
            throw new RuntimeError("Array index must be a number.", expr.bracket.line);
        }

        int i = (int) (double) (Double) index.data;
        if (i < 0 || i >= arr.elements.size()) {
            throw new RuntimeError("Array index out of bounds.", expr.bracket.line);
        }

        return arr.elements.get(i);
    }

    private Value visitDotCallExpr(DotCallExpr expr) {
        // super.method(args) — resolve before evaluating the object expression
        if (expr.object instanceof SuperExpr) {
            SuperExpr superExpr = (SuperExpr) expr.object;
            if (currentSelf == null || currentClass == null)
                throw new RuntimeError("Cannot use 'super' outside a method.", superExpr.keyword.line);
            if (currentClass.superclass == null)
                throw new RuntimeError("'" + currentClass.name + "' has no superclass.", superExpr.keyword.line);
            MethodLookup superLookup = findMethodWithOwner(currentClass.superclass, expr.name.lexeme);
            if (superLookup == null)
                throw new RuntimeError("Method '" + expr.name.lexeme + "' not found in superclass.", expr.name.line);
            List<Value> superArgs = new ArrayList<>();
            for (Expr arg : expr.arguments) superArgs.add(evaluate(arg));
            ClassObj prevClass = currentClass;
            currentClass = superLookup.owner;
            try {
                return callFunction(superLookup.func, superArgs);
            } finally {
                currentClass = prevClass;
            }
        }

        Value object = evaluate(expr.object);

        List<Value> args = new ArrayList<>();
        for (Expr arg : expr.arguments) {
            args.add(evaluate(arg));
        }

        if (object.type == ValueType.STRING) {
            String str = (String) object.data;
            String method = expr.name.lexeme;
            switch (method) {
                case "length":
                    if (!args.isEmpty()) throw new RuntimeError("length() takes no arguments.", expr.dot.line);
                    return new Value((double) str.length());
                case "upper":
                    if (!args.isEmpty()) throw new RuntimeError("upper() takes no arguments.", expr.dot.line);
                    return new Value(str.toUpperCase());
                case "lower":
                    if (!args.isEmpty()) throw new RuntimeError("lower() takes no arguments.", expr.dot.line);
                    return new Value(str.toLowerCase());
                case "trim":
                    if (!args.isEmpty()) throw new RuntimeError("trim() takes no arguments.", expr.dot.line);
                    return new Value(str.trim());
                case "contains":
                    if (args.size() != 1 || args.get(0).type != ValueType.STRING)
                        throw new RuntimeError("contains() expects a string argument.", expr.dot.line);
                    return new Value(str.contains((String) args.get(0).data));
                case "startsWith":
                    if (args.size() != 1 || args.get(0).type != ValueType.STRING)
                        throw new RuntimeError("startsWith() expects a string argument.", expr.dot.line);
                    return new Value(str.startsWith((String) args.get(0).data));
                case "endsWith":
                    if (args.size() != 1 || args.get(0).type != ValueType.STRING)
                        throw new RuntimeError("endsWith() expects a string argument.", expr.dot.line);
                    return new Value(str.endsWith((String) args.get(0).data));
                case "indexOf":
                    if (args.size() != 1 || args.get(0).type != ValueType.STRING)
                        throw new RuntimeError("indexOf() expects a string argument.", expr.dot.line);
                    return new Value((double) str.indexOf((String) args.get(0).data));
                case "replace":
                    if (args.size() != 2 || args.get(0).type != ValueType.STRING || args.get(1).type != ValueType.STRING)
                        throw new RuntimeError("replace(old, new) expects two string arguments.", expr.dot.line);
                    return new Value(str.replace((String) args.get(0).data, (String) args.get(1).data));
                case "substr":
                case "substring": {
                    if (args.isEmpty() || args.get(0).type != ValueType.NUMBER)
                        throw new RuntimeError("substr(start[, end]) expects a numeric start.", expr.dot.line);
                    int start = (int) (double) (Double) args.get(0).data;
                    if (args.size() == 1) {
                        if (start < 0 || start > str.length())
                            throw new RuntimeError("substr() start index out of bounds.", expr.dot.line);
                        return new Value(str.substring(start));
                    }
                    if (args.get(1).type != ValueType.NUMBER)
                        throw new RuntimeError("substr(start, end) expects a numeric end.", expr.dot.line);
                    int end = (int) (double) (Double) args.get(1).data;
                    if (start < 0 || end > str.length() || start > end)
                        throw new RuntimeError("substr() indices out of bounds.", expr.dot.line);
                    return new Value(str.substring(start, end));
                }
                case "split": {
                    if (args.size() != 1 || args.get(0).type != ValueType.STRING)
                        throw new RuntimeError("split(delim) expects a string delimiter.", expr.dot.line);
                    String[] parts = str.split(java.util.regex.Pattern.quote((String) args.get(0).data), -1);
                    ArrayObj arr = new ArrayObj();
                    for (String part : parts) arr.elements.add(new Value(part));
                    return new Value(arr);
                }
                default:
                    throw new RuntimeError("Unknown string method '" + method + "'.", expr.dot.line);
            }
        }

        if (object.type == ValueType.ARRAY) {
            ArrayObj arr = (ArrayObj) object.data;
            String method = expr.name.lexeme;

            if (method.equals("push")) {
                if (args.size() != 1) throw new RuntimeError("push() expects 1 argument.", expr.dot.line);
                arr.elements.add(args.get(0));
                return args.get(0);
            }
            if (method.equals("pop")) {
                if (arr.elements.isEmpty()) throw new RuntimeError("pop() from empty array.", expr.dot.line);
                return arr.elements.remove(arr.elements.size() - 1);
            }
            if (method.equals("size")) {
                if (!args.isEmpty()) throw new RuntimeError("size() takes no arguments.", expr.dot.line);
                return new Value((double) arr.elements.size());
            }
            if (method.equals("shift")) {
                if (arr.elements.isEmpty()) throw new RuntimeError("shift() from empty array.", expr.dot.line);
                return arr.elements.remove(0);
            }
            if (method.equals("unshift")) {
                if (args.size() != 1) throw new RuntimeError("unshift() expects 1 argument.", expr.dot.line);
                arr.elements.add(0, args.get(0));
                return args.get(0);
            }
            if (method.equals("insert")) {
                if (args.size() != 2 || args.get(0).type != ValueType.NUMBER)
                    throw new RuntimeError("insert(index, value) expects an index and a value.", expr.dot.line);
                int idx = (int) (double) (Double) args.get(0).data;
                if (idx < 0 || idx > arr.elements.size())
                    throw new RuntimeError("insert() index out of bounds.", expr.dot.line);
                arr.elements.add(idx, args.get(1));
                return args.get(1);
            }
            if (method.equals("remove")) {
                if (args.size() != 1 || args.get(0).type != ValueType.NUMBER)
                    throw new RuntimeError("remove(index) expects a numeric index.", expr.dot.line);
                int idx = (int) (double) (Double) args.get(0).data;
                if (idx < 0 || idx >= arr.elements.size())
                    throw new RuntimeError("remove() index out of bounds.", expr.dot.line);
                return arr.elements.remove(idx);
            }
            if (method.equals("contains")) {
                if (args.size() != 1) throw new RuntimeError("contains() expects 1 argument.", expr.dot.line);
                for (Value element : arr.elements) {
                    if (element.equals(args.get(0))) return new Value(true);
                }
                return new Value(false);
            }
            if (method.equals("indexOf")) {
                if (args.size() != 1) throw new RuntimeError("indexOf() expects 1 argument.", expr.dot.line);
                for (int i = 0; i < arr.elements.size(); i++) {
                    if (arr.elements.get(i).equals(args.get(0))) return new Value((double) i);
                }
                return new Value(-1.0);
            }
            if (method.equals("join")) {
                if (args.size() > 1) throw new RuntimeError("join() expects at most 1 argument.", expr.dot.line);
                String sep = args.isEmpty() ? "" : args.get(0).toString();
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < arr.elements.size(); i++) {
                    if (i > 0) sb.append(sep);
                    sb.append(arr.elements.get(i).toString());
                }
                return new Value(sb.toString());
            }
            if (method.equals("map")) {
                if (args.size() != 1) throw new RuntimeError("map() expects a function argument.", expr.dot.line);
                Value fn = args.get(0);
                ArrayObj result = new ArrayObj();
                for (Value elem : arr.elements) {
                    List<Value> a = new ArrayList<>(); a.add(elem);
                    result.elements.add(callValue(fn, a, expr.dot.line));
                }
                return new Value(result);
            }
            if (method.equals("filter")) {
                if (args.size() != 1) throw new RuntimeError("filter() expects a function argument.", expr.dot.line);
                Value fn = args.get(0);
                ArrayObj result = new ArrayObj();
                for (Value elem : arr.elements) {
                    List<Value> a = new ArrayList<>(); a.add(elem);
                    if (callValue(fn, a, expr.dot.line).isTruthy()) result.elements.add(elem);
                }
                return new Value(result);
            }
            if (method.equals("reduce")) {
                if (args.size() != 2) throw new RuntimeError("reduce(fn, initial) expects a function and an initial value.", expr.dot.line);
                Value fn = args.get(0);
                Value acc = args.get(1);
                for (Value elem : arr.elements) {
                    List<Value> a = new ArrayList<>(); a.add(acc); a.add(elem);
                    acc = callValue(fn, a, expr.dot.line);
                }
                return acc;
            }
            if (method.equals("forEach")) {
                if (args.size() != 1) throw new RuntimeError("forEach() expects a function argument.", expr.dot.line);
                Value fn = args.get(0);
                for (Value elem : arr.elements) {
                    List<Value> a = new ArrayList<>(); a.add(elem);
                    callValue(fn, a, expr.dot.line);
                }
                return new Value();
            }
            throw new RuntimeError("Unknown array method '" + method + "'.", expr.dot.line);
        }

        if (object.type == ValueType.OBJECT) {
            ObjObj obj = (ObjObj) object.data;
            MethodLookup lookup = findMethodWithOwner(obj.klass, expr.name.lexeme);
            if (lookup != null) {
                Value prevSelf = currentSelf;
                ClassObj prevClass = currentClass;
                currentSelf = object;
                currentClass = lookup.owner;
                try {
                    return callFunction(lookup.func, args);
                } finally {
                    currentSelf = prevSelf;
                    currentClass = prevClass;
                }
            }
            Value fieldVal = obj.fields.get(expr.name.lexeme);
            if (fieldVal != null) {
                if (fieldVal.type == ValueType.FUNCTION) {
                    return callFunction((FuncObj) fieldVal.data, args);
                }
                if (fieldVal.type == ValueType.NATIVE_FN) {
                    return callNative((NativeFn) fieldVal.data, args);
                }
            }
            throw new RuntimeError("Method '" + expr.name.lexeme + "' not found on object.", expr.dot.line);
        }

        if (object.type == ValueType.CLASS) {
            ClassObj klass = (ClassObj) object.data;
            MethodLookup lookup = findMethodWithOwner(klass, expr.name.lexeme);
            if (lookup != null) {
                if (args.isEmpty()) {
                    throw new RuntimeError("Class method call requires at least one argument (self).", expr.dot.line);
                }
                Value instance = args.get(0);
                List<Value> methodArgs = args.subList(1, args.size());
                Value prevSelf = currentSelf;
                ClassObj prevClass = currentClass;
                currentSelf = instance;
                currentClass = lookup.owner;
                try {
                    return callFunction(lookup.func, methodArgs);
                } finally {
                    currentSelf = prevSelf;
                    currentClass = prevClass;
                }
            }
            throw new RuntimeError("Method '" + expr.name.lexeme + "' not found on CLASS.", expr.dot.line);
        }

        if (object.type == ValueType.LOG) {
            String method = expr.name.lexeme;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < args.size(); i++) {
                if (i > 0) sb.append(" ");
                sb.append(args.get(i).toString());
            }
            switch (method) {
                case "info":
                    System.out.println("[INFO] " + sb.toString());
                    break;
                case "warn":
                    System.out.println("[WARN] " + sb.toString());
                    break;
                case "error":
                    System.out.println("[ERROR] " + sb.toString());
                    break;
                default:
                    throw new RuntimeError("Unknown log method '" + method + "'.", expr.dot.line);
            }
            return new Value();
        }

        throw new RuntimeError("Method '" + expr.name.lexeme + "' not found on " + object.type + ".", expr.dot.line);
    }

    private Value visitArrayExpr(ArrayExpr expr) {
        ArrayObj arr = new ArrayObj();
        for (Expr elem : expr.elements) {
            arr.elements.add(evaluate(elem));
        }
        return new Value(arr);
    }

    private Value visitCompoundAssignExpr(CompoundAssignExpr expr) {
        Value current = currentEnv.get(expr.name.lexeme);
        Value right = evaluate(expr.value);

        if (expr.op.type == TokenType.PLUS_EQUAL) {
            if (current.type == ValueType.NUMBER && right.type == ValueType.NUMBER) {
                Value result = new Value((Double) current.data + (Double) right.data);
                currentEnv.assign(expr.name.lexeme, result);
                return result;
            }
            if (current.type == ValueType.STRING || right.type == ValueType.STRING) {
                Value result = new Value(current.toString() + right.toString());
                currentEnv.assign(expr.name.lexeme, result);
                return result;
            }
            throw new RuntimeError("Operands must be two numbers or two strings for '+='.", expr.op.line);
        }

        if (current.type != ValueType.NUMBER || right.type != ValueType.NUMBER) {
            throw new RuntimeError("Operands must be numbers.", expr.op.line);
        }

        double a = (Double) current.data;
        double b = (Double) right.data;
        Value result;

        switch (expr.op.type) {
            case MINUS_EQUAL: result = new Value(a - b); break;
            case STAR_EQUAL: result = new Value(a * b); break;
            case SLASH_EQUAL:
                if (b == 0) throw new RuntimeError("Division by zero.", expr.op.line);
                result = new Value(a / b); break;
            case PERCENT_EQUAL:
                if (b == 0) throw new RuntimeError("Modulo by zero.", expr.op.line);
                result = new Value(a % b); break;
            default:
                throw new RuntimeError("Unknown compound operator.", expr.op.line);
        }

        currentEnv.assign(expr.name.lexeme, result);
        return result;
    }

    private Value visitPostfixExpr(PostfixExpr expr) {
        Value current = currentEnv.get(expr.name.lexeme);
        if (current.type != ValueType.NUMBER) {
            throw new RuntimeError("Operand must be a number for '" + expr.op.lexeme + "'.", expr.op.line);
        }
        double old = (Double) current.data;
        double newVal = expr.op.type == TokenType.PLUS_PLUS ? old + 1 : old - 1;
        currentEnv.assign(expr.name.lexeme, new Value(newVal));
        return new Value(old);
    }

    // --- Helpers ---

    private void checkNumberOperands(Token op, Value left, Value right) {
        if (left.type != ValueType.NUMBER || right.type != ValueType.NUMBER) {
            throw new RuntimeError("Operands must be numbers.", op.line);
        }
    }

    private Value callFunction(FuncObj func, List<Value> args) {
        if (args.size() != func.params.size()) {
            throw new RuntimeError("Expected " + func.params.size() +
                " arguments but got " + args.size() + ".", func.declaration.name.line);
        }

        Environment env = new Environment(func.closureEnv);
        for (int i = 0; i < func.params.size(); i++) {
            env.define(func.params.get(i), args.get(i), true);
        }

        Environment previous = currentEnv;
        currentEnv = env;

        try {
            for (Stmt stmt : func.declaration.body.statements) {
                execute(stmt);
            }
            return new Value();
        } catch (ReturnException ret) {
            return ret.value;
        } finally {
            currentEnv = previous;
        }
    }

    private Value callNative(NativeFn fn, List<Value> args) {
        return fn.apply(args);
    }

    private Value visitSelfExpr(SelfExpr expr) {
        if (currentSelf == null) {
            throw new RuntimeError("Cannot use 'self' outside a method.", expr.keyword.line);
        }
        return currentSelf;
    }

    private Value instantiateClass(Value klassVal, List<Value> args) {
        ClassObj klass = (ClassObj) klassVal.data;
        ObjObj instance = new ObjObj();
        instance.klass = klass;
        Value instanceVal = new Value(instance);

        MethodLookup initLookup = findMethodWithOwner(klass, "init");
        if (initLookup != null) {
            Value prevSelf = currentSelf;
            ClassObj prevClass = currentClass;
            currentSelf = instanceVal;
            currentClass = initLookup.owner;
            try {
                callFunction(initLookup.func, args);
            } finally {
                currentSelf = prevSelf;
                currentClass = prevClass;
            }
        } else if (args.size() > 0) {
            throw new RuntimeError("Class '" + klass.name + "' has no init() but got arguments.", 0);
        }

        return instanceVal;
    }

    private FuncObj findMethod(ClassObj klass, String name) {
        MethodLookup lookup = findMethodWithOwner(klass, name);
        return lookup != null ? lookup.func : null;
    }

    private MethodLookup findMethodWithOwner(ClassObj klass, String name) {
        while (klass != null) {
            FuncObj method = klass.methods.get(name);
            if (method != null) return new MethodLookup(method, klass);
            klass = klass.superclass;
        }
        return null;
    }

    private Value callValue(Value fn, List<Value> args, int line) {
        if (fn.type == ValueType.FUNCTION) return callFunction((FuncObj) fn.data, args);
        if (fn.type == ValueType.NATIVE_FN) return callNative((NativeFn) fn.data, args);
        throw new RuntimeError("Expected a function.", line);
    }

    // --- Return handling ---

    private static class ReturnException extends RuntimeException {
        Value value;

        ReturnException(Value value) {
            super(null, null, true, false);
            this.value = value;
        }
    }

    private static class BreakException extends RuntimeException {
        BreakException() {
            super(null, null, true, false);
        }
    }

    private static class ContinueException extends RuntimeException {
        ContinueException() {
            super(null, null, true, false);
        }
    }

}
