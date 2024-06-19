package com.gabe;

public enum Type {
    //TODO pointer types
    VOID, CHAR, BOOL, SHORT, INT, FLOAT, STRING, CLASS;

    public static Type from(String lexeme) {
        return switch (lexeme) {
            case "void" -> VOID;
            case "char" -> CHAR;
            case "bool" -> BOOL;
            case "short" -> SHORT;
            case "int" -> INT;
            case "float" -> FLOAT;
            default -> CLASS;
        };
    }

    public static Type from(Object o) {
        if (o instanceof Character) {
            return CHAR;
        }
        if (o instanceof Boolean) {
            return BOOL;
        }
        if (o instanceof Short) {
            return SHORT;
        }
        if (o instanceof Integer) {
            return INT;
        }
        if (o instanceof Float) {
            return FLOAT;
        }
        if (o instanceof String) {
            return STRING;
        }

        return CLASS;
    }

    public boolean isNumeric() {
        return this == CHAR || this == SHORT || this == INT || this == FLOAT;
    }

    public static Type higherNumeric(Type t1, Type t2) {
        if (t1.isNumeric() && t2.isNumeric()) {
            Type t = Type.values()[Math.max(t1.ordinal(), t2.ordinal())];
            return t;
        }
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
