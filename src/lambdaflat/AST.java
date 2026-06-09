package lambdaflat;

import java.util.ArrayList;
import java.util.List;

abstract class Expr {}

class BinaryExpr extends Expr {
    Expr left;
    Token op;
    Expr right;

    BinaryExpr(Expr left, Token op, Expr right) {
        this.left = left;
        this.op = op;
        this.right = right;
    }
}

class UnaryExpr extends Expr {
    Token op;
    Expr right;

    UnaryExpr(Token op, Expr right) {
        this.op = op;
        this.right = right;
    }
}

class CallExpr extends Expr {
    Expr callee;
    Token paren;
    List<Expr> arguments = new ArrayList<>();

    CallExpr(Expr callee, Token paren) {
        this.callee = callee;
        this.paren = paren;
    }
}

class IndexExpr extends Expr {
    Expr object;
    Expr index;
    Token bracket;

    IndexExpr(Expr object, Expr index, Token bracket) {
        this.object = object;
        this.index = index;
        this.bracket = bracket;
    }
}

class DotCallExpr extends Expr {
    Expr object;
    Token dot;
    Token name;
    List<Expr> arguments = new ArrayList<>();

    DotCallExpr(Expr object, Token dot, Token name) {
        this.object = object;
        this.dot = dot;
        this.name = name;
    }
}

class LiteralExpr extends Expr {
    Token token;

    LiteralExpr(Token token) {
        this.token = token;
    }
}

class VariableExpr extends Expr {
    Token name;

    VariableExpr(Token name) {
        this.name = name;
    }
}

class AssignExpr extends Expr {
    Token name;
    Expr value;

    AssignExpr(Token name, Expr value) {
        this.name = name;
        this.value = value;
    }
}

class IndexAssignExpr extends Expr {
    Expr object;
    Expr index;
    Token bracket;
    Expr value;

    IndexAssignExpr(Expr object, Expr index, Token bracket, Expr value) {
        this.object = object;
        this.index = index;
        this.bracket = bracket;
        this.value = value;
    }
}

class ArrayExpr extends Expr {
    Token bracket;
    List<Expr> elements = new ArrayList<>();

    ArrayExpr(Token bracket) {
        this.bracket = bracket;
    }
}

abstract class Stmt {}

class VarDecl extends Stmt {
    Token name;
    Expr initializer;
    boolean isPublic;

    VarDecl(Token name, Expr initializer, boolean isPublic) {
        this.name = name;
        this.initializer = initializer;
        this.isPublic = isPublic;
    }
}

class FuncDecl extends Stmt {
    Token name;
    List<Token> params;
    BlockStmt body;
    boolean isPublic;

    FuncDecl(Token name, List<Token> params, BlockStmt body, boolean isPublic) {
        this.name = name;
        this.params = params;
        this.body = body;
        this.isPublic = isPublic;
    }
}

class ExpressionStmt extends Stmt {
    Expr expression;

    ExpressionStmt(Expr expression) {
        this.expression = expression;
    }
}

class BlockStmt extends Stmt {
    List<Stmt> statements;

    BlockStmt(List<Stmt> statements) {
        this.statements = statements;
    }
}

class IfStmt extends Stmt {
    Expr condition;
    BlockStmt thenBranch;
    BlockStmt elseBranch;

    IfStmt(Expr condition, BlockStmt thenBranch, BlockStmt elseBranch) {
        this.condition = condition;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
    }
}

class WhileStmt extends Stmt {
    Expr condition;
    BlockStmt body;

    WhileStmt(Expr condition, BlockStmt body) {
        this.condition = condition;
        this.body = body;
    }
}

class ForStmt extends Stmt {
    Stmt initializer;
    Expr condition;
    Expr increment;
    BlockStmt body;

    ForStmt(Stmt initializer, Expr condition, Expr increment, BlockStmt body) {
        this.initializer = initializer;
        this.condition = condition;
        this.increment = increment;
        this.body = body;
    }
}

class ReturnStmt extends Stmt {
    Token keyword;
    Expr value;

    ReturnStmt(Token keyword, Expr value) {
        this.keyword = keyword;
        this.value = value;
    }
}

class BreakStmt extends Stmt {
    BreakStmt() {}
}

class ContinueStmt extends Stmt {
    ContinueStmt() {}
}

class CompoundAssignExpr extends Expr {
    Token name;
    Token op;
    Expr value;

    CompoundAssignExpr(Token name, Token op, Expr value) {
        this.name = name;
        this.op = op;
        this.value = value;
    }
}

class PostfixExpr extends Expr {
    Token name;
    Token op;

    PostfixExpr(Token name, Token op) {
        this.name = name;
        this.op = op;
    }
}

class CaseClause {
    Expr value;
    BlockStmt body;

    CaseClause(Expr value, BlockStmt body) {
        this.value = value;
        this.body = body;
    }
}

class SwitchStmt extends Stmt {
    Expr value;
    List<CaseClause> cases;
    BlockStmt defaultBlock;

    SwitchStmt(Expr value, List<CaseClause> cases, BlockStmt defaultBlock) {
        this.value = value;
        this.cases = cases;
        this.defaultBlock = defaultBlock;
    }
}

class ScheduleStmt extends Stmt {
    Expr seconds;
    BlockStmt body;

    ScheduleStmt(Expr seconds, BlockStmt body) {
        this.seconds = seconds;
        this.body = body;
    }
}

class EveryStmt extends Stmt {
    Expr seconds;
    BlockStmt body;

    EveryStmt(Expr seconds, BlockStmt body) {
        this.seconds = seconds;
        this.body = body;
    }
}

class EventStmt extends Stmt {
    Expr eventName;
    BlockStmt body;
    int line;

    EventStmt(Expr eventName, BlockStmt body, int line) {
        this.eventName = eventName;
        this.body = body;
        this.line = line;
    }
}

class ClassDecl extends Stmt {
    Token name;
    Token superclass;
    List<FuncDecl> methods;

    ClassDecl(Token name, Token superclass, List<FuncDecl> methods) {
        this.name = name;
        this.superclass = superclass;
        this.methods = methods;
    }
}

class SelfExpr extends Expr {
    Token keyword;

    SelfExpr(Token keyword) {
        this.keyword = keyword;
    }
}

class SuperExpr extends Expr {
    Token keyword;

    SuperExpr(Token keyword) {
        this.keyword = keyword;
    }
}

class ForInStmt extends Stmt {
    Token indexVar;   // null for single-variable form
    Token itemVar;
    Expr iterable;
    BlockStmt body;

    ForInStmt(Token indexVar, Token itemVar, Expr iterable, BlockStmt body) {
        this.indexVar = indexVar;
        this.itemVar = itemVar;
        this.iterable = iterable;
        this.body = body;
    }
}

class InterpolatedStringExpr extends Expr {
    Token token;
    List<String> textParts;
    List<Expr> exprParts;

    InterpolatedStringExpr(Token token, List<String> textParts, List<Expr> exprParts) {
        this.token = token;
        this.textParts = textParts;
        this.exprParts = exprParts;
    }
}
