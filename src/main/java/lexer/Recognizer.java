package lexer;

import syspro.tm.lexer.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static javax.lang.model.SourceVersion.isIdentifier;
import static lexer.Utility.*;

public class Recognizer {
    public static int getWhitespaceLength(int pos, int end, ArrayList<Integer> codePoints) {
        int length = 0;
        while (pos <= end && (codePoints.get(pos) == ' ' || codePoints.get(pos) == '\t')) {
            length++;
            pos++;
        }
        return length;
    }

    public static int getNewLineLength(int pos, int end, ArrayList<Integer> codePoints) {
        if (pos > end) {
            return 0;
        } else if (codePoints.get(pos) == '\n') {
            return 1;
        } else if ((pos + 1 <= end) && (codePoints.get(pos) == '\r' && codePoints.get(pos + 1) == '\n')) {
            return 2;
        }
        return 0;
    }

    public static int getCommentLength(int pos, int end, ArrayList<Integer> codePoints) {
        if (pos > end || codePoints.get(pos) != '#') return 0;
        int length = 1;
        pos++;
        while (pos <= end) {
            if (codePoints.get(pos) == '\r' && pos + 1 <= end && codePoints.get(pos + 1) == '\n') {
                break;
            } else if (codePoints.get(pos) == '\n') {
                break;
            }
            length++;
            pos++;
        }
        return length;
    }

    private static int getTriviaLength(int pos, int end, ArrayList<Integer> codePoints) {
        int length = 0;

        int whitespaceLength = getWhitespaceLength(pos, end, codePoints);
        int commentLength = getCommentLength(pos, end, codePoints);
        int newLineLength = getNewLineLength(pos, end, codePoints);

        while (pos <= end && whitespaceLength + commentLength + newLineLength > 0){
            length += whitespaceLength + commentLength + newLineLength;
            pos += whitespaceLength + commentLength + newLineLength;
            whitespaceLength = getWhitespaceLength(pos, end, codePoints);
            commentLength = getCommentLength(pos, end, codePoints);
            newLineLength = getNewLineLength(pos, end, codePoints);

        }
        return length;
    }

    private static String getString(int start, int end, ArrayList<Integer> codePoints) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i <= end; i++) {
            sb.appendCodePoint(codePoints.get(i));
        }
        return sb.toString();
    }

    public static Token recognize(int start, int end, ArrayList<Integer> codePoints) {
        String word = getString(start, end, codePoints);

        if (isKeyword(word)) {
            Keyword keyword = Keyword.valueOf(word.toUpperCase());
            return new syspro.tm.lexer.KeywordToken(start, end, 0, 0, keyword);
        }

        if (isSymbol(word)) {
            Symbol symbol = Arrays.stream(Symbol.values())
                    .filter(s -> s.text.equals(word))
                    .findFirst()
                    .orElse(null);
            return new syspro.tm.lexer.SymbolToken(start, end, 0, 0, symbol);
        }

        if (isBooleanLiteral(word)) {
            if (word.equals("true")) {
                return new syspro.tm.lexer.BooleanLiteralToken(start, end, 0, 0,
                        true);
            }
            return new syspro.tm.lexer.BooleanLiteralToken(start, end, 0, 0,
                    false);
        }

        if (isIntegerLiteral(word)) {
            boolean hasTypeSuffix = false;
            BuiltInType type = BuiltInType.INT64;

            String regex = "(\\d+)(i32|i64|u32|u64)?";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(word);

            String suffix = null;

            if (matcher.matches()) {
                suffix = matcher.group(2);
            }

            String valueString = word;
            Map<String, BuiltInType> stringTypesToBuiltInTypes = Map.of(
                    "i32", BuiltInType.INT32,
                    "i64", BuiltInType.INT64,
                    "u32", BuiltInType.UINT32,
                    "u64", BuiltInType.UINT64
            );

            if (suffix != null) {
                hasTypeSuffix = true;
                valueString = matcher.group(1);
                type = stringTypesToBuiltInTypes.get(suffix);
            }

            try {
                long value = Long.parseLong(valueString);
                return new syspro.tm.lexer.IntegerLiteralToken(start, end, 0, 0, type,
                        hasTypeSuffix, value);
            } catch (NumberFormatException e) {
                return new BadToken(start, end, 0, 0);
            }

        }

        if (isRuneLiteral(word)) {
            int unicodeCodePoint = word.codePointAt(0);
            return new syspro.tm.lexer.RuneLiteralToken(start, end, 0, 0,
                    unicodeCodePoint);
        }

        if (isStringLiteral(word)) {
            return new syspro.tm.lexer.StringLiteralToken(start, end, 0, 0, word);
        }

        if (isIdentifier(word)) {
            Keyword keyword = null;
            if (isKeyword(word)) {
                keyword = Keyword.valueOf(word.toUpperCase());
            }
            return new IdentifierToken(start, end, 0, 0, word, keyword);
        }

        return new BadToken(start, end, 0, 0);
    }


    public static int getCorrectTokenLength(int end, int border, ArrayList<Integer> codePoints) {
        if (end > border) return 0;
        int length = 0;
        int start = end;
        int firstSymbol = codePoints.get(end);
        if (firstSymbol == '\'' || firstSymbol == '\"') {
            length = 1;
            end++;
            while (end < border && codePoints.get(end) != firstSymbol) {
                length++;
                end++;
            }
        }

        while (end <= border && !(Recognizer.recognize(start, end, codePoints) instanceof BadToken)) {
            length++;
            end++;
            if (end + 2 <= border && Recognizer.recognize(start, end, codePoints) instanceof BadToken &&
                    !(Recognizer.recognize(start, end + 2, codePoints) instanceof BadToken)) {
                end += 2;
                length += 2;
            }
        }
        return length;
    }

    public static int getBadTokenLength(int pos, int end, ArrayList<Integer> codePoints) {
        int length = 0;
        while (pos <= end && getTriviaLength(pos, end, codePoints) == 0) {
            length++;
            pos++;
        }
        return length;
    }

    public static Indentation countIndentation(int end, int border, int levelIndentation, int lengthIndentation,
                                               ArrayList<Integer> codePoints) {
        end += getNewLineLength(end, border, codePoints);

        int newLineLength = getNewLineLength(end, border, codePoints);

        if (newLineLength > 0) {
            return new Indentation(levelIndentation, lengthIndentation);
        }

        end += newLineLength;

        int nextIndentationLength = getWhitespaceLength(end, border, codePoints);
        if (nextIndentationLength == 0) {
            return new Indentation(0, -1);
        }
        end += nextIndentationLength;

        if (getNewLineLength(end, border, codePoints) > 0) {
            return new Indentation(levelIndentation, lengthIndentation);
        }

        if (end == border + 1) {
            return new Indentation(0, -1);
        } else {
            if (!isCorrectIndentation(end - nextIndentationLength, nextIndentationLength, codePoints)) {
                return new Indentation(levelIndentation, lengthIndentation);
            }
            if (levelIndentation == 0) {
                return new Indentation(1, nextIndentationLength);
            }
            if (nextIndentationLength % lengthIndentation != 0) {
                return new Indentation(levelIndentation, lengthIndentation);
            }
            int N = nextIndentationLength / lengthIndentation;
            return new Indentation(N, lengthIndentation);
        }
    }
}
