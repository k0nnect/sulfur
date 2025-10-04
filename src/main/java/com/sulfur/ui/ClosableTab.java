package com.sulfur.ui;

import javax.swing.*;
import java.awt.*;

public class ClosableTab extends JPanel {
    public ClosableTab(JTabbedPane pane, String title) {
        super(new FlowLayout(FlowLayout.LEFT, 0, 0));
        setOpaque(false);
        JLabel label = new JLabel(title);
        add(label);
        JButton close = new JButton("×");
        close.setBorder(null);
        close.setOpaque(false);
        close.setContentAreaFilled(false);
        close.addActionListener(e -> {
            int i = pane.indexOfTabComponent(this);
            if (i != -1) pane.remove(i);
        });
        add(Box.createHorizontalStrut(8));
        add(close);
    }
}