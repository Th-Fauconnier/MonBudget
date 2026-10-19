package com.monbudget.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class AppDataLocator {

    private AppDataLocator() {}

    public static Path getDatabasePath() {
        String os = System.getProperty("os.name").toLowerCase();
        String userHome = System.getProperty("user.home");
        Path appDir;

        if (os.contains("win")) {
            // Windows: %APPDATA%\MonBudget
            String appData = System.getenv("APPDATA");
            appDir = (appData != null) ? Paths.get(appData, "MonBudget") : Paths.get(userHome, "MonBudget");
        } else if (os.contains("mac")) {
            // macOS: ~/Library/Application Support/MonBudget
            appDir = Paths.get(userHome, "Library", "Application Support", "MonBudget");
        } else {
            // Linux/Unix: ~/.local/share/monbudget
            String xdgData = System.getenv("XDG_DATA_HOME");
            appDir = (xdgData != null) ? Paths.get(xdgData, "monbudget") : Paths.get(userHome, ".local", "share", "monbudget");
        }

        try {
            Files.createDirectories(appDir);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de créer le dossier de données : " + appDir, e);
        }

        return appDir.resolve("monbudget.db");
    }
}