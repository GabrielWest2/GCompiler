package com.gabe;

public class DataLocation {
    private final Type type;
    private final StorageType storageType;

    public static void main(String[] args) {
        DataLocation onStack = DataLocation.stack(8, Type.CHAR);
        DataLocation eax = DataLocation.register("eax", Type.CHAR);
        DataLocation inData = DataLocation.data("a", Type.CHAR);

        System.out.println(eax.moveTo(inData));
    }

    public enum StorageType {
        STACK,
        DATA_SECTION,
        REGISTER
    }

    private final int stackIndex;
    private final String identifier;

    private DataLocation(Type type, StorageType storageType, int stackIndex, String identifier) {
        this.type = type;
        this.storageType = storageType;
        this.stackIndex = stackIndex;
        this.identifier = identifier;
    }

    public static DataLocation register(String registerName, Type type) {
        return new DataLocation(type, StorageType.REGISTER, -1, registerName);
    }

    public static DataLocation stack(int offset, Type type) {
        return new DataLocation(type, StorageType.STACK, offset, null);
    }

    public static DataLocation data(String identifier, Type type) {
        return new DataLocation(type, StorageType.DATA_SECTION, -1, identifier);
    }

    public void freeScratchRegister() {
        if (this.storageType == StorageType.REGISTER) {
            Emitter.freeScratchRegister(this.identifier);
        }
    }


    public String moveTo(DataLocation other) {
        if (this.type != other.type) {
            throw new IllegalArgumentException("Type mismatch between source and destination");
        }

        String sizeSuffix;
        switch (this.type) {
            case CHAR, BOOL -> sizeSuffix = "byte";
            case SHORT -> sizeSuffix = "word";
            case INT, FLOAT -> sizeSuffix = "dword";
            case DOUBLE -> sizeSuffix = "qword";
            default ->
                    throw new IllegalArgumentException("Unsupported type for move operation");
        }

        String source = this.asSource();

        String destination;
        switch (other.storageType) {
            case STACK ->
                    destination = "[ebp" + (other.stackIndex >= 0 ? "+" : "") + other.stackIndex + "]";
            case DATA_SECTION -> destination = "[" + other.identifier + "]";
            case REGISTER -> destination = other.identifier;
            default ->
                    throw new IllegalArgumentException("Unsupported storage type for destination");
        }

        return "mov " + sizeSuffix + " " + destination + ", " + source;
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
            default ->
                    throw new IllegalArgumentException("Unsupported storage type for source");
        }
        return source;
    }
}
