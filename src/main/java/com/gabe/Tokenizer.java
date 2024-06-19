package com.gabe;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Tokenizer {
    private static String program;

    /**
     * Tokenizes the input string
     *
     * @return a {@code List<Token>} containing the tokens
     */
    static List<Token> tokenize(String program, boolean eof) {
        Tokenizer.program = program;
        List<Token> tokens = new ArrayList<>();
        String s = program;
        int lineNum = 1;
        int charNum = 0;

        while (!s.isEmpty()) {
            boolean consumed = false;
            token_search:
            for (TokenType t : TokenType.values())
                for (String regex : t.getRegexen()) {
                    Pattern pattern = Pattern.compile(regex);
                    Matcher matcher = pattern.matcher(s);
                    if (matcher.find()) {
                        String match = matcher.group(0);
                        String removed = s.substring(0, match.length());

                        int matches = StringUtils.countMatches(removed, "\n");
                        lineNum += matches;
                        if (matches > 0) charNum = 0;

                        for (int i = removed.length() - 1; i >= 0; i--)
                            if (removed.charAt(i) != '\n') charNum++;
                            else break;

                        s = s.substring(match.length());
                        consumed = true;
                        switch (t) {
                            case EMPTY -> {
                                break token_search;
                            }
                            case NUMBER -> {
                                Object num;
                                if (match.endsWith("f")) {
                                    num = Float.parseFloat(match.substring(0, match.length() - 1));
                                } else if (match.endsWith("d")) {
                                    num = Double.parseDouble(match.substring(0, match.length() - 1));
                                } else {
                                    num = Integer.parseInt(match);
                                }

                                tokens.add(new Token(t, match, num, lineNum, charNum));
                                break token_search;
                            }
                            case STRING -> {
                                String val = match.substring(1, match.length() - 1);
                                val = val.replaceAll("\\\\\\\"", "\"");
                                val = val.replaceAll("\\\\n", "\n");
                                val = val.replaceAll("\\\\r", "\r");
                                tokens.add(new Token(t, match, val, lineNum, charNum));
                            }
                            case CHAR -> {
                                String val = match.substring(1, match.length() - 1);
                                tokens.add(new Token(t, match, val.charAt(0), lineNum, charNum));
                            }
                            default -> {
                                tokens.add(new Token(t, match, null, lineNum, charNum));
                                break token_search;
                            }
                        }
                    }
                }

            if (!consumed) {
                String lexeme = s.substring(0, 1);
                Main.parseError(new Token(TokenType.EMPTY, lexeme, null, lineNum, charNum), "Invalid token: '" + lexeme + "'");
                return null;
            }
        }

        if (eof)
            tokens.add(new Token(TokenType.EOF, "", null, lineNum, charNum));
        return tokens;
    }
}
