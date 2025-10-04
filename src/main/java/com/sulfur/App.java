package com.sulfur;

import com.sulfur.rpc.DiscordService;
import com.sulfur.ui.MainWindow;

public class App {
    public static void main(String[] args) {
        // register shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            DiscordService.getInstance().shutdown();
        }));
        
        MainWindow.launch();
    }
}