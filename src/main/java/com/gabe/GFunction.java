package com.gabe;

import java.util.List;

public class GFunction {
    public final String name;
    public final Type type;
    public final List<GVar> params;

    public GFunction(String name, Type type, List<GVar> params) {
        this.name = name;
        this.type = type;
        this.params = params;
    }
}
