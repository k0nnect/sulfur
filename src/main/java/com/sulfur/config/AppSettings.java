package com.sulfur.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public class AppSettings {
    private static final String SETTINGS_FILE = "sulfur_settings.properties";
    private static final String USE_CFR_KEY = "useCfrDecompiler";
    private static final String DARK_THEME_KEY = "darkTheme";
    
    private boolean useCfrDecompiler;
    private boolean darkTheme;
    
    public AppSettings() {
        this.useCfrDecompiler = false;
        this.darkTheme = false;
    }
    
    public static AppSettings loadSettings() {
        AppSettings settings = new AppSettings();
        Properties props = new Properties();
        
        try {
            File file = new File(SETTINGS_FILE);
            if (file.exists()) {
                try (FileInputStream in = new FileInputStream(file)) {
                    props.load(in);
                    settings.useCfrDecompiler = Boolean.parseBoolean(props.getProperty(USE_CFR_KEY, "false"));
                    settings.darkTheme = Boolean.parseBoolean(props.getProperty(DARK_THEME_KEY, "false"));
                }
            }
        } catch (Exception e) {
            System.err.println("[!] Error loading settings: " + e.getMessage());
        }
        
        return settings;
    }
    
    public void saveSettings() {
        Properties props = new Properties();
        props.setProperty(USE_CFR_KEY, Boolean.toString(useCfrDecompiler));
        props.setProperty(DARK_THEME_KEY, Boolean.toString(darkTheme));
        
        try (FileOutputStream out = new FileOutputStream(SETTINGS_FILE)) {
            props.store(out, "Decompiler Settings");
        } catch (Exception e) {
            System.err.println("[!] Error saving settings: " + e.getMessage());
        }
    }

    public boolean isUseCfrDecompiler() {
        return useCfrDecompiler;
    }

    public void setUseCfrDecompiler(boolean useCfrDecompiler) {
        this.useCfrDecompiler = useCfrDecompiler;
    }

    public boolean isDarkTheme() {
        return darkTheme;
    }

    public void setDarkTheme(boolean darkTheme) {
        this.darkTheme = darkTheme;
    }
}