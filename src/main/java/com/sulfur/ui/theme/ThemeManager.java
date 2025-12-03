package com.sulfur.ui.theme;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import java.awt.*;

public class ThemeManager {
    public enum Theme {
        LIGHT,
        DARK
    }

    public static void applyTheme(Theme theme) {
        try {
            UIManager.getDefaults().clear();

            switch (theme) {
                case DARK:
                    FlatDarkLaf.setup();
                    break;
                case LIGHT:
                    FlatLightLaf.setup();
                    break;
            }
            UIManager.setLookAndFeel(new FlatDarkLaf()); 
        } catch (Exception e) {
            System.err.println("[!] Failed to apply theme: " + e.getMessage());
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