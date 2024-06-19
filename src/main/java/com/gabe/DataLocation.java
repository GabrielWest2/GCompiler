package com.gabe;

public class DataLocation {
    private final Type type;
    private final StorageType storageType;

    public Type getType() {
        return this.type;
    }

    public enum StorageType {
        STACK,
        DATA_SECTION,
        REGISTER,
        CONSTANT
    }

    private final int stackIndex;
    final String identifier;
    private final Object literal;

    private DataLocation(Type type, StorageType storageType, int stackIndex, String identifier, Object literal) {
        this.type = type;
        this.storageType = storageType;
        this.stackIndex = stackIndex;
        this.identifier = identifier;
        this.literal = literal;
    }

    public static DataLocation register(String registerName, Type type) {
        return new DataLocation(type, StorageType.REGISTER, -1, registerName, null);
    }

    public static DataLocation stack(int offset, Type type) {
        return new DataLocation(type, StorageType.STACK, offset, null, null);
    }

    public static DataLocation data(String identifier, Type type) {
        return new DataLocation(type, StorageType.DATA_SECTION, -1, identifier, null);
    }

    public static DataLocation constant(Object literal, Type type) {
        return new DataLocation(type, StorageType.CONSTANT, -1, null, literal);
    }


    public void freeScratchRegister() {
        if (this.storageType == StorageType.REGISTER) {
            if (this.identifier.equals("ebx") || this.identifier.equals("edi") || this.identifier.equals("esi")) {
                Emitter.freeScratchRegister(this.identifier);
            }
        }
    }


    public void moveTo(DataLocation other, String comment) {
        if (this.type != other.type) {
            throw new IllegalArgumentException("Type mismatch between source and destination. Cannot move " + this.type + " to " + other.type);
        }

        String sizeSuffix;

        switch (this.type) {
            case CHAR, BOOL -> {
                sizeSuffix = "byte";
            }
            case SHORT -> sizeSuffix = "word";
            case INT, FLOAT, STRING -> sizeSuffix = "dword";
            default ->
                    throw new IllegalArgumentException("Unsupported type for move operation");
        }

        String source;

        //TODO check:    when moving a char or bool from a register to memory, must only access al
        if (sizeSuffix.equals("byte") && this.storageType == StorageType.REGISTER && other.storageType != StorageType.REGISTER) {
            // move to eax first
            Emitter.emitln("mov eax, " + this.asSource());
            source = "al";
        } else {
            source = this.asSource();
        }

        //TODO check:   remove unnecessary size specification
        if (this.storageType == StorageType.REGISTER && other.storageType == StorageType.REGISTER ||
                this.storageType == StorageType.DATA_SECTION && other.storageType == StorageType.REGISTER ||
                this.storageType == StorageType.STACK && other.storageType == StorageType.REGISTER) {
            sizeSuffix = "";
        }


        String destination;
        switch (other.storageType) {
            case STACK ->
                    destination = "[ebp" + (other.stackIndex >= 0 ? "+" : "") + other.stackIndex + "]";
            case DATA_SECTION -> destination = "[" + other.identifier + "]";
            case REGISTER -> destination = other.identifier;
            default ->
                    throw new IllegalArgumentException("Unsupported storage type for destination: " + other.storageType);
        }

        Emitter.emitln("mov " + sizeSuffix + " " + destination + ", " + source + " ; " + comment);
    }


    public void pushOntoStack(boolean shouldPromoteFloats, String comment) {
        boolean isRegister = this.storageType == StorageType.REGISTER;
        switch (this.type) {
            case CHAR, BOOL -> {
                if (isRegister) {
                    Emitter.emitln("push " + this + " ; " + comment);
                } else {
                    Emitter.emitln("movzx eax, byte " + this.asSource() + " ; Move " + this.type.toString() + " into eax and zero extend");
                    Emitter.emitln("push eax ; " + comment);
                }
            }
            case SHORT -> {
                if (isRegister) {
                    Emitter.emitln("push " + this + " ; " + comment);
                } else {
                    Emitter.emitln("movzx eax, word " + this.asSource() + " ; Move " + this.type.toString() + " into eax and zero extend");
                    Emitter.emitln("push eax ; " + comment);
                }
            }
            case INT -> {
                if (isRegister) {
                    Emitter.emitln("push " + this + " ; " + comment);
                } else {
                    Emitter.emitln("mov eax, dword " + this.asSource() + " ; Move " + this.type.toString() + " into eax");
                    Emitter.emitln("push eax ; " + comment);
                }
            }
            case STRING -> {
                Emitter.emitln("push " + this.asSource() + " ; " + comment);
            }
            case FLOAT -> {
                if (!shouldPromoteFloats) {
                    Emitter.emitln("sub esp, 4 ; Allocate space on stack for float");
                    DataLocation from = this;
                    if (from.storageType != StorageType.DATA_SECTION) {
                        from = Emitter.allocFloatTmp(Type.FLOAT);
                        this.moveTo(from, comment);
                    }
                    Emitter.emitln("fld dword " + from + " ; Load float into st0");
                    Emitter.emitln("fstp dword [esp] ; " + comment);
                    Emitter.freeFloatTmp(from);
                } else {
                    Emitter.emitln("sub esp, 8 ; Allocate space on stack for float (promoted to double)");
                    DataLocation from = this;
                    if (from.storageType != StorageType.DATA_SECTION) {
                        from = Emitter.allocFloatTmp(Type.FLOAT);
                        this.moveTo(from, comment);
                    }
                    Emitter.emitln("fld dword " + from + " ; Load float into st0");
                    Emitter.emitln("fstp qword [esp] ; " + comment);
                    Emitter.freeFloatTmp(from);
                }
            }
            default ->
                    System.out.println("Unsupported storage type " + this.storageType + 0 / 0);
        }
    }

    @Override
    public String toString() {
        return this.asSource();
    }

    public DataLocation unconstantify() {
        if (this.storageType != StorageType.CONSTANT) return this;

        DataLocation temp = Emitter.allocScratchRegister(this.type);
        this.moveTo(temp, "Move constant to register");
        temp.freeScratchRegister();

        return temp;
    }

    public DataLocation floatdatify(Type t) {
        if (this.storageType == StorageType.DATA_SECTION) return this;

        DataLocation temp = Emitter.allocFloatTmp(t);
        this.moveTo(temp, "Move constant to register");
        temp.freeScratchRegister();

        return temp;
    }

    public String asSource() {
        String source;
        switch (this.storageType) {
            case STACK ->
                    source = "[ebp" + (this.stackIndex >= 0 ? "+" : "") + this.stackIndex + "]";
            case DATA_SECTION -> {
                if (this.type != Type.STRING) {
                    source = "[" + this.identifier + "]";
                } else {
                    source = this.identifier;
                }
            }
            case REGISTER -> source = this.identifier;
            case CONSTANT -> {
                source = Emitter.literalString(this.literal);
            }
            default ->
                    throw new IllegalArgumentException("Unsupported storage type for source");
        }
        return source;
    }


}
