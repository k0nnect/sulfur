package com.sulfur.rpc;

import club.minnced.discord.rpc.DiscordEventHandlers;
import club.minnced.discord.rpc.DiscordRPC;
import club.minnced.discord.rpc.DiscordRichPresence;

import java.time.Instant;

public class DiscordService {
    private static final String APPLICATION_ID = "1424078232444997652";
    
    private static DiscordService instance;
    private final DiscordRPC rpc;
    private final DiscordRichPresence presence;
    private boolean initialized = false;
    private boolean enabled = false;
    private long startTimestamp;
    
    private String currentJarName = "";
    private String currentClassName = "";
    private String decompilerBackend = "";
    
    private DiscordService() {
        this.rpc = DiscordRPC.INSTANCE;
        this.presence = new DiscordRichPresence();
        this.startTimestamp = Instant.now().getEpochSecond();
    }
    
    public static synchronized DiscordService getInstance() {
        if (instance == null) {
            instance = new DiscordService();
        }
        return instance;
    }
    
    public void initialize() {
        if (initialized) return;
        
        try {
            DiscordEventHandlers handlers = new DiscordEventHandlers();
            handlers.ready = (user) -> System.out.println("[!] sulfur: Discord RPC ready for user: " + user.username + "#" + user.discriminator);
            
            rpc.Discord_Initialize(APPLICATION_ID, handlers, true, null);

            presence.startTimestamp = startTimestamp;
            presence.largeImageKey = "sulfur_icon";
            presence.largeImageText = "Sulfur Decompiler";
            presence.smallImageKey = "java_icon";
            presence.smallImageText = "Analyzing Java bytecode";
            updatePresence("Idle", "Ready to decompile", false);

            new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    rpc.Discord_RunCallbacks();
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }, "Discord-RPC-Callback-Handler").start();
            
            initialized = true;
            System.out.println("[!] sulfur: Discord Rich Presence initialized");
        } catch (Exception e) {
            System.err.println("[!] sulfur: Failed to initialize Discord RPC: " + e.getMessage());
        }
    }
    
    public void enable() {
        if (!initialized) {
            initialize();
        }
        enabled = true;
        updatePresence();
    }
    
    public void disable() {
        enabled = false;
        try {
            DiscordRichPresence emptyPresence = new DiscordRichPresence();
            rpc.Discord_UpdatePresence(emptyPresence);
        } catch (Exception e) {
            System.err.println("[!] sulfur: Error clearing Discord presence: " + e.getMessage());
        }
    }
    
    public void shutdown() {
        if (!initialized) return;
        
        try {
            rpc.Discord_Shutdown();
            initialized = false;
            System.out.println("[!] sulfur: Discord Rich Presence shutdown");
        } catch (Exception e) {
            System.err.println("[!] sulfur: Error shutting down Discord RPC: " + e.getMessage());
        }
    }
    
    public void setCurrentJar(String jarName) {
        this.currentJarName = jarName;
        updatePresence();
    }
    
    public void setCurrentClass(String className) {
        this.currentClassName = className;
        updatePresence();
    }
    
    public void setDecompilerBackend(String decompiler) {
        this.decompilerBackend = decompiler;
        updatePresence();
    }
    
    public void updatePresence() {
        if (!enabled || !initialized) return;
        
        String details;
        String state;
        boolean showTimestamp = true;
        
        if (currentJarName.isEmpty()) {
            details = "Idle";
            state = "Ready to decompile";
            showTimestamp = false;
        } else if (currentClassName.isEmpty()) {
            details = "Analyzing: " + currentJarName;
            state = "Browsing classes";
        } else {
            details = "Decompiling: " + shortenClassName(currentClassName);

            if (!decompilerBackend.isEmpty()) {
                state = "Using " + decompilerBackend + " | JAR: " + currentJarName;
            } else {
                state = "JAR: " + currentJarName;
            }
        }
        
        updatePresence(details, state, showTimestamp);
    }
    
    private void updatePresence(String details, String state, boolean showTimestamp) {
        if (!initialized) return;
        
        try {
            presence.details = details;
            presence.state = state;
            
            if (showTimestamp) {
                presence.startTimestamp = startTimestamp;
            } else {
                presence.startTimestamp = 0;
            }
            
            rpc.Discord_UpdatePresence(presence);
        } catch (Exception e) {
            System.err.println("[!] sulfur: Error updating Discord presence: " + e.getMessage());
        }
    }
    
    private String shortenClassName(String className) {
        if (className == null || className.isEmpty()) {
            return "";
        }

        if (className.length() < 35) {
            return className;
        }

        int lastDot = className.lastIndexOf('.');
        if (lastDot > 0) {
            String[] parts = className.split("\\.");
            StringBuilder shortened = new StringBuilder();

            for (int i = 0; i < parts.length - 1; i++) {
                shortened.append(parts[i].charAt(0)).append(".");
            }

            shortened.append(parts[parts.length - 1]);
            return shortened.toString();
        }
        
        return className;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public boolean isInitialized() {
        return initialized;
    }
}
