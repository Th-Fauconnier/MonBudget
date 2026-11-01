package com.monbudget.util;

import java.nio.file.Files;
import java.nio.file.Path;

public final class AppDataLocator {

    private static Path customPathOverride = null;

    private AppDataLocator() {}

    public static Path getAppDataDirectory() {
        if (customPathOverride != null) {
            return customPathOverride;
        }

        String os = System.getProperty("os.name").toLowerCase();
        String userHome = System.getProperty("user.home");
        Path appDir;

        if (os.contains("win")) {
            String appDataEnv = System.getenv("APPDATA");
            if (appDataEnv != null && !appDataEnv.isBlank()) {
                appDir = Path.of(appDataEnv, "MonBudget");
            } else {
                appDir = Path.of(userHome, "AppData", "Roaming", "MonBudget");
            }
        } else if (os.contains("mac")) {
            appDir = Path.of(userHome, "Library", "Application Support", "MonBudget");
        } else {
            // Linux et autres systèmes Unix (Respect des spécifications XDG)
            String xdgDataHome = System.getenv("XDG_DATA_HOME");
            if (xdgDataHome != null && !xdgDataHome.isBlank()) {
                appDir = Path.of(xdgDataHome, "monbudget");
            } else {
                appDir = Path.of(userHome, ".local", "share", "monbudget");
            }
        }

        // Création automatique du répertoire s'il n'existe pas
        try {
            Files.createDirectories(appDir);
        } catch (Exception e) {
            throw new RuntimeException("Impossible de créer le dossier de données de l'application : " + appDir, e);
        }

        return appDir;
    }

    public static Path getDatabasePath() {
        return getAppDataDirectory().resolve("budget.db");
    }

    /**
     * Permet de définir un chemin personnalisé (utilisé uniquement pour les tests unitaires/d'intégration).
     */
    public static void setCustomPathOverride(Path path) {
        customPathOverride = path;
    }
}