package lexer;

import syspro.tm.lexer.Keyword;
import syspro.tm.lexer.Symbol;

import java.util.ArrayList;

public class Utility {

    public static boolean isKeyword(String word) {
        for (Keyword keyword : Keyword.values()) {
            int[] codePoints = keyword.text.codePoints().toArray();
            String checkedKeyword = new String(codePoints, 0, codePoints.length);
            if (checkedKeyword.equals(word)) return true;
        }
        return false;
    }

    public static boolean isBooleanLiteral(String word) {
        return word.equals("true") || word.equals("false");
    }

    public static boolean isCorrectIndentation(int pos, int length, ArrayList<Integer> codePoints) {
        int spacesLength = 0;
        for (int i = 0; i < length; i++) {
            int currentCodePoint = codePoints.get(pos + i);
            if (currentCodePoint == ' ') {
                spacesLength++;
            }
        }
        return spacesLength % 2 == 0;
    }

    public static boolean isIntegerLiteral(String word) {
        return word.matches("[0-9]+(i32|i64|u32|u64)?");
    }

    public static boolean isRuneLiteral(String word) {
        String SIMPLE_RUNE_CHARACTER = "[^'\r\n]";
        String SHORT_ESCAPE = "\\\\[0abrnvt'\"\\\\]";
        String UNICODE_ESCAPE = "\\\\U\\+[0-9A-F]{4,5}";
        String ESCAPE = "(" + SHORT_ESCAPE + "|" + UNICODE_ESCAPE + ")";
        String RUNE_CHARACTER = "(" + SIMPLE_RUNE_CHARACTER + "|" + ESCAPE + ")";
        String RUNE = "'" + RUNE_CHARACTER + "'";
        return word.matches(RUNE);
    }


    public static boolean isStringLiteral(String word) {
        String SIMPLE_STRING_CHARACTER = "[^\"\r\n]";
        String SHORT_ESCAPE = "\\\\[0abnrtv'\"\\\\]";
        String UNICODE_ESCAPE = "\\\\U\\+[0-9A-F]{4,5}";
        String ESCAPE = "(" + SHORT_ESCAPE + "|" + UNICODE_ESCAPE + ")";
        String STRING_CHARACTER = "(" + SIMPLE_STRING_CHARACTER + "|" + ESCAPE + ")";
        String STRING = "\"" + STRING_CHARACTER + "*\"";
        return word.matches(STRING);
    }

    public static boolean isSymbol(String word) {
        for (Symbol symbol : Symbol.values()) {
            if (symbol.text.equals(word)) {
                return true;
            }
        }
        return false;
    }
}
