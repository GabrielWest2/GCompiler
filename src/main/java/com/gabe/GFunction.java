package com.gabe;

import java.util.List;

public class GFunction {
    public final String name;
    public final Type type;
    public final List<GVar> params;
    public final boolean varargs;

    public GFunction(String name, Type type, List<GVar> params, boolean varargs) {
        this.name = name;
        this.type = type;
        this.params = params;
        this.varargs = varargs;
    }
}
