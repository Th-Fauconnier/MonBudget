package com.monbudget.dao;

import com.monbudget.model.RecurrenceRule;
import com.monbudget.model.enums.Frequency;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class RecurrenceRuleDaoTest {

    private Connection connection;
    private RecurrenceRuleDao recurrenceRuleDao;

    @BeforeEach
    void setUp() throws SQLException {
        // Connexion SQLite en mémoire éphémère pour les tests
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");

        // Initialisation du schéma (simulant le passage de Flyway ou la création de la table)
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE recurrence_rule (
                    id           INTEGER PRIMARY KEY AUTOINCREMENT,
                    frequency    TEXT NOT NULL CHECK (frequency IN ('DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY')),
                    interval     INTEGER NOT NULL DEFAULT 1 CHECK (interval > 0),
                    start_date   TEXT NOT NULL,
                    end_date     TEXT,
                    day_of_month INTEGER CHECK (day_of_month BETWEEN 1 AND 31)
                );
            """);
        }

        recurrenceRuleDao = new RecurrenceRuleDao(connection);
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Test
    @DisplayName("Devrait sauvegarder une règle de récurrence complète et générer un ID")
    void testSaveCompleteRecurrenceRule() {
        // Given
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 12, 31);
        RecurrenceRule newRule = new RecurrenceRule(
                null,
                Frequency.MONTHLY,
                1,
                startDate,
                endDate,
                15
        );

        // When
        RecurrenceRule savedRule = recurrenceRuleDao.save(newRule);

        // Then
        assertThat(savedRule).isNotNull();
        assertThat(savedRule.id()).isNotNull().isPositive();
        assertThat(savedRule.frequency()).isEqualTo(Frequency.MONTHLY);
        assertThat(savedRule.interval()).isEqualTo(1);
        assertThat(savedRule.startDate()).isEqualTo(startDate);
        assertThat(savedRule.endDate()).isEqualTo(endDate);
        assertThat(savedRule.dayOfMonth()).isEqualTo(15);
    }

    @Test
    @DisplayName("Devrait sauvegarder une règle avec des champs optionnels nuls (end_date et day_of_month)")
    void testSaveRuleWithNullOptionalFields() {
        // Given
        LocalDate startDate = LocalDate.of(2026, 6, 1);
        RecurrenceRule newRule = new RecurrenceRule(
                null,
                Frequency.WEEKLY,
                2,
                startDate,
                null,
                null
        );

        // When
        RecurrenceRule savedRule = recurrenceRuleDao.save(newRule);

        // Then
        assertThat(savedRule).isNotNull();
        assertThat(savedRule.id()).isNotNull().isPositive();
        assertThat(savedRule.frequency()).isEqualTo(Frequency.WEEKLY);
        assertThat(savedRule.interval()).isEqualTo(2);
        assertThat(savedRule.startDate()).isEqualTo(startDate);
        assertThat(savedRule.endDate()).isNull();
        assertThat(savedRule.dayOfMonth()).isNull();
    }

    @Test
    @DisplayName("Devrait sauvegarder une règle de récurrence et la retrouver par son ID")
    void testSaveAndFindById() {
        // Given
        RecurrenceRule rule = new RecurrenceRule(
                null,
                Frequency.MONTHLY,
                1,
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2027, 11, 1),
                15
        );

        // When
        RecurrenceRule saved = recurrenceRuleDao.save(rule);

        // Then
        assertNotNull(saved.id());
        Optional<RecurrenceRule> found = recurrenceRuleDao.findById(saved.id());

        assertTrue(found.isPresent());
        assertEquals(Frequency.MONTHLY, found.get().frequency());
        assertEquals(1, found.get().interval());
        assertEquals(LocalDate.of(2026, 11, 1), found.get().startDate());
        assertEquals(LocalDate.of(2027, 11, 1), found.get().endDate());
        assertEquals(15, found.get().dayOfMonth());
    }

    @Test
    @DisplayName("Devrait retourner Optional.empty si l'ID n'existe pas")
    void testFindByIdNotFound() {
        Optional<RecurrenceRule> found = recurrenceRuleDao.findById(999L);
        assertTrue(found.isEmpty());
    }
}