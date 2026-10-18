package com.monbudget.dao;

import com.monbudget.util.AppDataLocator;
import org.flywaydb.core.Flyway;
import org.sqlite.SQLiteConfig;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseManager {

    private static Connection connection;

    private DatabaseManager() {}

    /**
     * Initialise la base et applique les migrations Flyway en attente.
     * À appeler une seule fois au démarrage de l'application.
     */
    public static void initializeDatabase() {
        Path dbPath = AppDataLocator.getDatabasePath();
        String jdbcUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath();

        // 1. Configuration et exécution de Flyway
        Flyway flyway = Flyway.configure(DatabaseManager.class.getClassLoader())
                .dataSource(jdbcUrl, null, null)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true) // Pratique si la base existe déjà
                .load();

        int appliedMigrations = flyway.migrate().migrationsExecuted;
        System.out.printf("Base SQLite vérifiée (%s). %d migration(s) appliquée(s).%n",
                dbPath, appliedMigrations);
    }

    /**
     * Fournit la connexion JDBC configurée pour l'application.
     */
    public static synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            Path dbPath = AppDataLocator.getDatabasePath();
            String jdbcUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath();

            SQLiteConfig config = new SQLiteConfig();
            config.enforceForeignKeys(true);
            config.setJournalMode(SQLiteConfig.JournalMode.WAL);
            config.setBusyTimeout(5000);

            connection = DriverManager.getConnection(jdbcUrl, config.toProperties());
        }
        return connection;
    }
}