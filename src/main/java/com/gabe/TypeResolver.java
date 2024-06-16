package com.gabe;

public class TypeResolver implements Expr.Visitor<Type> {

    private static final TypeResolver instance;

    static {
        instance = new TypeResolver();
    }

    static Type resolveType(Expr expr) {
        return expr.accept(instance);
    }


    @Override
    public Type visitAssignExpr(Expr.Assign expr) {
        return null;
    }

    @Override
    public Type visitBinaryExpr(Expr.Binary expr) {
        TokenType op = expr.operator.tokenType();
        if (
                op == TokenType.LESS ||
                        op == TokenType.GREATER ||
                        op == TokenType.LESS_EQUAL ||
                        op == TokenType.GREATER_EQUAL
        ) {
            Type left = resolveType(expr.left);
            Type right = resolveType(expr.left);
            if (!(left.isNumeric() && right.isNumeric()))
                Main.typeError(expr.operator, "Expected 2 numbers");
            return Type.BOOL;
        } else if (
                op == TokenType.PLUS ||
                        op == TokenType.MINUS ||
                        op == TokenType.STAR ||
                        op == TokenType.SLASH ||
                        op == TokenType.PERCENT) {
            Type left = resolveType(expr.left);
            Type right = resolveType(expr.left);
            if (!(left.isNumeric() && right.isNumeric()))
                Main.typeError(expr.operator, "Expected 2 numbers");
            return Type.higherNumeric(left, right);
        } else if (op == TokenType.EQUAL_EQUAL || op == TokenType.BANG_EQUAL) {
            Type left = resolveType(expr.left);
            Type right = resolveType(expr.left);
            if (left.isVoid() || right.isVoid())
                Main.typeError(expr.operator, "Cannot compare with void.");
            return Type.BOOL;
        }
        return null;
    }

    @Override
    public Type visitCallExpr(Expr.Call expr) {
        //TODO allow for overloading??
        GFunction func = Codegen.currentEnviornment.findFunction(expr.name.lexeme());
        //TODO check function types
        return func.type;
    }

    @Override
    public Type visitGroupingExpr(Expr.Grouping expr) {
        // Keep same type.
        return TypeResolver.resolveType(expr.expression);
    }

    @Override
    public Type visitLiteralExpr(Expr.Literal expr) {
        if (expr.value instanceof Character) return Type.CHAR;
        if (expr.value instanceof Short) return Type.SHORT;
        if (expr.value instanceof Boolean) return Type.BOOL;
        if (expr.value instanceof Integer) return Type.INT;
        if (expr.value instanceof Double) return Type.DOUBLE;
        if (expr.value instanceof Float) return Type.FLOAT;

        return Type.CLASS;
    }

    @Override
    public Type visitLogicalExpr(Expr.Logical expr) {
        return null;
    }

    @Override
    public Type visitUnaryExpr(Expr.Unary expr) {
        // Keep same type.
        return resolveType(expr.right);
    }

    @Override
    public Type visitTernaryExpr(Expr.Ternary expr) {
        return null;
    }

    @Override
    public Type visitVariableExpr(Expr.Variable expr) {
        GVar var = Codegen.currentEnviornment.findVar(expr.name.lexeme());
        return var.type;
    }
}
