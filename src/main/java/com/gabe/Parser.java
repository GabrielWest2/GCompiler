package com.gabe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class Parser {


    private static List<Token> tokens;
    private static int current = 0;

    static List<Stmt> parse(List<Token> tokens) {
        Parser.tokens = tokens;
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) statements.add(declaration());

        return statements;
    }

    // Statement Rules

    private static Stmt declaration() {
        if (match(TokenType.VAR)) return varDeclaration();

        return statement();
    }

    private static Stmt varDeclaration() {
        Token var = consume(TokenType.IDENTIFIER, "Expect variable name.");
        consume(TokenType.COLON, "Expect colon.");
        Token typeName = consume(TokenType.IDENTIFIER, "Expect variable type.");
        Expr init = null;
        if (match(TokenType.EQUAL)) init = expression();
        consume(TokenType.SEMICOLON, "Line should be followed by semicolon");
        return new Stmt.Var(var, init, Type.from(typeName.lexeme()));
    }


    private static Stmt statement() {
        if (match(TokenType.PRINT)) return printStatement();
        if (match(TokenType.LEFT_BRACE)) return new Stmt.Block(block());
        if (match(TokenType.FOR)) return forStatement();
        if (match(TokenType.IF)) return ifStatement();
        if (match(TokenType.WHILE)) return whileStatement();
        if (match(TokenType.DEF)) return function("function");
        if (match(TokenType.RETURN)) return returnStatement();
        return expressionStatement();

    }


    private static Stmt returnStatement() {
        Token rtrn = prev();
        // Optional return value
        Expr val = check(TokenType.SEMICOLON) ? null : expression();
        consume(TokenType.SEMICOLON, "Line should be followed by semicolon");

        return new Stmt.Return(rtrn, val);
    }

    /**
     * Parse function declaration
     *
     * @param type {@code String} containing either 'function' or 'method'
     * @return the parsed function stmt
     */
    private static Stmt.Function function(String type) {
        Token name = consume(TokenType.IDENTIFIER, "Expected name of " + type);
        Token lp = consume(TokenType.LEFT_PAREN, "Expect ( after name of " + type);
        List<GVar> params = new ArrayList<>();
        while (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (params.size() > 255) {
                    Main.parseError(lp, "Method cannot have more than 255 arguments.");
                    return null;
                }
                Token pname = consume(TokenType.IDENTIFIER, "Expected parameter name");
                consume(TokenType.COLON, "Expect colon.");
                Token ptype = consume(TokenType.IDENTIFIER, "Expected parameter type");
                Type t = Type.from(ptype.lexeme());
                params.add(new GVar(pname, t));
            } while (match(TokenType.COMMA));
        }
        consume(TokenType.RIGHT_PAREN, "Expect ) after params of " + type);
        consume(TokenType.COLON, "Expect : after params of " + type);
        Token typTok = consume(TokenType.IDENTIFIER, "Expect return type of " + type);
        Type t = Type.from(typTok.lexeme());
        consume(TokenType.LEFT_BRACE, "Expect { before body of " + type);

        List<Stmt> body = block();
        return new Stmt.Function(name, params, body, t);
    }

    private static Stmt forStatement() {
        consume(TokenType.LEFT_PAREN, "Expect ( after for");
        Stmt initializer = null;
        if (match(TokenType.SEMICOLON)) initializer = null;
        else if (match(TokenType.VAR)) initializer = varDeclaration();
        else initializer = expressionStatement();

        Expr condition = null;
        if (!check(TokenType.SEMICOLON)) condition = expression();
        consume(TokenType.SEMICOLON, "Expect ; after loop condition");

        Expr increment = null;
        if (!check(TokenType.RIGHT_PAREN)) increment = expression();
        consume(TokenType.RIGHT_PAREN, "Expect ) after for clauses");
        Stmt body = statement();

        if (increment != null) body = new Stmt.Block(
                Arrays.asList(
                        body,
                        new Stmt.Expression(increment)));

        if (condition == null) condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body);

        if (initializer != null)
            body = new Stmt.Block(Arrays.asList(initializer, body));

        return body;
    }

    private static Stmt whileStatement() {
        consume(TokenType.LEFT_PAREN, "Expect ( before if condition");
        Expr condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expect ) after if condition");
        Stmt body = statement();

        return new Stmt.While(condition, body);
    }

    private static Stmt ifStatement() {
        consume(TokenType.LEFT_PAREN, "Expect ( before if condition");
        Expr condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expect ) after if condition");
        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(TokenType.ELSE)) elseBranch = statement();
        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private static List<Stmt> block() {
        List<Stmt> stmts = new ArrayList<>();

        while (!isAtEnd() && !check(TokenType.RIGHT_BRACE))
            stmts.add(declaration());
        consume(TokenType.RIGHT_BRACE, "Expect closing brace.");

        return stmts;
    }

    private static Stmt expressionStatement() {
        Expr expr = expression();
        consume(TokenType.SEMICOLON, "Line should be followed by semicolon");
        return new Stmt.Expression(expr);
    }

    private static Stmt printStatement() {
        Expr value = expression();
        consume(TokenType.SEMICOLON, "Line should be followed by semicolon");
        return new Stmt.Print(value);
    }

    // Statement Rules


    // Expression Rules
    private static Expr expression() {
        return assignment();
    }

    //private static Expr

    private static Expr assignment() {
        Expr expr = ternary();
        if (match(TokenType.EQUAL)) {
            Token equals = prev();
            Expr value = assignment();
            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable) expr).name;
                return new Expr.Assign(name, value);
            }
            Main.parseError(equals, "Invalid assignment target");
        }

        return expr;
    }

    private static Expr ternary() {
        Expr cond = or();
        if (match(TokenType.QUESTION)) {
            Token symbol = prev();
            Expr exp1 = expression();
            consume(TokenType.COLON, "Expect colon separating expressions");
            Expr exp2 = expression();
            return new Expr.Ternary(cond, symbol, exp1, exp2);
        }
        return cond;
    }

    private static Expr or() {
        Expr expr = xor();
        while (match(TokenType.OR)) {
            Token operator = prev();
            Expr right = xor();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    private static Expr xor() {
        Expr expr = and();
        while (match(TokenType.XOR)) {
            Token operator = prev();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    private static Expr and() {
        Expr expr = equality();
        while (match(TokenType.AND)) {
            Token operator = prev();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    private static Expr equality() {
        Expr expr = comparison();
        while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
            Token operator = prev();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private static Expr comparison() {
        Expr expr = term();
        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            Token op = prev();
            Expr right = term();
            expr = new Expr.Binary(expr, op, right);
        }
        return expr;
    }

    private static Expr term() {
        Expr expr = factor();
        while (match(TokenType.MINUS, TokenType.PLUS)) {
            Token operator = prev();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private static Expr factor() {
        Expr expr = unary();
        while (match(TokenType.SLASH, TokenType.STAR, TokenType.PERCENT)) {
            Token operator = prev();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private static Expr unary() {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            Token operator = prev();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return call();
    }

    private static Expr call() {
        Expr expr = primary();
        if (expr instanceof Expr.Variable && match(TokenType.LEFT_PAREN)) {
            Token name = ((Expr.Variable) expr).name;
            expr = finishCall(expr, name);
        }

        return expr;
    }


    private static Expr primary() {
        if (match(TokenType.FALSE)) return new Expr.Literal(false);
        if (match(TokenType.TRUE)) return new Expr.Literal(true);
        if (match(TokenType.NULL)) return new Expr.Literal(null);
        if (match(TokenType.NUMBER)) {
            Double d = (Double) prev().literal();
            if (d % 1 == 0) return new Expr.Literal((int) d.doubleValue());
            return new Expr.Literal(d);
        }
        if (match(TokenType.STRING)) return new Expr.Literal(prev().literal());
        if (match(TokenType.CHAR)) return new Expr.Literal(prev().literal());

        if (match(TokenType.IDENTIFIER)) return new Expr.Variable(prev());

        if (match(TokenType.LEFT_PAREN)) {
            Expr expr = expression();
            consume(TokenType.RIGHT_PAREN, "Expected closing parentheses.");
            return new Expr.Grouping(expr);
        }

        Main.parseError(curr(), "Unexpected token: " + curr());
        return null;
    }


    private static Expr finishCall(Expr expr, Token name) {
        List<Expr> arguments = new ArrayList<>();

        if (!check(TokenType.RIGHT_PAREN)) do {
            arguments.add(expression());
            if (arguments.size() > 255)
                Main.parseError(prev(), "Method cannot have more than 255 arguments.");
        } while (match(TokenType.COMMA));

        Token paren = consume(TokenType.RIGHT_PAREN, "Expect ) after arguments");

        return new Expr.Call(expr, paren, name, arguments);
    }


    //////////////// Token matching utils
    private static boolean match(TokenType... types) {
        for (TokenType t : types)
            if (check(t)) {
                advance();
                return true;
            }

        return false;
    }

    private static Token consume(TokenType type, String err) {
        if (match(type)) return prev();

        Main.parseError(curr(), err);
        return null;
    }

    private static void advance() {
        if (!isAtEnd()) current++;
    }

    private static boolean check(TokenType t) {
        if (isAtEnd()) return false;
        return tokens.get(current).tokenType() == t;
    }

    private static Token curr() {
        return tokens.get(current);
    }

    private static boolean isAtEnd() {
        return peek().tokenType() == TokenType.EOF;
    }

    private static Token peek() {
        return tokens.get(current);
    }

    private static Token prev() {
        return tokens.get(current - 1);
    }


}
