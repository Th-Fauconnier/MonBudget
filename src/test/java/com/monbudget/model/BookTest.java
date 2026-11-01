package com.monbudget.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class BookTest {

    @Test
    @DisplayName("Devrait créer un livre avec les valeurs par défaut via la fabrique create")
    void testBookCreateWithDefaults() {
        Book book = Book.create("Compte Personnel", "Description test", null, null);

        assertNotNull(book);
        assertNull(book.id());
        assertEquals("Compte Personnel", book.name());
        assertEquals("Description test", book.description());
        assertEquals("EUR", book.currency()); // Valeur par défaut
        assertEquals(BigDecimal.ZERO, book.initialBalance()); // Valeur par défaut si null
        assertEquals("#3498db", book.colorHex()); // Couleur par défaut si null
        assertEquals("wallet", book.iconName()); // Icône par défaut
        assertFalse(book.isArchived());
        assertNotNull(book.createdAt());
        assertNotNull(book.updatedAt());
    }

    @Test
    @DisplayName("Devrait respecter les valeurs fournies à la fabrique create")
    void testBookCreateCustomValues() {
        BigDecimal customBalance = new BigDecimal("500.00");
        Book book = Book.create("Pro", "Pro desc", customBalance, "#e74c3c");

        assertEquals("Pro", book.name());
        assertEquals(customBalance, book.initialBalance());
        assertEquals("#e74c3c", book.colorHex());
    }

    @Test
    @DisplayName("Devrait convertir correctement le solde initial en centimes")
    void testInitialBalanceInCents() {
        Book book = new Book(
                1L, "Test", "", "EUR",
                new BigDecimal("123.45"),
                "#000000", "wallet", false, null, null
        );

        assertEquals(12345L, book.initialBalanceInCents());
    }
}