package com.sulfur.ui;

import com.sulfur.core.DecompilerService;
import com.sulfur.core.Disassembler;
import com.sulfur.core.JarIndex;
import com.sulfur.util.SwingUtil;
import com.sulfur.config.AppSettings;
import com.sulfur.ui.theme.ThemeManager;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.StyledDocument;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.StyleContext;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.Desktop;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.*;

public class MainWindow {
    private JFrame frame;
    private JTree classTree;
    private JTextPane outputArea;
    private JTextArea statusBar;
    private JTextField searchField;
    private JTabbedPane tabs;
    private JCheckBoxMenuItem useCfrMenuItem;
    private JCheckBoxMenuItem darkThemeMenuItem;
    private JCheckBoxMenuItem zkmDeobfuscationMenuItem;

    private JarIndex index;
    private File currentJar;
    private String currentClass;
    private AppSettings settings;
    private ThemeManager themeManager;

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
                if (settings.isZkmDeobfuscation()) {
                    statusMessage += " (with ZKM deobfuscation)";
                }
                statusBar.setText(statusMessage);
            } catch (Exception ex) {
                outputArea.setText("[!] Failed to decompile: " + ex.getMessage());
                statusBar.setText("[!] Error decompiling: " + className);
            }
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
        if (currentClass != null && !currentClass.isEmpty()) {
            decompileSelectedClass();
        }
    }
    
    private void toggleDarkTheme() {
        settings.setDarkTheme(darkThemeMenuItem.isSelected());
        settings.saveSettings();
        themeManager.applyTheme(darkThemeMenuItem.isSelected());
    }
    
    private void toggleZkmDeobfuscation(boolean enabled) {
        settings.setZkmDeobfuscation(enabled);
        settings.saveSettings();
        
        // if a class is already decompiled, then refresh it to apply/remove ZKM deobf
        if (currentClass != null && !currentClass.isEmpty()) {
            decompileSelectedClass();
        }
        
        String status = enabled ? "enabled" : "disabled";
        statusBar.setText("[!] ZKM string deobfuscation " + status);
    }

    private void init() {
        settings = AppSettings.loadSettings();
        themeManager = new ThemeManager();
        
        frame = new JFrame("Sulfur");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 800);

        themeManager.applyTheme(settings.isDarkTheme());

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
                frame.dispose();
            }
        });
        menuBar.add(fileMenu);

        var settingsMenu = new JMenu("Settings");
        useCfrMenuItem = new JCheckBoxMenuItem("Use CFR 0.152 decompiler", settings.isUseCfrDecompiler());
        useCfrMenuItem.addActionListener(e -> toggleDecompilerBackend());
        settingsMenu.add(useCfrMenuItem);
        
        darkThemeMenuItem = new JCheckBoxMenuItem("Dark Theme", settings.isDarkTheme());
        darkThemeMenuItem.addActionListener(e -> toggleDarkTheme());
        settingsMenu.add(darkThemeMenuItem);
        
        settingsMenu.addSeparator();
        
        zkmDeobfuscationMenuItem = new JCheckBoxMenuItem("ZKM String Deobfuscation", 
                settings.isZkmDeobfuscation());
        zkmDeobfuscationMenuItem.addActionListener(e -> toggleZkmDeobfuscation(zkmDeobfuscationMenuItem.isSelected()));
        settingsMenu.add(zkmDeobfuscationMenuItem);
        
        // help menu
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
        
        var bytecodeArea = SwingUtil.monoArea();
        tabs.addTab("Bytecode", new JScrollPane(bytecodeArea));

        statusBar = new JTextArea(1, 0);
        statusBar.setEditable(false);

        var splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, tabs);

        frame.add(splitPane, BorderLayout.CENTER);
        frame.add(statusBar, BorderLayout.SOUTH);

        frame.setVisible(true);
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
        
        // HTML formatting
        String creditsText = "<html><body style='text-align: center; font-family: Arial; margin: 20px;'>" +
                "<h2>The Sulfur Project</h2>" +
                "<p>Developed by k0nnect</p>" +
                "<p>Visit the project on GitHub:</p>" +
                "<p><a href='https://github.com/k0nnect/sulfur'>https://github.com/k0nnect/sulfur</a></p>" +
                "</body></html>";
        creditsPane.setText(creditsText);

        // impl: hyperlink listener to open the url in ur browser
        creditsPane.addHyperlinkListener(e -> {
            if (e.getEventType() == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    Desktop.getDesktop().browse(e.getURL().toURI());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dialog, 
                        "[!] Error opening URL: " + ex.getMessage(),
                        "[!] Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        
        // close button
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dialog.dispose());
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(closeButton);

        dialog.add(new JScrollPane(creditsPane), BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
            }
}