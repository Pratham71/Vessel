package com.vessel.util;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SyntaxService {
    public static StyleSpans<Collection<String>> computeJavaHighlighting(String text) {
        final String[] JAVA_KEYWORDS = new String[]{
                "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native", "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while"
        };
        final String KEYWORD_PATTERN = "\\b(" + String.join("|", JAVA_KEYWORDS) + ")\\b";
        final String PAREN_PATTERN = "\\(|\\)";
        final String BRACE_PATTERN = "\\{|\\}";
        final String BRACKET_PATTERN = "\\[|\\]";
        final String SEMICOLON_PATTERN = ";";
        final String STRING_PATTERN = "\"([^\\\"\\\\]|\\\\.)*\"";
        final String CHAR_PATTERN = "'(?:[^'\\\\]|\\\\.)*'";
        final String COMMENT_PATTERN = "//[^\\n]*|/\\*.*?\\*/";
        final String NUMBER_PATTERN = "\\b[0-9]+(\\.[0-9]+)?([eE][+-]?[0-9]+)?[fFdD]?\\b";

        final Pattern PATTERN = Pattern.compile(
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

    public static StyleSpans<Collection<String>> computeMarkdownHighlighting(String text) {
        StyleSpansBuilder<Collection<String>> spans = new StyleSpansBuilder<>();

        if (text == null || text.isEmpty()) {
            spans.add(Collections.singleton("plain"), 0);
            return spans.create();
        }

        Pattern pattern = Pattern.compile(
                "(?<HEADING>^#{1,6}.*$)"
                        + "|(?<BOLD>\\*\\*[^*]+\\*\\*)"
                        + "|(?<ITALIC>\\*[^*]+\\*)"
                        + "|(?<STRIKE>~~[^~]+~~)"
                        + "|(?<CODE>(?<!`)`([^`]|``)+`(?!`))"
                        + "|(?<CODEBLOCK>``````)"
                        + "|(?<LINK>\\[[^]]+\\]\\([^)]*\\))"
                        + "|(?<ULIST>^[ \\t]*[-+*][ \\t].*$)"
                        + "|(?<OLIST>^[ \\t]*\\d+\\.[ \\t].*$)"
                        + "|(?<QUOTE>^[ \\t]*>.*$)",
                Pattern.MULTILINE
        );

        Matcher m = pattern.matcher(text);
        int last = 0;

        while (m.find()) {
            if (m.start() > last) {
                spans.add(Collections.singleton("plain"), m.start() - last);
            }

            String style =
                    m.group("HEADING") != null ? "md-heading" :
                            m.group("BOLD") != null ? "md-bold" :
                                    m.group("ITALIC") != null ? "md-italic" :
                                            m.group("STRIKE") != null ? "md-strike" :
                                                    m.group("CODEBLOCK") != null ? "md-code" :
                                                            m.group("CODE") != null ? "md-code" :
                                                                    m.group("LINK") != null ? "md-link" :
                                                                            m.group("ULIST") != null ? "md-ulist" :
                                                                                    m.group("OLIST") != null ? "md-olist" :
                                                                                            m.group("QUOTE") != null ? "md-quote" :
                                                                                                    "plain";

            spans.add(Collections.singleton(style), m.end() - m.start());
            last = m.end();
        }

        if (last < text.length()) {
            spans.add(Collections.singleton("plain"), text.length() - last);
        }

        return spans.create();
    }
}
