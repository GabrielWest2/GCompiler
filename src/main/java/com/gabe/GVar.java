package com.gabe;

public class GVar {
    public final Token name;
    public final Type type;

    public GVar(Token name, Type type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String toString() {
        return "GVar{" +
                "name='" + this.name + '\'' +
                ", type=" + this.type +
                '}';
    }
}
