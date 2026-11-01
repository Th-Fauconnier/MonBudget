package com.monbudget.dao;

import com.monbudget.util.AppDataLocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseManagerTest {

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        // Redirige le stockage de la base vers un dossier temporaire unique pour chaque test
        AppDataLocator.setCustomPathOverride(tempDir);
    }

    @AfterEach
    void tearDown() {
        // Nettoie l'override pour ne pas impacter les autres tests
        AppDataLocator.setCustomPathOverride(null);
    }

    @Test
    @DisplayName("Devrait initialiser la base de données et appliquer les configurations SQLite")
    void testInitializeDatabaseAndConnection() throws Exception {
        // When : Initialisation de la base (exécute Flyway et configure SQLite)
        DatabaseManager.initializeDatabase();

        // Then : Vérification de la connexion et des pragmas (clés étrangères, mode WAL)
        try (Connection connection = DatabaseManager.getConnection()) {
            assertNotNull(connection);
            assertFalse(connection.isClosed());

            // Vérification que les clés étrangères sont bien activées par le DatabaseManager
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("PRAGMA foreign_keys;")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1), "Les clés étrangères doivent être activées");
            }
        }
    }
}