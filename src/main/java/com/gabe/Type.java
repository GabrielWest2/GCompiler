package com.gabe;

public enum Type {
    //TODO pointer types
    VOID, CHAR, BOOL, SHORT, INT, FLOAT, DOUBLE, CLASS;

    public static Type from(String lexeme) {
        return switch (lexeme) {
            case "void" -> VOID;
            case "char" -> CHAR;
            case "bool" -> BOOL;
            case "short" -> SHORT;
            case "int" -> INT;
            case "float" -> FLOAT;
            case "double" -> DOUBLE;
            default -> CLASS;
        };
    }

    public boolean isNumeric() {
        return this == CHAR || this == SHORT || this == INT || this == FLOAT || this == DOUBLE;
    }

    public static Type higherNumeric(Type t1, Type t2) {
        if (t1.isNumeric() && t2.isNumeric())
            return Type.values()[Math.max(t1.ordinal(), t2.ordinal())];
        Main.typeError(null, "Expected 2 numbers, got " + t1.ordinal() + " and " + t2.ordinal());
        return null;
    }

    public boolean isBool() {
        return this == BOOL;
    }

    public boolean isVoid() {
        return this == VOID;
    }
}
