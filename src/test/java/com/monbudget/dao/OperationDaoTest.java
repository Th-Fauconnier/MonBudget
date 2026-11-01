package com.monbudget.dao;

import com.monbudget.model.Operation;
import com.monbudget.model.enums.OperationStatus;
import com.monbudget.model.enums.OperationType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class OperationDaoTest {

    private Connection connection;
    private OperationDao operationDao;

    @BeforeEach
    void setUp() throws Exception {
        // Connexion SQLite en mémoire
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");

        // Création du schéma de la table operation (simulant vos migrations Flyway)
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE operation (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    book_id INTEGER NOT NULL,
                    category_id INTEGER,
                    payment_method_id INTEGER,
                    recurrence_rule_id INTEGER,
                    label TEXT NOT NULL,
                    amount INTEGER NOT NULL,
                    operation_type TEXT NOT NULL,
                    status TEXT NOT NULL,
                    operation_date TEXT NOT NULL,
                    settled_at TEXT,
                    notes TEXT,
                    transfer_operation_id INTEGER,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
        }

        operationDao = new OperationDao(connection);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Test
    @DisplayName("Devrait sauvegarder une opération et la retrouver par ID")
    void testSaveAndFindById() {
        // Given
        Operation operation = new Operation(
                null, 1L, null, null, null,
                "Courses Supermarché", new BigDecimal("45.90"),
                OperationType.EXPENSE, OperationStatus.SETTLED,
                LocalDate.of(2026, 10, 18), LocalDate.of(2026, 10, 18),
                "Courses de la semaine", null, null, null
        );

        // When
        Operation saved = operationDao.save(operation);

        // Then
        assertNotNull(saved.id());
        Optional<Operation> found = operationDao.findById(saved.id());

        assertTrue(found.isPresent());
        assertEquals("Courses Supermarché", found.get().label());
        assertEquals(new BigDecimal("45.90"), found.get().amount());
        assertEquals(OperationType.EXPENSE, found.get().operationType());
        assertEquals(OperationStatus.SETTLED, found.get().status());
        assertEquals(LocalDate.of(2026, 10, 18), found.get().operationDate());
        assertTrue(found.get().isSettled());
        assertFalse(found.get().isPlanned());
    }

    @Test
    @DisplayName("Devrait filtrer les opérations par livre et par plage de dates")
    void testFindByBookAndDateRange() {
        // Given
        long bookId = 1L;
        long otherBookId = 2L;

        Operation op1 = new Operation(null, bookId, null, null, null, "Op 1", new BigDecimal("10.00"), OperationType.EXPENSE, OperationStatus.PLANNED, LocalDate.of(2026, 10, 10), null, null, null, null, null);
        Operation op2 = new Operation(null, bookId, null, null, null, "Op 2", new BigDecimal("20.00"), OperationType.INCOME, OperationStatus.SETTLED, LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 15), null, null, null, null);
        Operation op3 = new Operation(null, bookId, null, null, null, "Op Hors Date", new BigDecimal("30.00"), OperationType.EXPENSE, OperationStatus.PLANNED, LocalDate.of(2026, 11, 1), null, null, null, null, null);
        Operation op4 = new Operation(null, otherBookId, null, null, null, "Autre Livre", new BigDecimal("40.00"), OperationType.EXPENSE, OperationStatus.PLANNED, LocalDate.of(2026, 10, 12), null, null, null, null, null);

        operationDao.save(op1);
        operationDao.save(op2);
        operationDao.save(op3);
        operationDao.save(op4);

        // When : Filtrage entre le 2026-10-01 et le 2026-10-20 pour le bookId 1
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 10, 20);
        List<Operation> results = operationDao.findByBookAndDateRange(bookId, start, end);

        // Then
        assertEquals(2, results.size());
        // Tri par défaut : operation_date DESC, id DESC -> Op 2 (15 oct) puis Op 1 (10 oct)
        assertEquals("Op 2", results.get(0).label());
        assertEquals("Op 1", results.get(1).label());
    }

    @Test
    @DisplayName("Devrait mettre à jour une opération existante")
    void testUpdateOperation() {
        // Given
        Operation initial = new Operation(null, 1L, null, null, null, "Facture", new BigDecimal("100.00"), OperationType.EXPENSE, OperationStatus.PLANNED, LocalDate.of(2026, 10, 18), null, "À payer", null, null, null);
        Operation saved = operationDao.save(initial);

        // When : Passage au statut réglé
        Operation updated = saved.markAsSettled(LocalDate.of(2026, 10, 19));
        operationDao.update(updated);

        // Then
        Operation found = operationDao.findById(saved.id()).orElseThrow();
        assertEquals(OperationStatus.SETTLED, found.status());
        assertEquals(LocalDate.of(2026, 10, 19), found.settledAt());
        assertTrue(found.isSettled());
    }

    @Test
    @DisplayName("Devrait supprimer une opération par son ID")
    void testDeleteOperation() {
        // Given
        Operation saved = operationDao.save(new Operation(null, 1L, null, null, null, "Suppression", new BigDecimal("5.00"), OperationType.EXPENSE, OperationStatus.PLANNED, LocalDate.of(2026, 10, 18), null, null, null, null, null));
        long id = saved.id();

        // When
        operationDao.delete(id);

        // Then
        Optional<Operation> found = operationDao.findById(id);
        assertTrue(found.isEmpty());
    }
}