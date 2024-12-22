package lexer;

import syspro.tm.lexer.BadToken;
import syspro.tm.lexer.IndentationToken;
import syspro.tm.lexer.Lexer;
import syspro.tm.lexer.Token;

import java.util.ArrayList;
import java.util.List;

import static lexer.Recognizer.*;

public class MyLexer implements Lexer {



    @Override
    public List<Token> lex(String text) {
        List<Token> tokens = new ArrayList<>();
        tokens.add(new BadToken(0, 0, 0, 0));

        ArrayList<Integer> codePoints = new ArrayList<>();
        text.codePoints().forEach(codePoints::add);

        int start = 0, end = 0, currentIndentationLevel = 0, currentIndentationLength = -1;
        int border = codePoints.size() - 1;

        while (true) {
            int triviaStart = start;

            int whitespacesLength = getWhitespaceLength(end, border, codePoints);
            int commentsLength = getCommentLength(end, border, codePoints);
            int newLineLength = getNewLineLength(end, border, codePoints);

            boolean newLineEncountered = newLineLength > 0;
            int firstNewLineStart = newLineEncountered ? end : -1;
            int firstNewLineLength = newLineLength;

            while (whitespacesLength + commentsLength + newLineLength > 0) {
                if (newLineEncountered) {
                    while (newLineLength > 0) {
                        end += newLineLength;
                        newLineLength = getNewLineLength(end, border, codePoints);
                    }
                    break;
                }
                end += whitespacesLength + commentsLength;

                commentsLength = getCommentLength(end, border, codePoints);
                whitespacesLength = getWhitespaceLength(end, border, codePoints);

                newLineLength = getNewLineLength(end, border, codePoints);
                newLineEncountered = newLineLength > 0;
                firstNewLineStart = newLineEncountered ? end : -1;
                firstNewLineLength = newLineLength;
            }

            int triviaEnd = end - 1;
            start = end;

            if (newLineEncountered) {

                updateLastToken(triviaStart, triviaEnd, tokens);
                newLineLength = firstNewLineLength;

                while (firstNewLineStart != -1) {
                    Indentation resultIndentation = countIndentation(firstNewLineStart, border, currentIndentationLevel,
                            currentIndentationLength, codePoints);

                    int indentationLevel = resultIndentation.indentationLevel();
                    int indentationLength = resultIndentation.indentationLength();

                    updateIndentation(firstNewLineStart, newLineLength, currentIndentationLevel, indentationLevel,
                            tokens);



                    currentIndentationLevel = indentationLevel;
                    currentIndentationLength = indentationLength;

                    firstNewLineStart += newLineLength;
                    newLineLength = getNewLineLength(firstNewLineStart, triviaEnd, codePoints);
                    firstNewLineStart = newLineLength > 0 ? firstNewLineStart : -1;
                }

            } else if (triviaEnd + 1 > border) {
                updateLastToken(triviaStart, triviaEnd, tokens);
            } else {
                int tokenStart = triviaEnd + 1;
                int tokenEnd = tokenStart;
                int tokenLength = getCorrectTokenLength(tokenStart, border, codePoints);

                if (tokenLength == 0) {
                    tokenLength = getBadTokenLength(tokenStart, border, codePoints);
                }

                tokenEnd += tokenLength;
                Token token = recognize(tokenStart, tokenEnd - 1, codePoints);

                start = tokenEnd;
                end = tokenEnd;

                token = token.withLeadingTriviaLength(triviaEnd - triviaStart + 1).withStart(triviaStart);
                tokens.add(token);
            }
            if (end == border + 1) {
                updateIndentation(border + 1, 1, currentIndentationLevel, 0,
                        tokens);
                break;
            }

        }

        Token fakeToken = tokens.getFirst();


        Token firstRealToken = null;
        int index = -1;
        for (int i = 1; i < tokens.size(); i++) {
            if (!(tokens.get(i) instanceof IndentationToken)) {
                firstRealToken = tokens.get(i);
                index = i;
                break;
            }
        }
        if (firstRealToken == null) return tokens;


        firstRealToken = firstRealToken.withLeadingTriviaLength(fakeToken.trailingTriviaLength +
                firstRealToken.leadingTriviaLength);
        firstRealToken = firstRealToken.withStart(0);
        tokens.set(index, firstRealToken);
        return tokens.subList(1, tokens.size());
    }

    public void updateLastToken(int triviaStart, int triviaEnd, List<Token> tokens) {
        Token lastRealToken = null;
        int index = -1;

        for (int i = tokens.size() - 1; i >= 0; i--) {
            if (!(tokens.get(i) instanceof IndentationToken)) {
                lastRealToken = tokens.get(i);
                index = i;
                break;
            }
        }
        if (lastRealToken == null) return;


        lastRealToken = lastRealToken.withEnd(triviaEnd).withTrailingTriviaLength(lastRealToken.trailingTriviaLength
                + triviaEnd - triviaStart + 1);
        tokens.set(index, lastRealToken);
    }

    public void updateIndentation(int index, int newLineLength, int oldLevelIndentation, int newLevelIndentation,
                                  List<Token> tokens) {
        if (oldLevelIndentation == newLevelIndentation) return;
        int diff = newLevelIndentation - oldLevelIndentation;
        int dx = diff > 0 ? 1 : -1;

        for (int i = 0; i != diff; i += dx) {
            tokens.add(new IndentationToken(index, index + newLineLength - 1, 0,
                    0, dx));
        }
    }
}