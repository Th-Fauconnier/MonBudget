package com.monbudget.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AppDataLocatorTest {

    // Déclaré en champ de classe pour être partagé proprement entre setUp et le test
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        AppDataLocator.setCustomPathOverride(tempDir);
    }

    @AfterEach
    void tearDown() {
        AppDataLocator.setCustomPathOverride(null);
    }

    @Test
    @DisplayName("Devrait retourner un chemin de base de données valide isolé dans le répertoire temporaire")
    void testGetDatabasePath() {
        Path dbPath = AppDataLocator.getDatabasePath();

        assertNotNull(dbPath, "Le chemin de la base ne doit pas être null");
        assertTrue(dbPath.toString().endsWith("budget.db") || dbPath.toString().endsWith("monbudget.db"),
                "Le fichier doit se nommer budget.db ou monbudget.db");

        Path parentDir = dbPath.getParent();
        assertNotNull(parentDir);
        assertTrue(Files.exists(parentDir), "Le répertoire parent de données doit exister");
        assertTrue(Files.isDirectory(parentDir), "Le chemin parent doit être un dossier");

        // Vérification exacte et robuste par résolution de chemins réels
        try {
            assertEquals(tempDir.toRealPath(), parentDir.toRealPath(),
                    "Le dossier parent doit être strictement égal au répertoire temporaire configuré");
        } catch (Exception e) {
            assertEquals(tempDir.toAbsolutePath().normalize(), parentDir.toAbsolutePath().normalize());
        }
    }
}