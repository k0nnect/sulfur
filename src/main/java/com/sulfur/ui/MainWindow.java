package com.sulfur.ui;

import com.sulfur.core.DecompilerService;
import com.sulfur.core.Disassembler;
import com.sulfur.core.JarIndex;
import com.sulfur.core.UsageAnalyzer;
import com.sulfur.util.SwingUtil;
import com.sulfur.config.AppSettings;
import com.sulfur.ui.theme.ThemeManager;
import com.sulfur.ui.theme.ThemeManager.Theme;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.Desktop;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainWindow {
    private JFrame frame;
    private JTree classTree;
    private JTextPane outputArea;
    private JTextArea statusBar;
    private JTextField searchField;
    private JTabbedPane tabs;
    private JCheckBoxMenuItem useCfrMenuItem;
    private JRadioButtonMenuItem lightThemeMenuItem;
    private JRadioButtonMenuItem darkThemeMenuItem;
    private JCheckBoxMenuItem zkmDeobfuscationMenuItem;
    private JCheckBoxMenuItem allatoriDeobfuscationMenuItem;
    private JCheckBoxMenuItem discordRpcMenuItem;

    private JPopupMenu outputAreaPopupMenu;

    private UsageAnalyzer usageAnalyzer;
    private UsageResultsPanel usageResultsPanel;

    private JarIndex index;
    private File currentJar;
    private String currentClass;
    private AppSettings settings;
    private com.sulfur.rpc.DiscordService discordService;

    public static void launch() {
        SwingUtilities.invokeLater(() -> new MainWindow().init());
    }

    private void onOpenJar() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".jar");
            }

            @Override
            public String getDescription() {
                return "JAR Files (*.jar)";
            }
        });

        int result = chooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                currentJar = chooser.getSelectedFile();
            
            if (currentJar == null || !currentJar.exists()) {
                JOptionPane.showMessageDialog(frame, "[!] Invalid .jar file",
                        "[!] Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
                
SwingWorker<JarIndex, Void> worker = new SwingWorker<>() {
    @Override
    protected JarIndex doInBackground() throws Exception {
        if (currentJar == null) {
            throw new IllegalArgumentException(".jar file is null");
        }

        if (!currentJar.exists() || !currentJar.isFile() || !currentJar.getName().endsWith(".jar")) {
            throw new IllegalArgumentException("Not a valid .jar file: " + currentJar.getAbsolutePath());
        }
        
        try {
            return JarIndex.fromJar(currentJar.toPath());
        } catch (IOException e) {
            throw new IOException("[!] Failed to process .jar file: " + e.getMessage(), e);
        }
    }

    @Override
    protected void done() {
        try {
            index = get();
            updateClassTree();
            statusBar.setText("[!] Loaded: " + currentJar.getName());

            if (settings.isDiscordRichPresence()) {
                discordService.setCurrentJar(currentJar.getName());
            }
            // initialize usageAnalyzer after index is set
            usageAnalyzer = new UsageAnalyzer(index);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "[!] Error opening .jar file: " + ex.getMessage(),
                    "[!] Error", JOptionPane.ERROR_MESSAGE);
        }
    }
};
worker.execute();
                
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "[!] Error opening .jar file: " + ex.getMessage(),
                        "[!] Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void updateClassTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(currentJar.getName());
        Map<String, DefaultMutableTreeNode> packageNodes = new HashMap<>();

        for (String className : index.classNames()) {
            String[] parts = className.split("\\.");
            StringBuilder packagePath = new StringBuilder();
            DefaultMutableTreeNode parentNode = root;

            for (int i = 0; i < parts.length - 1; i++) {
                if (i > 0) packagePath.append(".");
                packagePath.append(parts[i]);
                String pkg = packagePath.toString();

                if (!packageNodes.containsKey(pkg)) {
                    DefaultMutableTreeNode node = new DefaultMutableTreeNode(parts[i]);
                    packageNodes.put(pkg, node);
                    parentNode.add(node);
                }
                parentNode = packageNodes.get(pkg);
            }

            DefaultMutableTreeNode classNode = new DefaultMutableTreeNode(parts[parts.length - 1]);
            classNode.setUserObject(className);
            parentNode.add(classNode);
        }

        classTree.setModel(new DefaultTreeModel(root));
        SwingUtil.expandAll(classTree);
    }

    private void decompileSelectedClass() {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) classTree.getLastSelectedPathComponent();
        if (node == null) return;

        Object nodeInfo = node.getUserObject();
        if (node.isLeaf() && nodeInfo instanceof String) {
            String className = (String) nodeInfo;
            currentClass = className;
            
            try {
                String decompiled;
                if (settings.isUseCfrDecompiler()) {
                    decompiled = DecompilerService.decompileWithCFR(index, className, settings);
                } else {
                    decompiled = DecompilerService.decompile(index, className, settings);
                }
    
                outputArea.setText(decompiled);
                SyntaxHighlighter.highlightJava(outputArea);
    
                JTextArea bytecodeArea = (JTextArea) ((JScrollPane) tabs.getComponentAt(1)).getViewport().getView();
                bytecodeArea.setText(Disassembler.disassemble(index, className));
                
                String statusMessage = "[!] Decompiled: " + className;
                if (settings.isZkmDeobfuscation() || settings.isAllatoriDeobfuscation()) {
                    statusMessage += " (with";
                    if (settings.isZkmDeobfuscation()) {
                        statusMessage += " ZKM";
                    }
                    if (settings.isZkmDeobfuscation() && settings.isAllatoriDeobfuscation()) {
                        statusMessage += " &";
                    }
                    if (settings.isAllatoriDeobfuscation()) {
                        statusMessage += " Allatori";
                    }
                    statusMessage += " deobfuscation)";
                }
                statusBar.setText(statusMessage);
                
                if (settings.isDiscordRichPresence()) {
                    discordService.setCurrentClass(className);
                    discordService.setDecompilerBackend(settings.isUseCfrDecompiler() ? "CFR" : "Procyon");
                }
            } catch (Exception ex) {
                outputArea.setText("[!] Failed to decompile: " + ex.getMessage());
                statusBar.setText("[!] Error decompiling: " + className);
            }
        }
    }
    
    private void toggleDiscordRpc(boolean enabled) {
        settings.setDiscordRichPresence(enabled);
        settings.saveSettings();
        
        if (enabled) {
            discordService.enable();
            
            if (currentJar != null) {
                discordService.setCurrentJar(currentJar.getName());
            }
            if (currentClass != null && !currentClass.isEmpty()) {
                discordService.setCurrentClass(currentClass);
            }
            discordService.setDecompilerBackend(settings.isUseCfrDecompiler() ? "CFR" : "Procyon");
            
            statusBar.setText("[!] Discord Rich Presence enabled");
        } else {
            discordService.disable();
            statusBar.setText("[!] Discord Rich Presence disabled");
        }
    }

    private void saveDecompiledSource() {
        if (currentClass == null || currentClass.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "[!] No class selected to save", "[!] Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(currentClass.substring(currentClass.lastIndexOf('.') + 1) + ".java"));
        chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".java");
            }

            @Override
            public String getDescription() {
                return "Java Source Files (*.java)";
            }
        });

        int result = chooser.showSaveDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            try (FileWriter writer = new FileWriter(chooser.getSelectedFile())) {
                writer.write(outputArea.getText());
                statusBar.setText("[!] Saved: " + chooser.getSelectedFile().getName());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, 
                    "[!] Error saving file: " + ex.getMessage(), 
                    "[!] Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void toggleDecompilerBackend() {
        settings.setUseCfrDecompiler(useCfrMenuItem.isSelected());
        settings.saveSettings();
        
        if (settings.isDiscordRichPresence()) {
            discordService.setDecompilerBackend(settings.isUseCfrDecompiler() ? "CFR" : "Procyon");
        }
        
        if (currentClass != null && !currentClass.isEmpty()) {
            decompileSelectedClass();
        }
    }
    
    private void toggleZkmDeobfuscation(boolean enabled) {
        settings.setZkmDeobfuscation(enabled);
        settings.saveSettings();
        
        if (currentClass != null && !currentClass.isEmpty()) {
            decompileSelectedClass();
        }
        
        String status = enabled ? "enabled" : "disabled";
        statusBar.setText("[!] ZKM string deobfuscation " + status);
    }
            
            private void toggleAllatoriDeobfuscation(boolean enabled) {
        settings.setAllatoriDeobfuscation(enabled);
        settings.saveSettings();
        
        if (currentClass != null && !currentClass.isEmpty()) {
            decompileSelectedClass();
        }
        
        String status = enabled ? "enabled" : "disabled";
        statusBar.setText("[!] Sulfur - Allatori string deobfuscation " + status);
            }

    private void init() {
        settings = AppSettings.loadSettings();
        discordService = com.sulfur.rpc.DiscordService.getInstance();
        
        if (settings.isDiscordRichPresence()) {
            discordService.initialize();
            discordService.enable();
        }
        
        frame = new JFrame("Sulfur");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 800);

        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("/gengar.png"));
            frame.setIconImage(icon.getImage());
        } catch (Exception e) {
            System.err.println("[!] Failed to load application icon: " + e.getMessage());
        }
    
        ThemeManager.applyTheme(settings.getTheme());

        // usageAnalyzer is now initialized in onOpenJar() after index is loaded

        var menuBar = new JMenuBar();

        var fileMenu = new JMenu("File");
        fileMenu.add(new AbstractAction("Open new .jar file") {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOpenJar();
            }
        });
        
        fileMenu.add(new AbstractAction("Save source as .java file") {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveDecompiledSource();
            }
        });
        
        fileMenu.addSeparator();
        fileMenu.add(new AbstractAction("Exit") {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (settings.isDiscordRichPresence() && discordService != null) {
                    discordService.shutdown();
                }
                frame.dispose();
            }
        });
        menuBar.add(fileMenu);

        var editMenu = new JMenu("Edit");
        editMenu.add(new AbstractAction("Find & Replace") {
            @Override
            public void actionPerformed(ActionEvent e) {
                showFindReplaceDialog();
            }
        });
        menuBar.add(editMenu);
        
        var settingsMenu = new JMenu("Settings");
        useCfrMenuItem = new JCheckBoxMenuItem("Use CFR 0.152 decompiler", settings.isUseCfrDecompiler());
        useCfrMenuItem.addActionListener(e -> toggleDecompilerBackend());
        settingsMenu.add(useCfrMenuItem);

        settingsMenu.addSeparator();

        var themeMenu = new JMenu("Theme");
        ButtonGroup themeGroup = new ButtonGroup();

        lightThemeMenuItem = new JRadioButtonMenuItem("Light Theme", settings.getTheme() == Theme.LIGHT);
        lightThemeMenuItem.addActionListener(e -> {
            settings.setTheme(Theme.LIGHT);
            settings.saveSettings();
            ThemeManager.applyTheme(Theme.LIGHT);
        });
        themeGroup.add(lightThemeMenuItem);
        themeMenu.add(lightThemeMenuItem);

        darkThemeMenuItem = new JRadioButtonMenuItem("Dark Theme", settings.getTheme() == Theme.DARK);
        darkThemeMenuItem.addActionListener(e -> {
            settings.setTheme(Theme.DARK);
            settings.saveSettings();
            ThemeManager.applyTheme(Theme.DARK);
        });
        themeGroup.add(darkThemeMenuItem);
        themeMenu.add(darkThemeMenuItem);
        settingsMenu.add(themeMenu);

        settingsMenu.addSeparator();

        zkmDeobfuscationMenuItem = new JCheckBoxMenuItem("ZKM String Deobfuscation",
                settings.isZkmDeobfuscation());
        zkmDeobfuscationMenuItem.addActionListener(e -> toggleZkmDeobfuscation(zkmDeobfuscationMenuItem.isSelected()));
        settingsMenu.add(zkmDeobfuscationMenuItem);

        allatoriDeobfuscationMenuItem = new JCheckBoxMenuItem("Allatori String Deobfuscation",
                settings.isAllatoriDeobfuscation());
        allatoriDeobfuscationMenuItem.addActionListener(e ->
                toggleAllatoriDeobfuscation(allatoriDeobfuscationMenuItem.isSelected()));
        settingsMenu.add(allatoriDeobfuscationMenuItem);

        settingsMenu.addSeparator();

        discordRpcMenuItem = new JCheckBoxMenuItem("Discord Rich Presence",
                settings.isDiscordRichPresence());
        discordRpcMenuItem.addActionListener(e ->
                toggleDiscordRpc(discordRpcMenuItem.isSelected()));
        settingsMenu.add(discordRpcMenuItem);

        var helpMenu = new JMenu("Help");
        helpMenu.add(new AbstractAction("Credits") {
            @Override
            public void actionPerformed(ActionEvent e) {
                showCredits();
            }
        });
        
        menuBar.add(settingsMenu);
        menuBar.add(helpMenu);
        frame.setJMenuBar(menuBar);

        var left = new JPanel(new BorderLayout());
        searchField = new JTextField();
        left.add(searchField, BorderLayout.NORTH);
        classTree = new JTree(new DefaultMutableTreeNode("[!] No .jar file loaded!"));
        classTree.addTreeSelectionListener(e -> decompileSelectedClass());
        left.add(new JScrollPane(classTree), BorderLayout.CENTER);
        left.setPreferredSize(new Dimension(340, 800));

        tabs = new JTabbedPane();
        outputArea = new JTextPane();
        outputArea.setEditable(false);
        outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        tabs.addTab("Decompiled", new JScrollPane(outputArea));

        outputAreaPopupMenu = new JPopupMenu();
        JMenuItem findUsagesMenuItem = new JMenuItem("Find Usages");
        findUsagesMenuItem.addActionListener(e -> findUsagesInOutputArea());
        outputAreaPopupMenu.add(findUsagesMenuItem);

        outputArea.setComponentPopupMenu(outputAreaPopupMenu);

        var bytecodeArea = SwingUtil.monoArea();
        tabs.addTab("Bytecode", new JScrollPane(bytecodeArea));

        usageResultsPanel = new UsageResultsPanel();
        tabs.addTab("Usages", usageResultsPanel);

        statusBar = new JTextArea(1, 0);
        statusBar.setEditable(false);

        var splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, tabs);

        frame.add(splitPane, BorderLayout.CENTER);
        frame.add(statusBar, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private void findUsagesInOutputArea() {
        String selectedText = outputArea.getSelectedText();
        if (selectedText == null || selectedText.trim().isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please select text to find usages.", "Find Usages", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (index == null) {
            JOptionPane.showMessageDialog(frame, "No JAR file loaded.", "Find Usages", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Set<String> usages = usageAnalyzer.findUsages(selectedText);

        usageResultsPanel.displayUsages(usages);
        tabs.setSelectedComponent(usageResultsPanel);
    }

    private void showFindReplaceDialog() {
                JDialog dialog = new JDialog(frame, "Sulfur - Find & Replace", true);
                dialog.setLayout(new BorderLayout());
                dialog.setSize(500, 250);
                dialog.setLocationRelativeTo(frame);
                dialog.setResizable(true);
                
                JPanel searchPanel = new JPanel(new GridBagLayout());
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(5, 5, 5, 5);
                gbc.fill = GridBagConstraints.HORIZONTAL;
                
                gbc.gridx = 0;
                gbc.gridy = 0;
                gbc.weightx = 0;
                searchPanel.add(new JLabel("Find:"), gbc);
                
                JTextField findField = new JTextField(20);
                gbc.gridx = 1;
                gbc.weightx = 1;
                searchPanel.add(findField, gbc);
                
                gbc.gridx = 0;
                gbc.gridy = 1;
                gbc.weightx = 0;
                searchPanel.add(new JLabel("Replace with:"), gbc);
                
                JTextField replaceField = new JTextField(20);
                gbc.gridx = 1;
                gbc.weightx = 1;
                searchPanel.add(replaceField, gbc);
                
                JPanel optionsPanel = new JPanel();
                JCheckBox matchCaseCheckBox = new JCheckBox("Match case");
                JCheckBox wholeWordCheckBox = new JCheckBox("Whole word");
                JCheckBox regexCheckBox = new JCheckBox("Use regex");
                
                optionsPanel.add(matchCaseCheckBox);
                optionsPanel.add(wholeWordCheckBox);
                optionsPanel.add(regexCheckBox);
                
                gbc.gridx = 0;
                gbc.gridy = 2;
                gbc.gridwidth = 2;
                searchPanel.add(optionsPanel, gbc);
                
                JPanel buttonPanel = new JPanel();
                JButton findButton = new JButton("Find");
                JButton replaceButton = new JButton("Replace");
                JButton replaceAllButton = new JButton("Replace All");
                JButton closeButton = new JButton("Close");
                
                buttonPanel.add(findButton);
                buttonPanel.add(replaceButton);
                buttonPanel.add(replaceAllButton);
                buttonPanel.add(closeButton);
                
                JLabel statusLabel = new JLabel(" ");
                statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

                dialog.add(searchPanel, BorderLayout.NORTH);
                dialog.add(statusLabel, BorderLayout.CENTER);
                dialog.add(buttonPanel, BorderLayout.SOUTH);
                
                findField.requestFocus();
                
                closeButton.addActionListener(e -> dialog.dispose());
                
                findButton.addActionListener(e -> {
                    if (findField.getText().isEmpty()) {
                        statusLabel.setText("[!] Please enter a search term");
                        return;
                    }
                    
                    String text = outputArea.getText();
                    String searchTerm = findField.getText();
                    boolean matchCase = matchCaseCheckBox.isSelected();
                    boolean wholeWord = wholeWordCheckBox.isSelected();
                    boolean useRegex = regexCheckBox.isSelected();
                    
                    int[] result = findInText(text, searchTerm, matchCase, wholeWord, useRegex);
                    if (result != null) {
                        outputArea.setCaretPosition(result[0]);
                        outputArea.select(result[0], result[1]);
                        outputArea.requestFocus();
                        statusLabel.setText("[!] sulfur: Found match at position " + result[0]);
                    } else {
                        statusLabel.setText("[!] sulfur: No matches found");
                    }
                });
                
                replaceButton.addActionListener(e -> {
                    if (findField.getText().isEmpty()) {
                        statusLabel.setText("[!] sulfur: Please enter a search term");
                        return;
                    }
                    
                    String text = outputArea.getText();
                    String searchTerm = findField.getText();
                    String replacement = replaceField.getText();
                    boolean matchCase = matchCaseCheckBox.isSelected();
                    boolean wholeWord = wholeWordCheckBox.isSelected();
                    boolean useRegex = regexCheckBox.isSelected();
                    
                    int[] result = findInText(text, searchTerm, matchCase, wholeWord, useRegex);
                    if (result != null) {
                        try {
                            outputArea.getDocument().remove(result[0], result[1] - result[0]);
                        } catch (BadLocationException ex) {
                            throw new RuntimeException(ex);
                        }
                        try {
                            outputArea.getDocument().insertString(result[0], replacement, null);
                        } catch (BadLocationException ex) {
                            throw new RuntimeException(ex);
                        }
                        statusLabel.setText("[!] sulfur: Replaced 1 occurrence");
                    } else {
                        statusLabel.setText("[!] sulfur: No matches found");
                    }
                });
                
                replaceAllButton.addActionListener(e -> {
                    if (findField.getText().isEmpty()) {
                        statusLabel.setText("[!] sulfur: Please enter a search term");
                        return;
                    }
                    
                    String text = outputArea.getText();
                    String searchTerm = findField.getText();
                    String replacement = replaceField.getText();
                    boolean matchCase = matchCaseCheckBox.isSelected();
                    boolean wholeWord = wholeWordCheckBox.isSelected();
                    boolean useRegex = regexCheckBox.isSelected();
                    
                    int count = replaceAllInText(text, searchTerm, replacement, matchCase, wholeWord, useRegex);
                    statusLabel.setText("[!] sulfur: Replaced " + count + " occurrences");
                });
                
                dialog.setVisible(true);
            }
            
            private int[] findInText(String text, String searchTerm, boolean matchCase, boolean wholeWord, boolean useRegex) {
                if (!matchCase) {
                    text = text.toLowerCase();
                    searchTerm = searchTerm.toLowerCase();
                }
                
                if (useRegex) {
                    Pattern pattern;
                    try {
                        if (wholeWord) {
                            pattern = Pattern.compile("\\b" + searchTerm + "\\b", 
                                    matchCase ? 0 : Pattern.CASE_INSENSITIVE);
                        } else {
                            pattern = Pattern.compile(searchTerm, matchCase ? 0 : Pattern.CASE_INSENSITIVE);
                        }
                        Matcher matcher = pattern.matcher(text);
                        if (matcher.find()) {
                            return new int[] { matcher.start(), matcher.end() };
                        }
                    } catch (Exception e) {
                        return null;
                    }
                } else {
                    if (wholeWord) {
                        searchTerm = "\\b" + Pattern.quote(searchTerm) + "\\b";
                        Pattern pattern = Pattern.compile(searchTerm, matchCase ? 0 : Pattern.CASE_INSENSITIVE);
                        Matcher matcher = pattern.matcher(text);
                        if (matcher.find()) {
                            return new int[] { matcher.start(), matcher.end() };
                        }
                    } else {
                        int index = text.indexOf(searchTerm);
                        if (index >= 0) {
                            return new int[] { index, index + searchTerm.length() };
                        }
                    }
                }
                
                return null;
            }
            
            private int replaceAllInText(String text, String searchTerm, String replacement, 
                                       boolean matchCase, boolean wholeWord, boolean useRegex) {
                String original = outputArea.getText();
                String result;
                int count = 0;
                
                if (useRegex) {
                    try {
                        Pattern pattern;
                        if (wholeWord) {
                            pattern = Pattern.compile("\\b" + searchTerm + "\\b", 
                                    matchCase ? 0 : Pattern.CASE_INSENSITIVE);
                        } else {
                            pattern = Pattern.compile(searchTerm, matchCase ? 0 : Pattern.CASE_INSENSITIVE);
                        }
                        
                        Matcher matcher = pattern.matcher(original);
                        result = matcher.replaceAll(replacement);
                        count = (int) matcher.results().count();
                    } catch (Exception e) {
                        return 0;
                    }
                } else {
                    if (wholeWord) {
                        Pattern pattern = Pattern.compile("\\b" + Pattern.quote(searchTerm) + "\\b", 
                                matchCase ? 0 : Pattern.CASE_INSENSITIVE);
                        Matcher matcher = pattern.matcher(original);
                        StringBuffer sb = new StringBuffer();
                        while (matcher.find()) {
                            matcher.appendReplacement(sb, replacement);
                            count++;
                        }
                        matcher.appendTail(sb);
                        result = sb.toString();
                    } else {
                        if (!matchCase) {
                            StringBuffer sb = new StringBuffer();
                            String lowerText = original.toLowerCase();
                            String lowerSearchTerm = searchTerm.toLowerCase();
                            int lastIndex = 0;
                            int index;
                            
                            while ((index = lowerText.indexOf(lowerSearchTerm, lastIndex)) != -1) {
                                sb.append(original, lastIndex, index);
                                sb.append(replacement);
                                lastIndex = index + searchTerm.length();
                                count++;
                            }
                            
                            sb.append(original.substring(lastIndex));
                            result = sb.toString();
                        } else {
                            result = original.replace(searchTerm, replacement);
                            count = (original.length() - result.length()) / (searchTerm.length() - replacement.length());
                            if (searchTerm.length() == replacement.length()) {
                                int fromIndex = 0;
                                count = 0;
                                while ((fromIndex = original.indexOf(searchTerm, fromIndex)) >= 0) {
                                    count++;
                                    fromIndex += searchTerm.length();
                                }
                            }
                        }
                    }
                }
                
                outputArea.setText(result);
                return count;
            }
            
            private void showCredits() {
        JDialog dialog = new JDialog(frame, "Credits", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(500, 300);
        dialog.setLocationRelativeTo(frame);

        JEditorPane creditsPane = new JEditorPane();
        creditsPane.setContentType("text/html");
        creditsPane.setEditable(false);
        creditsPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        
        String creditsText = "<html><body style='text-align: center; font-family: Arial; margin: 20px;'>" +
                "<h2>The Sulfur Project</h2>" +
                "<p>Developed by k0nnect</p>" +
                "<p>Visit the project repository on GitHub!:</p>" +
                "<p><a href='https://github.com/k0nnect/sulfur'>https://github.com/k0nnect/sulfur</a></p>" +
                "</body></html>";
        creditsPane.setText(creditsText);

        creditsPane.addHyperlinkListener(e -> {
            if (e.getEventType() == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    Desktop.getDesktop().browse(e.getURL().toURI());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dialog, 
                        "[!] Error opening the URL: " + ex.getMessage(),
                        "[!] Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dialog.dispose());
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(closeButton);

        dialog.add(new JScrollPane(creditsPane), BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
            }
}