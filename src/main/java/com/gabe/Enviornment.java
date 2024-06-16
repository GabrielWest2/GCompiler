package com.gabe;

import java.util.HashMap;

public class Enviornment {
    public final Enviornment parent;
    public HashMap<String, Object> declared = new HashMap<>();


    public GVar findVar(String name) {
        if (this.declared.containsKey(name))
            if (this.declared.get(name) instanceof GVar)
                return (GVar) this.declared.get(name);
            else return null;
        else if (this.parent != null) return this.parent.findVar(name);
        else return null;
    }

    public GFunction findFunction(String name) {
        if (this.declared.containsKey(name))
            if (this.declared.get(name) instanceof GFunction)
                return (GFunction) this.declared.get(name);
            else return null;
        else if (this.parent != null) return this.parent.findFunction(name);
        else return null;
    }

    public boolean isGlobal() {
        return this.parent == null;
    }


    public Enviornment(Enviornment parent) {
        this.parent = parent;
    }

    public void defineVar(Token name, GVar value) {
        if (this.declared.containsKey(name.lexeme()))
            Main.parseError(name, "Duplicate variable name!");
        this.declared.put(name.lexeme(), value);
    }

    public void defineFunction(Token name, GFunction func) {
        if (this.declared.containsKey(name.lexeme()))
            Main.parseError(name, "Duplicate function name!");
        this.declared.put(name.lexeme(), func);
    }
}
