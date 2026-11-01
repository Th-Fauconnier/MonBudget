package com.monbudget.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AppDataLocatorTest {

    @Test
    @DisplayName("Devrait retourner un chemin de base de données valide se terminant par monbudget.db")
    void testGetDatabasePath() {
        Path dbPath = AppDataLocator.getDatabasePath();

        assertNotNull(dbPath, "Le chemin de la base ne doit pas être null");
        assertTrue(dbPath.toString().endsWith("monbudget.db"), "Le fichier doit se nommer monbudget.db");

        // Vérifie que le dossier parent a bien été créé par la méthode
        Path parentDir = dbPath.getParent();
        assertNotNull(parentDir);
        assertTrue(Files.exists(parentDir), "Le répertoire parent de données doit exister");
        assertTrue(Files.isDirectory(parentDir), "Le chemin parent doit être un dossier");
    }
}