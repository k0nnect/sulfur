package com.sulfur.config;

import com.sulfur.ui.theme.ThemeManager.Theme;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.UUID;

public class AppSettings {
    private static final String SETTINGS_FILE = "sulfur_settings.properties";
    private static final String USE_CFR_KEY = "useCfrDecompiler";
    private static final String THEME_KEY = "theme";
    private static final String ZKM_DEOBFUSCATION_KEY = "zkmDeobfuscation";
    private static final String ALLATORI_DEOBFUSCATION_KEY = "allatoriDeobfuscation";
    private static final String DISCORD_RPC_KEY = "discordRichPresence";
    private static final String PROFILE_COUNT_KEY = "decompilerProfileCount";
    private static final String ACTIVE_PROFILE_ID_KEY = "activeDecompilerProfileId";

    private boolean useCfrDecompiler;
    private Theme theme;
    private boolean zkmDeobfuscation;
    private boolean allatoriDeobfuscation;
    private boolean discordRichPresence;

    private Map<String, DecompilerProfile> decompilerProfiles;
    private String activeDecompilerProfileId;

    public AppSettings() {
        this.decompilerProfiles = new HashMap<>();
        // default profile is always present
        DecompilerProfile defaultProfile = new DecompilerProfile("Default", false, Theme.LIGHT, false, false, true);
        this.decompilerProfiles.put(defaultProfile.getId(), defaultProfile);
        this.activeDecompilerProfileId = defaultProfile.getId();

        applyProfileSettings(defaultProfile);
    }

    public static AppSettings loadSettings() {
        AppSettings settings = new AppSettings();
        Properties props = new Properties();
        
        try {
            File file = new File(SETTINGS_FILE);
            if (file.exists()) {
                try (FileInputStream in = new FileInputStream(file)) {
                    props.load(in);

                    // load profiles
                    int profileCount = Integer.parseInt(props.getProperty(PROFILE_COUNT_KEY, "0"));
                    for (int i = 0; i < profileCount; i++) {
                        String id = props.getProperty("profile." + i + ".id");
                        String name = props.getProperty("profile." + i + ".name");
                        boolean useCfr = Boolean.parseBoolean(props.getProperty("profile." + i + ".useCfrDecompiler", "false"));
                        Theme theme = Theme.valueOf(props.getProperty("profile." + i + ".theme", Theme.LIGHT.name()));
                        boolean zkm = Boolean.parseBoolean(props.getProperty("profile." + i + ".zkmDeobfuscation", "false"));
                        boolean allatori = Boolean.parseBoolean(props.getProperty("profile." + i + ".allatoriDeobfuscation", "false"));
                        boolean discord = Boolean.parseBoolean(props.getProperty("profile." + i + ".discordRichPresence", "true"));
                        settings.decompilerProfiles.put(id, new DecompilerProfile(id, name, useCfr, theme, zkm, allatori, discord));
                    }

                    // if no profiles loaded or default is missing, ensure default exists
                    if (!settings.decompilerProfiles.containsKey(settings.activeDecompilerProfileId)) {
                        DecompilerProfile defaultProfile = new DecompilerProfile("Default", false, Theme.LIGHT, false, false, true);
                        settings.decompilerProfiles.put(defaultProfile.getId(), defaultProfile);
                        settings.activeDecompilerProfileId = defaultProfile.getId();
                    }

                    settings.activeDecompilerProfileId = props.getProperty(ACTIVE_PROFILE_ID_KEY, settings.decompilerProfiles.keySet().iterator().next());
                    settings.applyProfileSettings(settings.decompilerProfiles.get(settings.activeDecompilerProfileId));
                }
            }
        } catch (Exception e) {
            System.err.println("[!] Error loading settings: " + e.getMessage());
        }
        
        return settings;
    }
    
    public void saveSettings() {
        Properties props = new Properties();
        
        // save individual settings (these will be overwritten by active profile settings on load)
        props.setProperty(USE_CFR_KEY, Boolean.toString(useCfrDecompiler));
        props.setProperty(THEME_KEY, theme.name());
        props.setProperty(ZKM_DEOBFUSCATION_KEY, Boolean.toString(zkmDeobfuscation));
        props.setProperty(ALLATORI_DEOBFUSCATION_KEY, Boolean.toString(allatoriDeobfuscation));
        props.setProperty(DISCORD_RPC_KEY, Boolean.toString(discordRichPresence));

        // save profiles
        props.setProperty(PROFILE_COUNT_KEY, String.valueOf(decompilerProfiles.size()));
        int i = 0;
        for (DecompilerProfile profile : decompilerProfiles.values()) {
            props.setProperty("profile." + i + ".id", profile.getId());
            props.setProperty("profile." + i + ".name", profile.getName());
            props.setProperty("profile." + i + ".useCfrDecompiler", Boolean.toString(profile.isUseCfrDecompiler()));
            props.setProperty("profile." + i + ".theme", profile.getTheme().name());
            props.setProperty("profile." + i + ".zkmDeobfuscation", Boolean.toString(profile.isZkmDeobfuscation()));
            props.setProperty("profile." + i + ".allatoriDeobfuscation", Boolean.toString(profile.isAllatoriDeobfuscation()));
            props.setProperty("profile." + i + ".discordRichPresence", Boolean.toString(profile.isDiscordRichPresence()));
            i++;
        }
        props.setProperty(ACTIVE_PROFILE_ID_KEY, activeDecompilerProfileId);
        
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
        return theme == Theme.DARK;
    }

