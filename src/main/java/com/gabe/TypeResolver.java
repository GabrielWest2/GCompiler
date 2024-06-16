package com.gabe;

import java.util.HashMap;

public class TypeResolver implements Expr.Visitor<Type> {

    private static final TypeResolver instance;
    private static final HashMap<Expr, Type> resolvedTypes = new HashMap<>();

    static {
        instance = new TypeResolver();
    }

    static Type resolveType(Expr expr) {
        if (resolvedTypes.containsKey(expr)) {
            return resolvedTypes.get(expr);
        }
        Type t = expr.accept(instance);
        resolvedTypes.put(expr, t);
        return t;
    }


    @Override
    public Type visitAssignExpr(Expr.Assign expr) {
        GVar var = Codegen.currentEnvironment.findVar(expr.name.lexeme());
        Type varType = var.type;
        Type valType = resolveType(expr.value);
        if (valType != varType) Main.typeError(expr.name, "Incompatible types");

        return valType;
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
        GFunction func = Codegen.currentEnvironment.findFunction(expr.name.lexeme());

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
        Type left = resolveType(expr.left);
        Type right = resolveType(expr.right);
        if (!(left == Type.INT && right == Type.INT)) {
            Main.typeError(expr.operator, "Cannot use logical expressions with non-integer types");
        }
        return null;
    }

    @Override
    public Type visitUnaryExpr(Expr.Unary expr) {
        // Keep same type.
        return resolveType(expr.right);
    }

    @Override
    public Type visitTernaryExpr(Expr.Ternary expr) {
        Type condition = resolveType(expr.cond);
        if (!condition.isBool())
            Main.typeError(expr.operator, "Expected boolean condition");

        Type left = resolveType(expr.left);
        Type right = resolveType(expr.right);
        if (left != right)
            Main.typeError(expr.operator, "Ternary must return same types");

        return left;
    }

    @Override
    public Type visitVariableExpr(Expr.Variable expr) {
        GVar var = Codegen.currentEnvironment.findVar(expr.name.lexeme());
        return var.type;
    }
}
