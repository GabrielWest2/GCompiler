package com.gabe;

public class GVar {
    public final String name;
    public final Type type;

    public GVar(String name, Type type) {
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
