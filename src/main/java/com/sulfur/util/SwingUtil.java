package com.sulfur.util;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;

public class SwingUtil {
    public static JTextArea monoArea() {
        var area = new JTextArea();
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        area.setTabSize(4);
        return area;
    }

    public static void expandAll(JTree tree) {
        for (int i = 0; i < tree.getRowCount(); i++) tree.expandRow(i);
    }
}