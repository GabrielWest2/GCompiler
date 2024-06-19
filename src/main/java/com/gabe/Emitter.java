package com.gabe;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Emitter {
    public record BSS(String name, int bytes) {
    }

    record DATA(String name, Object defaultValue) {
    }

    private static final List<DATA> dataSection;
    private static final List<BSS> bssSection;
    private static final List<String> freeRegisters;
    private static final List<String> freeFloatLocations;


    static final List<String> usedRegisters;
    private static final List<String> externals;
    private static final HashMap<LabelType, Integer> labelNums;

    private static final StringBuilder program;
    private static final StringBuilder functionSection;

    private static int indentLevel;
    private static final int COMMENT_START = 35;
    public static boolean inFunctionSection = false;

    static {
        freeRegisters = new ArrayList<>(Arrays.asList("ebx", "edi", "esi"));
        freeFloatLocations = new ArrayList<>(Arrays.asList("floatreg1", "floatreg2"));
        usedRegisters = new ArrayList<>();
        dataSection = new ArrayList<>();
        bssSection = new ArrayList<>();
        externals = new ArrayList<>();
        program = new StringBuilder();
        functionSection = new StringBuilder();
        labelNums = new HashMap<>();
        for (LabelType l : LabelType.values()) labelNums.put(l, 0);
    }

    public static DataLocation allocFloatTmp(Type t) {
        return DataLocation.data(freeFloatLocations.get(0), t);
    }

    public static void freeFloatTmp(DataLocation location) {
        freeFloatLocations.add(location.identifier);
        if (freeRegisters.size() > 2) System.out.println("uhh ohh " + 0 / 0);
    }

    public enum LabelType {
        CMP_IS_TRUE, CMP_END, TERNARY_IS_TRUE, TERNARY_END, IF_STMT_TRUE, IF_STMT_END, WHILE_BEGIN, WHILE_END
    }

    public enum LineEnding {
        NULL("0"), NL("10, 0"), CRLF("13, 0");

        private final String ending;

        LineEnding(String ending) {
            this.ending = ending;
        }

        String getEnding() {
            return this.ending;
        }
    }

    private record StringLiteral(String str, LineEnding ending) {
    }

    public enum RegisterType {
        B64, B32, B16, B8L, B8H
    }

    public static String getReg(String eReg, RegisterType registerType) {
        switch (eReg) {
            case "ebx" -> {
                switch (registerType) {
                    case B64:
                        return "rbx";
                    case B32:
                        return "ebx";
                    case B16:
                        return "bx";
                    case B8L:
                        return "bl";
                    case B8H:
                        return "bh";
                }
            }
            case "edi" -> {
                switch (registerType) {
                    case B64:
                        return "rdi";
                    case B32:
                        return "edi";
                    case B16:
                        return "di";
                    case B8L:
                        return "dil";
                }
            }
            case "esi" -> {
                switch (registerType) {
                    case B64:
                        return "rsi";
                    case B32:
                        return "esi";
                    case B16:
                        return "si";
                    case B8L:
                        return "sil";
                }
            }
        }

        return "dummy";
    }


    private static int stringNum;
    private static int floatNum;

    static DataLocation declareStringConstant(String s) {
        String name = "str" + stringNum++;
        LineEnding ending = LineEnding.NULL;
        if (s.endsWith("\r\n")) {
            ending = LineEnding.CRLF;
            s = s.substring(0, s.length() - 2);
        } else if (s.endsWith("\n")) {
            ending = LineEnding.NL;
            s = s.substring(0, s.length() - 1);
        }
        dataSection.add(new DATA(name, new StringLiteral(s, ending)));
        return DataLocation.data(name, Type.STRING);
    }

    static DataLocation declareFloatConstant(Float f) {
        String name = "cfloat" + floatNum++;
        dataSection.add(new DATA(name, f));
        return DataLocation.data(name, Type.FLOAT);
    }

    static String getNextLabelName(LabelType l) {
        String s = l.toString();
        int index = labelNums.get(l);
        s += index;
        labelNums.put(l, index + 1);
        return s;
    }

    static void defineLabel(String l) {
        Emitter.unindent();
        Emitter.emitln(l + ":");
        Emitter.indent();
    }

    static void addData(DATA d) {
        dataSection.add(d);
    }

    public static void addBSS(BSS b) {
        bssSection.add(b);
    }

    static void addExternal(String s) {
        externals.add(s);
    }


    static void emitln(String line) {
        StringBuilder b = inFunctionSection ? functionSection : program;
        if (line.contains(";")) {
            String code = line.split(";")[0];
            String comment = line.split(";")[1];
            if (code.length() < COMMENT_START && !code.isEmpty()) {


                b.append("    ".repeat(indentLevel)).append(code).append(" ".repeat(COMMENT_START - code.length())).append(";").append(comment).append("\n");
            } else
                b.append("    ".repeat(indentLevel)).append(line).append("\n");
        } else
            b.append("    ".repeat(indentLevel)).append(line).append("\n");
    }

    static void emitln() {
        emitln("\n");
    }

    static void indent() {
        indentLevel++;
    }

    static void unindent() {
        indentLevel--;
        indentLevel = Math.max(0, indentLevel);
    }

    static void epilogue() {
        Emitter.emitln("; Epilogue");
        Emitter.emitln("mov esp, ebp ; Return stack pointer");
        Emitter.emitln("pop ebp ; Recover base pointer");
        Emitter.emitln("ret ; Return from function");
    }

    public static String literalString(Object o) {
        if (o instanceof Integer)
            return o.toString();
        else if (o instanceof Double)
            return o.toString();
        else if (o instanceof StringLiteral literal) {
            return "\"" + literal.str + "\"";
        } else if (o instanceof Character) {
            return "'" + o + "'";
        } else if (o instanceof Boolean) {
            Boolean b = (Boolean) o;
            return b ? "1" : "0";
        } else if (o == null) {
            return "0";
        }
        throw new RuntimeException("Unhandled default value: " + o.getClass().getSimpleName());
    }

    private static String generateProgram() {
        StringBuilder sb = new StringBuilder();
        sb.append("section .data\n");
        for (DATA d : dataSection)
            if (d.defaultValue instanceof Integer)
                sb.append("    ").append(d.name).append(" dd ").append(d.defaultValue).append(" ; Declare int").append("\n");
            else if (d.defaultValue instanceof Float)
                sb.append("    ").append(d.name).append(" dd ").append(d.defaultValue).append(" ; Declare float").append("\n");
            else if (d.defaultValue instanceof Double)
                sb.append("    ").append(d.name).append(" dq ").append(d.defaultValue).append(" ; Declare double").append("\n");
            else if (d.defaultValue instanceof StringLiteral) {
                StringLiteral literal = (StringLiteral) d.defaultValue;
                sb.append("    ").append(d.name).append(" db \"").append(literal.str).append("\", ").append(literal.ending.getEnding()).append(" ; Declare string").append("\n");
            } else if (d.defaultValue instanceof Character) {
                Character charLit = (Character) d.defaultValue;
                sb.append("    ").append(d.name).append(" db '").append(charLit).append("' ; Declare character").append("\n");
            } else if (d.defaultValue instanceof Boolean) {
                Boolean b = (Boolean) d.defaultValue;
                sb.append("    ").append(d.name).append(" db ").append(b ? "1" : "0").append(" ; Declare boolean").append("\n");
            } else
                throw new RuntimeException("Unhandled default value: " + d.defaultValue);
        sb.append("section .bss\n");
        for (BSS b : bssSection)
            sb.append("    ").append(b.name).append(" resb ").append(b.bytes).append(" ; Reserve ").append(b.bytes).append(" bytes\n");
        sb.append("section .text\n    global _main\n");
        for (String external : externals)
            sb.append("    extern ").append(external).append("\n");
        sb.append("\n_main:\n");
        sb.append(program.toString());
        sb.append(functionSection.toString());

        return sb.toString();
    }

    static void printProgram() {
        System.out.println(generateProgram());
    }

    static void writeToFile(String filename) {
        File file = new File(filename);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file, false));
            writer.write(generateProgram());
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static DataLocation allocScratchRegister(Type t) {
        if (freeRegisters.isEmpty()) {
            printProgram();
            throw new RuntimeException("Out of registers");
        }
        String s = freeRegisters.remove(0);
        usedRegisters.add(s);
        return DataLocation.register(s, t);
    }

    static void freeScratchRegister(String s) {
        freeRegisters.add(0, s);
        usedRegisters.remove(s);
        if (freeRegisters.size() > 3) {
            printProgram();
            throw new RuntimeException("REGISTER FREED TOO MUCH: " + s);
        }
    }
}
