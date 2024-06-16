package com.gabe;

public class DataLocation {
    private final Type type;
    private final StorageType storageType;

    public enum StorageType {
        STACK,
        DATA_SECTION,
        REGISTER,
        CONSTANT
    }

    private final int stackIndex;
    private final String identifier;
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
            System.out.println("this is " + this.type + " and " + this.storageType);
            throw new IllegalArgumentException("Type mismatch between source and destination. Cannot move " + this.type + " to " + other.type);
        }

        String sizeSuffix;

        switch (this.type) {
            case CHAR, BOOL -> {
                sizeSuffix = "byte";
            }
            case SHORT -> sizeSuffix = "word";
            case INT, FLOAT -> sizeSuffix = "dword";
            case DOUBLE -> sizeSuffix = "qword";
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
                    throw new IllegalArgumentException("Unsupported storage type for destination");
        }

        Emitter.emitln("mov " + sizeSuffix + " " + destination + ", " + source + " ; " + comment);
    }

    @Override
    public String toString() {
        return this.asSource();
    }

    public String asSource() {
        String source;
        switch (this.storageType) {
            case STACK ->
                    source = "[ebp" + (this.stackIndex >= 0 ? "+" : "") + this.stackIndex + "]";
            case DATA_SECTION -> source = "[" + this.identifier + "]";
            case REGISTER -> source = this.identifier;
            case CONSTANT -> source = Emitter.literalString(this.literal);
            default ->
                    throw new IllegalArgumentException("Unsupported storage type for source");
        }
        return source;
    }
}
