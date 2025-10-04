package com.sulfur.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public class AppSettings {
    private static final String SETTINGS_FILE = "sulfur_settings.properties";
    private static final String USE_CFR_KEY = "useCfrDecompiler";
    private static final String DARK_THEME_KEY = "darkTheme";
    private static final String ZKM_DEOBFUSCATION_KEY = "zkmDeobfuscation";
    private static final String ALLATORI_DEOBFUSCATION_KEY = "allatoriDeobfuscation";
    private static final String DISCORD_RPC_KEY = "discordRichPresence";
    
    private boolean useCfrDecompiler;
    private boolean darkTheme;
    private boolean zkmDeobfuscation;
    private boolean allatoriDeobfuscation;
    private boolean discordRichPresence;
    
    public AppSettings() {
        this.useCfrDecompiler = false;
        this.darkTheme = false;
        this.zkmDeobfuscation = false;
        this.allatoriDeobfuscation = false;
        this.discordRichPresence = true; // Enabled by default
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
                    settings.zkmDeobfuscation = Boolean.parseBoolean(props.getProperty(ZKM_DEOBFUSCATION_KEY, "false"));
                    settings.allatoriDeobfuscation = Boolean.parseBoolean(props.getProperty(ALLATORI_DEOBFUSCATION_KEY, "false"));
                    settings.discordRichPresence = Boolean.parseBoolean(props.getProperty(DISCORD_RPC_KEY, "true"));
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
        props.setProperty(ZKM_DEOBFUSCATION_KEY, Boolean.toString(zkmDeobfuscation));
        props.setProperty(ALLATORI_DEOBFUSCATION_KEY, Boolean.toString(allatoriDeobfuscation));
        props.setProperty(DISCORD_RPC_KEY, Boolean.toString(discordRichPresence));
        
        try (FileOutputStream out = new FileOutputStream(SETTINGS_FILE)) {
            props.store(out, "Sulfur Decompiler Settings");
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
    
    public boolean isZkmDeobfuscation() {
        return zkmDeobfuscation;
    }
    
    public void setZkmDeobfuscation(boolean zkmDeobfuscation) {
        this.zkmDeobfuscation = zkmDeobfuscation;
    }
    
    public boolean isAllatoriDeobfuscation() {
        return allatoriDeobfuscation;
    }
    
    public void setAllatoriDeobfuscation(boolean allatoriDeobfuscation) {
        this.allatoriDeobfuscation = allatoriDeobfuscation;
    }
    
    public boolean isDiscordRichPresence() {
        return discordRichPresence;
    }
    
    public void setDiscordRichPresence(boolean discordRichPresence) {
        this.discordRichPresence = discordRichPresence;
    }
}