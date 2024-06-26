package com.gabe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Codegen implements Expr.Visitor<DataLocation>, Stmt.Visitor<Void> {

    private static final Codegen instance;
    public static Environment currentEnvironment;

    private static GFunction currentFunction = null;


    static {
        instance = new Codegen();
        currentEnvironment = new Environment(null);
        // TODO find a better way to do this
        currentEnvironment.defineFunction(new Token(null, "_printf", null, -1, -1), new GFunction("_printf", Type.VOID, Collections.EMPTY_LIST, true));
    }

    static DataLocation genExpr(Expr expr) {
        return expr.accept(instance);
    }

    static void genStmt(Stmt stmt) {
        boolean spacing = stmt.getClass() != Stmt.Var.class && stmt.getClass() != Stmt.Block.class;
        if (spacing) {
            Emitter.emitln();
            Emitter.emitln("; " + stmt.getClass().getSimpleName());
        }
        stmt.accept(instance);
        if (spacing)
            Emitter.emitln();
    }


    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        for (Stmt s : stmt.statements) genStmt(s);
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        DataLocation reg = Codegen.genExpr(stmt.expression);
        //Trash value;
        reg.freeScratchRegister();
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        String name = stmt.name.lexeme();
        Type t = stmt.returnType;

        GFunction func = new GFunction(stmt.name.lexeme(), t, stmt.params, false);
        currentEnvironment.defineFunction(stmt.name, func);

        // Start function state
        Emitter.inFunctionSection = true;
        GFunction prevFunction = currentFunction;
        currentFunction = func;
        currentEnvironment = new Environment(currentEnvironment);

        //TODO funcs in funcs?

        Emitter.defineLabel(name);
        Emitter.emitln("; Prologue");
        Emitter.emitln("push ebp ; Push base pointer");
        Emitter.emitln("mov ebp, esp ; Move stack ptr to base ptr");


        for (int i = 0; i < stmt.params.size(); i++) {
            int offset = (i + 2) * 4;
            GVar param = stmt.params.get(i);
            DataLocation paramLoc = DataLocation.stack(offset, param.type);
            Emitter.emitln("  ; " + param.name + "  at  " + paramLoc);
            currentEnvironment.defineVar(param.name, param, paramLoc);
        }
        int stmtCount = stmt.body.size();
        boolean returnAtEnd = false;
        for (int i = 0; i < stmtCount; i++) {
            Stmt statement = stmt.body.get(i);
            if (statement instanceof Stmt.Return && i != stmtCount - 1) {
                //TODO make warning?
                Main.compileError(((Stmt.Return) statement).keyword, "Unreachable code.");
            } else if (statement instanceof Stmt.Return && i == stmtCount - 1) {
                returnAtEnd = true;
            }

            genStmt(statement);
        }


        if (!returnAtEnd) {
            Emitter.epilogue();
        }

        // Return to previous state
        currentEnvironment = currentEnvironment.parent;
        currentFunction = prevFunction;
        Emitter.inFunctionSection = false;

        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        DataLocation condition = Codegen.genExpr(stmt.condition);
        String ifTrueLabel = Emitter.getNextLabelName(Emitter.LabelType.IF_STMT_TRUE);
        String ifEndLabel = Emitter.getNextLabelName(Emitter.LabelType.IF_STMT_END);
        Emitter.emitln();
        Emitter.emitln("cmp " + condition.asSource() + ", 1 ; Check if statement condition");
        condition.freeScratchRegister();
        Emitter.emitln("je " + ifTrueLabel + " ; Jump to true label");
        if (stmt.elseBranch != null) genStmt(stmt.elseBranch);
        Emitter.emitln("jmp " + ifEndLabel + " ; Jump to end, condition was false");
        Emitter.defineLabel(ifTrueLabel);
        genStmt(stmt.thenBranch);
        Emitter.defineLabel(ifEndLabel);
        Emitter.emitln();
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        String startLabel = Emitter.getNextLabelName(Emitter.LabelType.WHILE_BEGIN);
        String endLabel = Emitter.getNextLabelName(Emitter.LabelType.WHILE_END);
        Emitter.defineLabel(startLabel);
        DataLocation cond = Codegen.genExpr(stmt.condition);
        cond.freeScratchRegister();
        Emitter.emitln("test " + cond.asSource() + ", " + cond.asSource() + " ; Check for false (0)");
        Emitter.emitln("jz " + endLabel + " ; Jump to end if false");
        genStmt(stmt.body);
        Emitter.emitln("jmp " + startLabel + " ; Jump to start, try again");
        Emitter.defineLabel(endLabel);

        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        // TODO resolve type of stmt, not assume int
        DataLocation value = Codegen.genExpr(stmt.expression);
        value.pushOntoStack(true, "Push value to print onto stack");
        value.freeScratchRegister();
        DataLocation formatSpecifier;
        String end = stmt.token.lexeme().equals("println") ? "\n" : "";
        System.out.println(stmt.token.lexeme());
        switch (value.getType()) {
            case CHAR ->
                    formatSpecifier = Emitter.declareStringConstant("%c" + end);
            case SHORT ->
                    formatSpecifier = Emitter.declareStringConstant("%h" + end);
            case INT, BOOL ->
                    formatSpecifier = Emitter.declareStringConstant("%d" + end);
            case FLOAT ->
                    formatSpecifier = Emitter.declareStringConstant("%f" + end);
            case STRING ->
                    formatSpecifier = Emitter.declareStringConstant("%s" + end);
            default -> formatSpecifier = null;
        }

        formatSpecifier.pushOntoStack(false, "Push format string");
        Emitter.emitln("call _printf ; Call print");
        Emitter.emitln("add esp, 8 ; Clean up the stack");
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Type t = stmt.type;
        String name = stmt.name.lexeme();

        if (currentEnvironment.findVar(name) != null)
            Main.compileError(stmt.name, "Variable already defined.");


        DataLocation varLoc;
        if (currentEnvironment.isGlobal()) {
            varLoc = DataLocation.data(name, t);
        } else {
            currentEnvironment.currentLocalVarStackOffset -= 4;
            varLoc = DataLocation.stack(currentEnvironment.currentLocalVarStackOffset, t);
        }
        GVar var = new GVar(stmt.name, t);
        System.out.println("var " + var.name);
        currentEnvironment.defineVar(stmt.name, var, varLoc);


        if (stmt.initializer instanceof Expr.Literal) {
            Expr.Literal litStmt = ((Expr.Literal) stmt.initializer);
            if (t != Type.from(litStmt.value))
                Main.typeError(stmt.name, "Incompatible types");

            if (currentEnvironment.isGlobal()) {
                Emitter.addData(new Emitter.DATA(name, litStmt.value));
            } else {
                System.out.println("Local var: " + var.name);
                Emitter.emitln("sub esp, 4 ; Allocate space for local var '" + name + "'");
                Emitter.emitln("mov dword " + varLoc + ", " + Emitter.literalString(litStmt.value) + " ; Store value in local var");
            }
        } else if (stmt.initializer != null) {
            if (!currentEnvironment.isGlobal()) {
                Emitter.emitln("sub esp, 4 ; Allocate space for local var '" + name + "'");
            }
            Emitter.addData(new Emitter.DATA(name, t == Type.FLOAT ? 0.0f : 0));
            DataLocation valReg = Codegen.genExpr(stmt.initializer);

            if (t != valReg.getType())
                Main.typeError(stmt.name, "Incompatible types: " + t + " != " + valReg.getType());

            valReg.freeScratchRegister();
            valReg.moveTo(varLoc, "Move value into variable");

        } else {
            if (!currentEnvironment.isGlobal()) {
                Emitter.emitln("sub esp, 4 ; Allocate space for local var '" + name + "'");
            }
            Emitter.addData(new Emitter.DATA(name, t == Type.FLOAT ? 0.0f : 0));
        }

        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        //TODO check if method is void, check return type
        if (currentEnvironment.isGlobal()) {
            DataLocation returnReg = Codegen.genExpr(stmt.value);
            returnReg.pushOntoStack(false, "Push exit code");
            Emitter.emitln("call _ExitProcess@4 ; Exit program");
        } else {
            if (stmt.value != null) {
                DataLocation returnReg = Codegen.genExpr(stmt.value);
                returnReg.freeScratchRegister();

                Type returnType = currentFunction.type;
                if (returnType != returnReg.getType()) {
                    Main.typeError(stmt.keyword, "Incompatible types");
                }

                DataLocation eaxReg = DataLocation.register("eax", returnType);
                returnReg.moveTo(eaxReg, "Put return value in eax");
            }
            Emitter.epilogue();
        }
        return null;
    }


    /***********************************
     EXPRESSIONS
     ***********************************/
    @Override
    public DataLocation visitAssignExpr(Expr.Assign expr) {
        DataLocation valReg = genExpr(expr.value);

        String name = expr.name.lexeme();
        Type t = TypeResolver.resolveType(expr.value);
        GVar v = currentEnvironment.findVar(name);
        DataLocation varLoc = currentEnvironment.findVarLocation(name);
        if (v == null) Main.compileError(expr.name, "Variable not found");
        Type varType = v.type;
        if (t != varType) Main.typeError(expr.name, "Incompatible types");


        valReg.moveTo(varLoc, "Assign variable");
        //TODO maybe illegal?
        return valReg;
    }


    @Override
    public DataLocation visitBinaryExpr(Expr.Binary expr) {
        DataLocation leftReg = genExpr(expr.left);
        DataLocation rightReg = genExpr(expr.right);

        Type type = TypeResolver.resolveType(expr);

        // Used in comparisons
        DataLocation outReg;
        String cmpTrueLabel;
        String cmpEndLabel;
        switch (expr.operator.tokenType()) {
            case PLUS -> {
                if (type == Type.INT) {
                    Emitter.emitln("add " + leftReg.asSource() + ", " + rightReg.asSource());
                    rightReg.freeScratchRegister();
                    return leftReg;
                } else if (type == Type.FLOAT) {
                    DataLocation lrOut = this.loadFloatIntValues(leftReg, rightReg);

                    Emitter.emitln("faddp ; Pop 2 numbers, then push sum on stack");
                    outReg = Emitter.allocFloatTmp(Type.FLOAT);
                    Emitter.emitln("fstp dword " + outReg + " ; Pop sum from stack and store in out");
                    outReg.moveTo(lrOut, "Move sum to out reg");
                    return lrOut;
                } else {
                    throw new RuntimeException("Unsupported type: " + type);
                }
            }
            case MINUS -> {
                if (type == Type.INT) {
                    Emitter.emitln("sub " + leftReg.asSource() + ", " + rightReg.asSource());
                    rightReg.freeScratchRegister();
                    return leftReg;
                } else if (type == Type.FLOAT) {
                    DataLocation lrOut = this.loadFloatIntValues(leftReg, rightReg);

                    Emitter.emitln("fsubp ; Pop 2 numbers, then push diff on stack");
                    outReg = Emitter.allocFloatTmp(Type.FLOAT);
                    Emitter.emitln("fstp dword " + outReg + " ; Pop diff from stack and store in out");
                    outReg.moveTo(lrOut, "Move diff to out reg");
                    return lrOut;
                } else {
                    throw new RuntimeException("Unsupported type: " + type);
                }
            }
            case STAR -> {
                if (type == Type.INT) {
                    Emitter.emitln("imul " + leftReg.asSource() + ", " + rightReg.asSource());
                    rightReg.freeScratchRegister();
                    Emitter.emitln();
                    return leftReg;
                } else if (type == Type.FLOAT) {
                    DataLocation lrOut = this.loadFloatIntValues(leftReg, rightReg);

                    Emitter.emitln("fmulp ; Pop 2 numbers, then push product on stack");
                    outReg = Emitter.allocFloatTmp(Type.FLOAT);
                    Emitter.emitln("fstp dword " + outReg + " ; Pop product from stack and store in out");
                    outReg.moveTo(lrOut, "Move product to out reg");
                    return lrOut;
                }
            }
            case SLASH -> {
                if (type == Type.INT) {
                    Emitter.emitln("mov eax, " + leftReg.asSource());
                    Emitter.emitln("cdq ; Sign Extend");
                    Emitter.emitln("idiv " + rightReg.unconstantify());
                    leftReg.freeScratchRegister();
                    rightReg.freeScratchRegister();

                    DataLocation out = Emitter.allocScratchRegister(Type.INT);
                    DataLocation eax = DataLocation.register("eax", Type.INT);
                    eax.moveTo(out, "Store output of division");
                    return out;
                } else if (type == Type.FLOAT) {
                    DataLocation lrOut = this.loadFloatIntValues(leftReg, rightReg);

                    Emitter.emitln("fdivp ; Pop 2 numbers, then push quot on stack");
                    outReg = Emitter.allocFloatTmp(Type.FLOAT);
                    Emitter.emitln("fstp dword " + outReg + " ; Pop quot from stack and store in out");
                    outReg.moveTo(lrOut, "Move quot to out reg");
                    return lrOut;
                }
            }
            case PERCENT -> {
                Emitter.emitln("mov eax, " + leftReg.asSource());
                Emitter.emitln("cdq ; Sign Extend");
                Emitter.emitln("idiv " + rightReg.unconstantify());
                leftReg.freeScratchRegister();
                rightReg.freeScratchRegister();
                DataLocation remainder = Emitter.allocScratchRegister(Type.INT);
                DataLocation edx = DataLocation.register("edx", Type.INT);
                edx.moveTo(remainder, "Store remainder of division");
                return remainder;
            }
            case GREATER -> {
                Emitter.emitln("cmp " + leftReg + ", " + rightReg + " ; Compare value in register " + leftReg + " with register " + rightReg);
                leftReg.freeScratchRegister();
                rightReg.freeScratchRegister();
                outReg = Emitter.allocScratchRegister(Type.BOOL);
                cmpTrueLabel = Emitter.getNextLabelName(Emitter.LabelType.CMP_IS_TRUE);
                cmpEndLabel = Emitter.getNextLabelName(Emitter.LabelType.CMP_END);
                Emitter.emitln("jg " + cmpTrueLabel + " ; Jump if " + leftReg + " > " + rightReg);
                Emitter.emitln("mov " + outReg + ", 0  ; Set " + outReg + " to 0 because " + leftReg + " <= " + rightReg);
                Emitter.emitln("jmp " + cmpEndLabel + " ; Jump to end of compare, " + outReg + " has been set");
                Emitter.defineLabel(cmpTrueLabel);
                Emitter.emitln("mov " + outReg + ", 1 ; Set " + outReg + " to 1 because " + leftReg + " > " + rightReg);
                Emitter.defineLabel(cmpEndLabel);
                return outReg;
            }
            case LESS -> {
                Emitter.emitln("cmp " + leftReg + ", " + rightReg + " ; Compare value in register " + leftReg + " with register " + rightReg);
                leftReg.freeScratchRegister();
                rightReg.freeScratchRegister();
                outReg = Emitter.allocScratchRegister(Type.BOOL);
                cmpTrueLabel = Emitter.getNextLabelName(Emitter.LabelType.CMP_IS_TRUE);
                cmpEndLabel = Emitter.getNextLabelName(Emitter.LabelType.CMP_END);
                Emitter.emitln("jl " + cmpTrueLabel + " ; Jump if " + leftReg + " < " + rightReg);
                Emitter.emitln("mov " + outReg + ", 0 ; Set " + outReg + " to 0 because " + leftReg + " >= " + rightReg);
                Emitter.emitln("jmp " + cmpEndLabel + " ; Jump to end of compare, " + outReg + " has been set");
                Emitter.defineLabel(cmpTrueLabel);
                Emitter.emitln("mov " + outReg + ", 1 ; Set " + outReg + " to 1 because " + leftReg + " < " + rightReg);
                Emitter.defineLabel(cmpEndLabel);
                return outReg;
            }
            case GREATER_EQUAL -> {
                Emitter.emitln("cmp " + leftReg + ", " + rightReg + " ; Compare value in register " + leftReg + " with register " + rightReg);
                leftReg.freeScratchRegister();
                rightReg.freeScratchRegister();
                outReg = Emitter.allocScratchRegister(Type.BOOL);
                cmpTrueLabel = Emitter.getNextLabelName(Emitter.LabelType.CMP_IS_TRUE);
                cmpEndLabel = Emitter.getNextLabelName(Emitter.LabelType.CMP_END);
                Emitter.emitln("jl " + cmpTrueLabel + " ; Jump if " + leftReg + " < " + rightReg);
                Emitter.emitln("mov " + outReg + ", 1 ; Set " + outReg + " to 1 because " + leftReg + " >= " + rightReg);
                Emitter.emitln("jmp " + cmpEndLabel + " ; Jump to end of compare, " + outReg + " has been set");
                Emitter.defineLabel(cmpTrueLabel);
                Emitter.emitln("mov " + outReg + ", 0 ; Set " + outReg + " to 0 because " + leftReg + " < " + rightReg);
                Emitter.defineLabel(cmpEndLabel);
                return outReg;
            }
            case LESS_EQUAL -> {
                Emitter.emitln("cmp " + leftReg + ", " + rightReg + " ; Compare value in register " + leftReg + " with register " + rightReg);
                leftReg.freeScratchRegister();
                rightReg.freeScratchRegister();
                outReg = Emitter.allocScratchRegister(Type.BOOL);
                cmpTrueLabel = Emitter.getNextLabelName(Emitter.LabelType.CMP_IS_TRUE);
                cmpEndLabel = Emitter.getNextLabelName(Emitter.LabelType.CMP_END);
                Emitter.emitln("jg " + cmpTrueLabel + " ; Jump if " + leftReg + " > " + rightReg);
                Emitter.emitln("mov " + outReg + ", 1  ; Set " + outReg + " to 1 because " + leftReg + " <= " + rightReg);
                Emitter.emitln("jmp " + cmpEndLabel + " ; Jump to end of compare, " + outReg + " has been set");
                Emitter.defineLabel(cmpTrueLabel);
                Emitter.emitln("mov " + outReg + ", 0 ; Set " + outReg + " to 0 because " + leftReg + " > " + rightReg);
                Emitter.defineLabel(cmpEndLabel);
                return outReg;
            }
            case EQUAL_EQUAL -> {
                Emitter.emitln("cmp " + leftReg + ", " + rightReg + " ; Compare value in register " + leftReg + " with register " + rightReg);
                leftReg.freeScratchRegister();
                rightReg.freeScratchRegister();
                outReg = Emitter.allocScratchRegister(Type.BOOL);
                cmpTrueLabel = Emitter.getNextLabelName(Emitter.LabelType.CMP_IS_TRUE);
                cmpEndLabel = Emitter.getNextLabelName(Emitter.LabelType.CMP_END);
                Emitter.emitln("je " + cmpTrueLabel + " ; Jump if " + leftReg + " == " + rightReg);
                Emitter.emitln("mov " + outReg + ", 0 ; Set " + outReg + " to 0 because " + leftReg + " != " + rightReg);
                Emitter.emitln("jmp " + cmpEndLabel + " ; Jump to end of compare, " + outReg + " has been set");
                Emitter.defineLabel(cmpTrueLabel);
                Emitter.emitln("mov " + outReg + ", 1 ; Set " + outReg + " to 1 because " + leftReg + " == " + rightReg);
                Emitter.defineLabel(cmpEndLabel);
                return outReg;
            }
            case BANG_EQUAL -> {
                Emitter.emitln("cmp " + leftReg + ", " + rightReg + " ; Compare value in register " + leftReg + " with register " + rightReg);
                leftReg.freeScratchRegister();
                rightReg.freeScratchRegister();
                outReg = Emitter.allocScratchRegister(Type.BOOL);
                cmpTrueLabel = Emitter.getNextLabelName(Emitter.LabelType.CMP_IS_TRUE);
                cmpEndLabel = Emitter.getNextLabelName(Emitter.LabelType.CMP_END);
                Emitter.emitln("je " + cmpTrueLabel + " ; Jump if " + leftReg + " == " + rightReg);
                Emitter.emitln("mov " + outReg + ", 1 ; Set " + outReg + " to 1 because " + leftReg + " != " + rightReg);
                Emitter.emitln("jmp " + cmpEndLabel + " ; Jump to end of compare, " + outReg + " has been set");
                Emitter.defineLabel(cmpTrueLabel);
                Emitter.emitln("mov " + outReg + ", 0 ; Set " + outReg + " to 0 because " + leftReg + " == " + rightReg);
                Emitter.defineLabel(cmpEndLabel);
                return outReg;
            }
        }
        throw new RuntimeException("Unreachable code. Binary operator: " + expr.operator.tokenType());
    }

    private DataLocation loadFloatIntValues(DataLocation leftReg, DataLocation rightReg) {
        DataLocation lrOut;
        if (leftReg.getType() == Type.FLOAT) {
            Emitter.emitln("fld dword " + leftReg.floatdatify(Type.FLOAT) + " ; Load num1");
            lrOut = leftReg;
            rightReg.freeScratchRegister();

            if (rightReg.getType() == Type.FLOAT) {
                Emitter.emitln("fld dword " + rightReg.floatdatify(Type.FLOAT) + " ; Load num2");
            } else if (rightReg.getType() == Type.INT) {
                Emitter.emitln("fild dword " + rightReg.floatdatify(Type.INT) + " ; Load num2");
            } else {
                throw new RuntimeException("Unsupported type: " + rightReg.getType());
            }

        } else if (leftReg.getType() == Type.INT) {
            Emitter.emitln("fild dword " + leftReg.floatdatify(Type.INT) + " ; Load num1");
            Emitter.emitln("fld dword " + rightReg.floatdatify(Type.FLOAT) + " ; Load num2");
            lrOut = rightReg;
            leftReg.freeScratchRegister();
        } else {
            throw new RuntimeException("Unsupported type: " + leftReg.getType());
        }
        return lrOut;
    }

    @Override
    public DataLocation visitCallExpr(Expr.Call expr) {
        List<String> usedRegisters = new ArrayList<>(Emitter.usedRegisters);
        Emitter.emitln("; JUST BEFORE CALL   vars: " + usedRegisters);
        for (int i = 0; i < usedRegisters.size(); i++) {
            Emitter.emitln("push " + usedRegisters.get(i) + " ; Stop brutal clobbering of my very important data");
        }

        GFunction func = currentEnvironment.findFunction(expr.name.lexeme());

        if (func == null) {
            Main.compileError(expr.name, "Function not defined");
        }
        Type returnType = func.type;

        // Push arguments onto the stack in reverse order
        for (int i = expr.arguments.size() - 1; i >= 0; i--) {
            Expr e = expr.arguments.get(i);
            DataLocation s = genExpr(e);

            if (!func.varargs) {
                Type paramType = func.params.get(i).type;
                if (paramType != s.getType()) {
                    Main.compileError(expr.paren, "Argument " + i + " type mismatch (" + func.params.get(i).name.lexeme() + " " + func.params.get(i).type + "). " + paramType + " != " + s.getType().toString());
                }
            }
            s.pushOntoStack(func.varargs, "Push function arg " + i + " onto stack");
            s.freeScratchRegister();
        }

        Emitter.emitln("call " + expr.name.lexeme() + " ; Result will be in eax");

        DataLocation out = Emitter.allocScratchRegister(returnType);
        //TODO double stuff?
        Emitter.emitln("add esp, " + (expr.arguments.size() * 4) + " ; Clean up the stack");


        for (int i = usedRegisters.size() - 1; i >= 0; i--) {
            Emitter.emitln("pop " + usedRegisters.get(i));
        }


        if (returnType != Type.VOID) {
            DataLocation eax = DataLocation.register("eax", returnType);
            eax.moveTo(out, "Move return value to " + out);

            return out;
        }
        return out;
    }

    @Override
    public DataLocation visitGroupingExpr(Expr.Grouping expr) {
        return genExpr(expr.expression);
    }

    @Override
    public DataLocation visitLiteralExpr(Expr.Literal expr) {
        if (expr.value instanceof String) {
            DataLocation reg = Emitter.allocScratchRegister(Type.STRING);
            DataLocation store = Emitter.declareStringConstant(expr.value.toString());

            store.moveTo(reg, "Store memory address of string");
            return reg;
        }

        if (expr.value instanceof Float f) {
            DataLocation reg = Emitter.allocScratchRegister(Type.FLOAT);
            DataLocation store = Emitter.declareFloatConstant(f);

            store.moveTo(reg, "Store memory address of string");
            return reg;
        }

        return DataLocation.constant(expr.value, Type.from(expr.value));
    }

    @Override
    public DataLocation visitLogicalExpr(Expr.Logical expr) {
        //TODO check for numeric types, and pass correct into ds
        DataLocation leftReg = genExpr(expr.left);
        DataLocation rightReg = genExpr(expr.right);

        switch (expr.operator.tokenType()) {
            case AND:
                Emitter.emitln("and " + leftReg + ", " + rightReg + " ; And " + leftReg + " with " + rightReg);
                rightReg.freeScratchRegister();
                return leftReg;
            case OR:
                Emitter.emitln("or " + leftReg + ", " + rightReg + " ; Or " + leftReg + " with " + rightReg);
                rightReg.freeScratchRegister();
                return leftReg;
            case XOR:
                Emitter.emitln("xor " + leftReg + ", " + rightReg + " ; Xor " + leftReg + " with " + rightReg);
                rightReg.freeScratchRegister();
                return leftReg;
        }

        throw new RuntimeException("Unhandled logical operator: " + expr.operator.getClass().getSimpleName());
    }

    @Override
    public DataLocation visitUnaryExpr(Expr.Unary expr) {
        switch (expr.operator.tokenType()) {
            case MINUS:
                //TODO check right is a num
                DataLocation reg = genExpr(expr.right);
                Emitter.emitln("neg " + reg + " ; Negate value in " + reg);
                return reg;
            case BANG:
                DataLocation r = genExpr(expr.right);
                //TODO check right is a boolean
                Emitter.emitln("xor " + r + ", 1 ; Invert boolean " + r);
                return r;
        }

        throw new RuntimeException("Unhandled unary type: " + expr.right.getClass().getSimpleName());
    }

    @Override
    public DataLocation visitTernaryExpr(Expr.Ternary expr) {
        DataLocation conditionReg = genExpr(expr.cond);
        Emitter.emitln("cmp " + conditionReg + ", 1 ; Check condition for ternary");
        conditionReg.freeScratchRegister();

        String ternaryTrueLabel = Emitter.getNextLabelName(Emitter.LabelType.CMP_IS_TRUE);
        String ternaryEndLabel = Emitter.getNextLabelName(Emitter.LabelType.CMP_END);
        Emitter.emitln("je " + ternaryTrueLabel + " ; Jump if condition is true");
        DataLocation outReg = genExpr(expr.right);
        Emitter.emitln("jmp " + ternaryEndLabel + " ; Jump to end of ternary");
        Emitter.defineLabel(ternaryTrueLabel);
        DataLocation outReg2 = genExpr(expr.left);
        if (!outReg2.equals(outReg)) {
            outReg2.moveTo(outReg, " ; Move result value into " + outReg);
            outReg2.freeScratchRegister();
        }
        Emitter.defineLabel(ternaryEndLabel);
        return outReg;
    }

    @Override
    public DataLocation visitVariableExpr(Expr.Variable expr) {
        Type t = TypeResolver.resolveType(expr);
        DataLocation reg = Emitter.allocScratchRegister(t);
        DataLocation var = currentEnvironment.findVarLocation(expr.name.lexeme());

        if (var == null) Main.compileError(expr.name, "Variable not defined");


        var.moveTo(reg, "Store variable in register");
        return reg;
    }


}
