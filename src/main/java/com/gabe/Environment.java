package com.gabe;

import java.util.HashMap;

public class Environment {
    public final Environment parent;
    public HashMap<String, Object> declared = new HashMap<>();
    public HashMap<String, DataLocation> dataLocations = new HashMap<>();
    public int currentLocalVarStackOffset = 0;


    public GVar findVar(String name) {
        if (this.declared.containsKey(name))
            if (this.declared.get(name) instanceof GVar)
                return (GVar) this.declared.get(name);
            else return null;
        else if (this.parent != null) return this.parent.findVar(name);
        else return null;
    }


    public DataLocation findVarLocation(String name) {
        if (this.dataLocations.containsKey(name))
            if (this.dataLocations.get(name) != null)
                return (DataLocation) this.dataLocations.get(name);
            else return null;
        else if (this.parent != null) return this.parent.findVarLocation(name);
        else return null;
    }

    public GFunction findFunction(String name) {
        if (this.declared.containsKey(name)) {
            if (this.declared.get(name) instanceof GFunction) {
                return (GFunction) this.declared.get(name);
            }
        } else if (this.parent != null) {
            return this.parent.findFunction(name);
        }
        return null;
    }

    public boolean isGlobal() {
        return this.parent == null;
    }


    public Environment(Environment parent) {
        this.parent = parent;
    }

    public void defineVar(Token name, GVar value, DataLocation location) {
        if (this.declared.containsKey(name.lexeme()))
            Main.parseError(name, "Duplicate variable name!");
        this.declared.put(name.lexeme(), value);
        this.dataLocations.put(name.lexeme(), location);
    }

    public void defineFunction(Token name, GFunction func) {
        if (this.declared.containsKey(name.lexeme()))
            Main.parseError(name, "Duplicate function name!");
        this.declared.put(name.lexeme(), func);
    }

}
