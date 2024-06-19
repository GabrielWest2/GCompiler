package com.gabe;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static String program;


    public static void main(String[] args) throws IOException {
        program = new Scanner(new File("program.glc")).useDelimiter("\\Z").next();

        List<Token> tokens = Tokenizer.tokenize(program, true);
        List<Stmt> stmts = Parser.parse(tokens);
        Emitter.addExternal("_ExitProcess@4");
        Emitter.addExternal("_printf");
        Emitter.addData(new Emitter.DATA("floatreg1", 0.0f));
        Emitter.addData(new Emitter.DATA("floatreg2", 0.0f));
        Emitter.indent();
        stmts.forEach(Codegen::genStmt);
        Emitter.unindent();
        Emitter.writeToFile("test.asm");
        //Emitter.printProgram();
        new ProcessBuilder("C:\\Program Files\\Git\\git-bash.exe", "-c", "source build.bat").start();
    }

    private static String getProgramLine(int linenum) {
        try {
            return StringUtils.split(program, "\n")[linenum - 1];
        } catch (IndexOutOfBoundsException e) {
            return "Invalid line";
        }
    }

    static void compileError(Token t, String message) {
        System.err.println("Compile Error: " + message);
        printErrorLine(t);
    }

    static void parseError(Token t, String message) {
        System.err.println("Parse Error: " + message);
        printErrorLine(t);
    }

    static void typeError(Token t, String message) {
        System.err.println("Type Error: " + message);
        printErrorLine(t);
    }

    private static void printErrorLine(Token t) {
        String lineNum = String.valueOf(t.line());
        System.err.println(lineNum + " | " + getProgramLine(t.line()));
        // Print out arrows pointing at the offending token
        int numSpaces = 3 + lineNum.length() + t.horizontal() - t.lexeme().length();
        System.err.println(" ".repeat(Math.max(0, numSpaces)) + "^^^");
        System.exit(-1);
    }
}