package com.sulfur.ui;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.UIManager;

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

        Style defaultStyle = doc.addStyle("defaultStyle", null);
        StyleConstants.setForeground(defaultStyle, UIManager.getColor("TextPane.foreground"));

        Style keywordStyle = doc.addStyle("keywordStyle", defaultStyle);
        StyleConstants.setForeground(keywordStyle, UIManager.getColor("Actions.Red")); 
        StyleConstants.setBold(keywordStyle, true);

        Style stringStyle = doc.addStyle("stringStyle", defaultStyle);
        StyleConstants.setForeground(stringStyle, UIManager.getColor("Component.accentColor")); 

        Style commentStyle = doc.addStyle("commentStyle", defaultStyle);
        StyleConstants.setForeground(commentStyle, UIManager.getColor("Label.disabledForeground")); 

        Style numberStyle = doc.addStyle("numberStyle", defaultStyle);
        StyleConstants.setForeground(numberStyle, UIManager.getColor("Editor.foreground")); 

        Style typeStyle = doc.addStyle("typeStyle", defaultStyle);
        StyleConstants.setForeground(typeStyle, UIManager.getColor("Editor.background")); 

        Style annotationStyle = doc.addStyle("annotationStyle", defaultStyle);
        StyleConstants.setForeground(annotationStyle, UIManager.getColor("ComboBox.buttonBackground")); 

        try {
            String text = doc.getText(0, doc.getLength());

            // apply default style to all text first
            doc.setCharacterAttributes(0, text.length(), defaultStyle, true);

            // then apply syntax highlighting
            highlight(doc, KEYWORDS, keywordStyle, text);
            highlight(doc, STRINGS, stringStyle, text);
            highlight(doc, COMMENTS, commentStyle, text);
            highlight(doc, ANNOTATIONS, annotationStyle, text);
            highlight(doc, NUMBERS, numberStyle, text);
        } catch (BadLocationException e) {
            return;
        }
    }

    private static void highlight(StyledDocument doc, Pattern pattern, Style style, String text) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            doc.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(), style, false);
        }
    }
}