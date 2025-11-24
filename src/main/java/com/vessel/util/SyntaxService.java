package com.vessel.util;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SyntaxService {
    private static final String[] JAVA_KEYWORDS = new String[] {
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native", "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while"
    };
    private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", JAVA_KEYWORDS) + ")\\b";
    private static final String PAREN_PATTERN = "\\(|\\)";
    private static final String BRACE_PATTERN = "\\{|\\}";
    private static final String BRACKET_PATTERN = "\\[|\\]";
    private static final String SEMICOLON_PATTERN = ";";
    private static final String STRING_PATTERN = "\"([^\\\"\\\\]|\\\\.)*\"";
    private static final String CHAR_PATTERN   = "'(?:[^'\\\\]|\\\\.)*'";
    private static final String COMMENT_PATTERN = "//[^\\n]*|/\\*.*?\\*/";
    private static final String NUMBER_PATTERN = "\\b[0-9]+(\\.[0-9]+)?([eE][+-]?[0-9]+)?[fFdD]?\\b";

    private static final Pattern PATTERN = Pattern.compile(
            "(?<KEYWORD>" + KEYWORD_PATTERN + ")"
                    + "|(?<PAREN>" + PAREN_PATTERN + ")"
                    + "|(?<BRACE>" + BRACE_PATTERN + ")"
                    + "|(?<BRACKET>" + BRACKET_PATTERN + ")"
                    + "|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")"
                    + "|(?<STRING>" + STRING_PATTERN + ")"
                    + "|(?<CHAR>" + CHAR_PATTERN + ")"
                    + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
                    + "|(?<NUMBER>" + NUMBER_PATTERN + ")",
            Pattern.DOTALL
    );

    public static StyleSpans<Collection<String>> computeHighlighting(String text) {
        if (text.isEmpty()) {
            StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
            spansBuilder.add(Collections.singleton("plain"), 0);
            return spansBuilder.create();
        }
        Matcher matcher = PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        while (matcher.find()) {
            String styleClass =
                    matcher.group("KEYWORD") != null ? "keyword" :
                            matcher.group("PAREN")    != null ? "paren"    :
                                    matcher.group("BRACE")    != null ? "brace"    :
                                            matcher.group("BRACKET")  != null ? "bracket"  :
                                                    matcher.group("SEMICOLON")!= null ? "semicolon":
                                                            matcher.group("STRING")   != null ? "string"   :
                                                                    matcher.group("CHAR")     != null ? "char"     :
                                                                            matcher.group("COMMENT")  != null ? "comment"  :
                                                                                    matcher.group("NUMBER")   != null ? "number"   :
                                                                                            "plain"; // For any matched but uncategorized case (very rare)
            // Add unstyled/plain segment before this match
            if (matcher.start() > lastKwEnd) {
                spansBuilder.add(Collections.singleton("plain"), matcher.start() - lastKwEnd);
            }
            // Add the highlighted segment
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        // Add any remaining plain text at the end
        if (lastKwEnd < text.length()) {
            spansBuilder.add(Collections.singleton("plain"), text.length() - lastKwEnd);
        }
        return spansBuilder.create();
    }
}
