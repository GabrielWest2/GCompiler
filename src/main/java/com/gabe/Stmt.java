package com.gabe;

import java.util.List;

public abstract class Stmt {
    public interface Visitor<R> {
        R visitBlockStmt(Block stmt);

        R visitExpressionStmt(Expression stmt);

        R visitFunctionStmt(Function stmt);

        R visitIfStmt(If stmt);

        R visitWhileStmt(While stmt);

        R visitPrintStmt(Print stmt);

        R visitVarStmt(Var stmt);

        R visitReturnStmt(Return stmt);
    }

    public static class Block extends Stmt {
        Block(List<Stmt> statements) {
            this.statements = statements;
        }

        final List<Stmt> statements;

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitBlockStmt(this);
        }
    }

    public static class Expression extends Stmt {
        Expression(Expr expression) {
            this.expression = expression;
        }

        final Expr expression;

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitExpressionStmt(this);
        }
    }

    public static class Function extends Stmt {
        Function(Token name, List<GVar> params, List<Stmt> body, Type returnType) {
            this.name = name;
            this.params = params;
            this.body = body;
            this.returnType = returnType;
        }

        public final Token name;
        public final Type returnType;
        final List<GVar> params;
        final List<Stmt> body;

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitFunctionStmt(this);
        }
    }

    public static class If extends Stmt {
        If(Expr condition, Stmt thenBranch, Stmt elseBranch) {
            this.condition = condition;
            this.thenBranch = thenBranch;
            this.elseBranch = elseBranch;
        }

        final Expr condition;
        final Stmt thenBranch;
        final Stmt elseBranch;

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitIfStmt(this);
        }
    }

    public static class While extends Stmt {
        While(Expr condition, Stmt body) {
            this.condition = condition;
            this.body = body;
        }

        final Expr condition;
        final Stmt body;

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitWhileStmt(this);
        }
    }

    public static class Print extends Stmt {
        Print(Token token, Expr expression) {
            this.expression = expression;
            this.token = token;
        }

        final Token token;
        final Expr expression;

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitPrintStmt(this);
        }
    }

    public static class Var extends Stmt {
        Var(Token name, Expr initializer, Type type) {
            this.name = name;
            this.initializer = initializer;
            this.type = type;
        }

        public final Token name;
        public final Type type;
        final Expr initializer;

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitVarStmt(this);
        }
    }

    public static class Return extends Stmt {
        Return(Token keyword, Expr value) {
            this.keyword = keyword;
            this.value = value;
        }

        final Token keyword;
        public final Expr value;

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitReturnStmt(this);
        }
    }

    public abstract <R> R accept(Visitor<R> visitor);
}
