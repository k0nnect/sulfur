package com.sulfur.ui;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

public class UsageResultsPanel extends JPanel {
    private final DefaultListModel<String> listModel;
    private final JList<String> usageList;

    public UsageResultsPanel() {
        super(new BorderLayout());
        listModel = new DefaultListModel<>();
        usageList = new JList<>(listModel);
        add(new JScrollPane(usageList), BorderLayout.CENTER);
    }

    public void displayUsages(Set<String> usages) {
        listModel.clear();
        if (usages != null && !usages.isEmpty()) {
            for (String usage : usages) {
                listModel.addElement(usage);
            }
        } else {
            listModel.addElement("No usages found.");
        }
    }

    public void clearUsages() {
        listModel.clear();
    }
}
