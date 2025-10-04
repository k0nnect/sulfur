package com.sulfur.ui;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SyntaxHighlighter {

    private static final Pattern KEYWORDS = Pattern.compile(
            "\\b(abstract|assert|boolean|break|byte|case|catch|char|class|const|continue|default|" +
            "do|double|else|enum|extends|final|finally|float|for|goto|if|implements|import|" +
            "instanceof|int|interface|long|native|new|package|private|protected|public|" +
            "return|short|static|strictfp|super|switch|synchronized|this|throw|throws|" +
            "transient|try|void|volatile|while)\\b");

    private static final Pattern STRINGS = Pattern.compile("\"[^\"\\\\]*(\\\\.[^\"\\\\]*)*\"");
    private static final Pattern COMMENTS = Pattern.compile("//.*|/\\*([^*]|\\*(?!/))*\\*/");
    private static final Pattern ANNOTATIONS = Pattern.compile("@\\w+");
    private static final Pattern NUMBERS = Pattern.compile("\\b\\d+(\\.\\d+)?\\b");

    public static void highlightJava(JTextPane textPane) {
        StyledDocument doc = textPane.getStyledDocument();
        String text = "";
        try {
            text = doc.getText(0, doc.getLength());
        } catch (BadLocationException e) {
            return;
        }

        // define styles
        Style defaultStyle = textPane.getStyle(StyleContext.DEFAULT_STYLE);
        Style keywordStyle = textPane.addStyle("keyword", defaultStyle);
        StyleConstants.setForeground(keywordStyle, new Color(127, 0, 85));
        StyleConstants.setBold(keywordStyle, true);

        Style stringStyle = textPane.addStyle("string", defaultStyle);
        StyleConstants.setForeground(stringStyle, new Color(0, 128, 0));

        Style commentStyle = textPane.addStyle("comment", defaultStyle);
        StyleConstants.setForeground(commentStyle, new Color(128, 128, 128));
        StyleConstants.setItalic(commentStyle, true);

        Style annotationStyle = textPane.addStyle("annotation", defaultStyle);
        StyleConstants.setForeground(annotationStyle, new Color(0, 0, 128));

        Style numberStyle = textPane.addStyle("number", defaultStyle);
        StyleConstants.setForeground(numberStyle, new Color(0, 0, 255));

        // apply default style to all text first
        StyleConstants.setForeground(defaultStyle, UIManager.getColor("TextPane.foreground"));
        doc.setCharacterAttributes(0, text.length(), defaultStyle, true);

        // then apply syntax highlighting
        highlight(doc, KEYWORDS, keywordStyle, text);
        highlight(doc, STRINGS, stringStyle, text);
        highlight(doc, COMMENTS, commentStyle, text);
        highlight(doc, ANNOTATIONS, annotationStyle, text);
        highlight(doc, NUMBERS, numberStyle, text);
    }

    private static void highlight(StyledDocument doc, Pattern pattern, Style style, String text) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            doc.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(), style, false);
        }
    }
}