package com.monbudget.dao;

import com.monbudget.model.Book;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class BookDaoTest {

    private Connection connection;
    private BookDao bookDao;

    @BeforeEach
    void setUp() throws Exception {
        // Connexion SQLite en mémoire
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");

        // Création du schéma de la table book (simulant les migrations Flyway)
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE book (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    description TEXT,
                    currency TEXT DEFAULT 'EUR',
                    initial_balance INTEGER DEFAULT 0,
                    color_hex TEXT DEFAULT '#3498db',
                    icon_name TEXT DEFAULT 'wallet',
                    is_archived INTEGER DEFAULT 0,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
        }

        bookDao = new BookDao(connection);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Test
    @DisplayName("Devrait sauvegarder un livre et le retrouver par son ID")
    void testSaveAndFindById() {
        // Given
        Book newBook = Book.create("Compte Courant", "Compte principal", new BigDecimal("1500.50"), "#2ecc71");

        // When
        Book savedBook = bookDao.save(newBook);

        // Then
        assertNotNull(savedBook.id());
        Optional<Book> found = bookDao.findById(savedBook.id());

        assertTrue(found.isPresent());
        assertEquals("Compte Courant", found.get().name());
        assertEquals("Compte principal", found.get().description());
        assertEquals(new BigDecimal("1500.50"), found.get().initialBalance());
        assertEquals("#2ecc71", found.get().colorHex());
        assertFalse(found.get().isArchived());
    }

    @Test
    @DisplayName("Devrait récupérer uniquement les livres actifs (non archivés)")
    void testFindAllActive() {
        // Given
        Book activeBook = Book.create("Livret A", "Épargne", new BigDecimal("500.00"), "#3498db");
        Book archivedBook = Book.create("Ancien Compte", "Fermé", BigDecimal.ZERO, "#e74c3c");

        Book savedActive = bookDao.save(activeBook);
        Book savedArchived = bookDao.save(archivedBook);

        // Archivage du second livre
        savedArchived = new Book(
                savedArchived.id(), savedArchived.name(), savedArchived.description(),
                savedArchived.currency(), savedArchived.initialBalance(), savedArchived.colorHex(),
                savedArchived.iconName(), true, savedArchived.createdAt(), savedArchived.updatedAt()
        );
        bookDao.update(savedArchived);

        // When
        List<Book> activeBooks = bookDao.findAllActive();

        // Then
        assertEquals(1, activeBooks.size());
        assertEquals(savedActive.id(), activeBooks.getFirst().id());
        assertEquals("Livret A", activeBooks.getFirst().name());
    }

    @Test
    @DisplayName("Devrait mettre à jour un livre existant")
    void testUpdateBook() {
        // Given
        Book book = bookDao.save(Book.create("Projet", "Vacances", new BigDecimal("200.00"), "#e67e22"));

        // When
        Book updated = new Book(
                book.id(), "Projet Vacances Été", "Mis à jour", "EUR",
                new BigDecimal("350.00"), "#f1c40f", "plane", false,
                book.createdAt(), book.updatedAt()
        );
        bookDao.update(updated);

        // Then
        Book found = bookDao.findById(book.id()).orElseThrow();
        assertEquals("Projet Vacances Été", found.name());
        assertEquals("Mis à jour", found.description());
        assertEquals(new BigDecimal("350.00"), found.initialBalance());
        assertEquals("#f1c40f", found.colorHex());
    }

    @Test
    @DisplayName("Devrait supprimer un livre par son ID")
    void testDeleteBook() {
        // Given
        Book book = bookDao.save(Book.create("À supprimer", "Test", BigDecimal.ZERO, "#000000"));
        long id = book.id();

        // When
        bookDao.delete(id);

        // Then
        Optional<Book> found = bookDao.findById(id);
        assertTrue(found.isEmpty());
    }
}