    public void setDarkTheme(boolean darkTheme) {
        this.theme = darkTheme ? Theme.DARK : Theme.LIGHT;
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

    public Theme getTheme() {
        return theme;
    }

    public void setTheme(Theme theme) {
        this.theme = theme;
    }

    public static class DecompilerProfile {
        private String id;
        private String name;
        private boolean useCfrDecompiler;
        private Theme theme;
        private boolean zkmDeobfuscation;
        private boolean allatoriDeobfuscation;
        private boolean discordRichPresence;

        public DecompilerProfile(String id, String name, boolean useCfrDecompiler, Theme theme, boolean zkmDeobfuscation, boolean allatoriDeobfuscation, boolean discordRichPresence) {
            this.id = id;
            this.name = name;
            this.useCfrDecompiler = useCfrDecompiler;
            this.theme = theme;
            this.zkmDeobfuscation = zkmDeobfuscation;
            this.allatoriDeobfuscation = allatoriDeobfuscation;
            this.discordRichPresence = discordRichPresence;
        }

        public DecompilerProfile(String name, boolean useCfrDecompiler, Theme theme, boolean zkmDeobfuscation, boolean allatoriDeobfuscation, boolean discordRichPresence) {
            this.id = UUID.randomUUID().toString(); // generate a unique id
            this.name = name;
            this.useCfrDecompiler = useCfrDecompiler;
            this.theme = theme;
            this.zkmDeobfuscation = zkmDeobfuscation;
            this.allatoriDeobfuscation = allatoriDeobfuscation;
            this.discordRichPresence = discordRichPresence;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public boolean isUseCfrDecompiler() { return useCfrDecompiler; }
        public Theme getTheme() { return theme; }
        public boolean isZkmDeobfuscation() { return zkmDeobfuscation; }
        public boolean isAllatoriDeobfuscation() { return allatoriDeobfuscation; }
        public boolean isDiscordRichPresence() { return discordRichPresence; }

        public void setName(String name) { this.name = name; }
        public void setUseCfrDecompiler(boolean useCfrDecompiler) { this.useCfrDecompiler = useCfrDecompiler; }
        public void setTheme(Theme theme) { this.theme = theme; }
        public void setZkmDeobfuscation(boolean zkmDeobfuscation) { this.zkmDeobfuscation = zkmDeobfuscation; }
        public void setAllatoriDeobfuscation(boolean allatoriDeobfuscation) { this.allatoriDeobfuscation = allatoriDeobfuscation; }
        public void setDiscordRichPresence(boolean discordRichPresence) { this.discordRichPresence = discordRichPresence; }
    }

    public DecompilerProfile getActiveProfile() {
        return decompilerProfiles.get(activeDecompilerProfileId);
    }

    public void setActiveProfile(String profileId) {
        if (decompilerProfiles.containsKey(profileId)) {
            this.activeDecompilerProfileId = profileId;
            applyProfileSettings(decompilerProfiles.get(profileId));
        }
    }

    public Collection<DecompilerProfile> getDecompilerProfiles() {
        return decompilerProfiles.values();
    }

    public void addProfile(DecompilerProfile profile) {
        decompilerProfiles.put(profile.getId(), profile);
    }

    public void updateProfile(DecompilerProfile profile) {
        decompilerProfiles.put(profile.getId(), profile);
        if (profile.getId().equals(activeDecompilerProfileId)) {
            applyProfileSettings(profile);
        }
    }

    public void deleteProfile(String profileId) {
        decompilerProfiles.remove(profileId);
        // if the active profile is deleted, switch to default
        String defaultProfileId = null;
        for (DecompilerProfile profile : decompilerProfiles.values()) {
            if ("Default".equals(profile.getName())) {
                defaultProfileId = profile.getId();
                break;
            }
        }

        if (defaultProfileId != null) {
            this.activeDecompilerProfileId = defaultProfileId;
            applyProfileSettings(decompilerProfiles.get(defaultProfileId));
        } else if (!decompilerProfiles.isEmpty()) {

            String firstAvailableProfileId = decompilerProfiles.keySet().iterator().next();
            this.activeDecompilerProfileId = firstAvailableProfileId;
            applyProfileSettings(decompilerProfiles.get(firstAvailableProfileId));
        } else {

            DecompilerProfile newDefaultProfile = new DecompilerProfile("Default", false, Theme.LIGHT, false, false, true);
            this.decompilerProfiles.put(newDefaultProfile.getId(), newDefaultProfile);
            this.activeDecompilerProfileId = newDefaultProfile.getId();
            applyProfileSettings(newDefaultProfile);
        }
    }

    private void applyProfileSettings(DecompilerProfile profile) {
        this.useCfrDecompiler = profile.isUseCfrDecompiler();
        this.theme = profile.getTheme();
        this.zkmDeobfuscation = profile.isZkmDeobfuscation();
        this.allatoriDeobfuscation = profile.isAllatoriDeobfuscation();
        this.discordRichPresence = profile.isDiscordRichPresence();
    }
}