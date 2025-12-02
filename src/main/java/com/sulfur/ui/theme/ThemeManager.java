package com.sulfur.ui.theme;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.OceanTheme;
import java.awt.*;

public class ThemeManager {
    public enum Theme {
        LIGHT,
        DARK
    }

    private static final class DarkTheme extends OceanTheme {
        private final ColorUIResource primary1 = new ColorUIResource(66, 66, 66);
        private final ColorUIResource primary2 = new ColorUIResource(50, 50, 50);
        private final ColorUIResource primary3 = new ColorUIResource(100, 100, 100);

        private final ColorUIResource secondary1 = new ColorUIResource(80, 80, 80);
        private final ColorUIResource secondary2 = new ColorUIResource(60, 60, 60);
        private final ColorUIResource secondary3 = new ColorUIResource(40, 40, 40);

        private final ColorUIResource black = new ColorUIResource(200, 200, 200);
        private final ColorUIResource white = new ColorUIResource(30, 30, 30);

        @Override
        protected ColorUIResource getPrimary1() { return primary1; }
        @Override
        protected ColorUIResource getPrimary2() { return primary2; }
        @Override
        protected ColorUIResource getPrimary3() { return primary3; }

        @Override
        protected ColorUIResource getSecondary1() { return secondary1; }
        @Override
        protected ColorUIResource getSecondary2() { return secondary2; }
        @Override
        protected ColorUIResource getSecondary3() { return secondary3; }

        @Override
        protected ColorUIResource getBlack() { return black; }
        @Override
        protected ColorUIResource getWhite() { return white; }
    }

    public static void applyTheme(Theme theme) {
        try {
            if (theme == Theme.DARK) {
                MetalLookAndFeel.setCurrentTheme(new DarkTheme());
            } else {
                MetalLookAndFeel.setCurrentTheme(new OceanTheme());
            }
            UIManager.setLookAndFeel(new MetalLookAndFeel());

            if (theme == Theme.DARK) {
                UIManager.put("Panel.background", new Color(40, 40, 40));
                UIManager.put("TextPane.background", new Color(30, 30, 30));
                UIManager.put("TextPane.foreground", new Color(200, 200, 200));
                UIManager.put("TextArea.background", new Color(30, 30, 30));
                UIManager.put("TextArea.foreground", new Color(200, 200, 200));
                UIManager.put("TextField.background", new Color(45, 45, 45));
                UIManager.put("TextField.foreground", new Color(200, 200, 200));
                UIManager.put("Tree.background", new Color(40, 40, 40));
                UIManager.put("Tree.foreground", new Color(200, 200, 200));
                UIManager.put("TabbedPane.background", new Color(50, 50, 50));
                UIManager.put("TabbedPane.foreground", new Color(200, 200, 200));
            } else {
                UIManager.put("Panel.background", null);
                UIManager.put("TextPane.background", null);
                UIManager.put("TextPane.foreground", null);
                UIManager.put("TextArea.background", null);
                UIManager.put("TextArea.foreground", null);
                UIManager.put("TextField.background", null);
                UIManager.put("TextField.foreground", null);
                UIManager.put("Tree.background", null);
                UIManager.put("Tree.foreground", null);
                UIManager.put("TabbedPane.background", null);
                UIManager.put("TabbedPane.foreground", null);
            }

            SwingUtilities.updateComponentTreeUI(getActiveWindow());
        } catch (Exception e) {
            System.err.println("[!] Failed to set theme: " + e.getMessage());
        }
    }

    private static Window getActiveWindow() {
        Window[] windows = Window.getWindows();
        for (Window window : windows) {
            if (window.isActive()) {
                return window;
            }
        }
        return windows.length > 0 ? windows[0] : null;
    }
}