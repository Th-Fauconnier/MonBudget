package com.monbudget.controller;

import com.monbudget.model.Book;
import com.monbudget.util.DashboardValidator; // Import du validateur extrait
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DashboardControllerTest {

    private DashboardController controller;
    private Book validBook;

    @BeforeEach
    void setUp() {
        controller = new DashboardController();
        validBook = new Book(1L, "Personnel", null, "EUR", BigDecimal.ZERO, "#3498db", "wallet", false, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void testValidateFilters_NullBook() {
        StringBuilder errorMsg = new StringBuilder();
        // Appel via DashboardValidator
        boolean isValid = DashboardValidator.validateFilters(null, LocalDate.now(), LocalDate.now(), errorMsg::append);

        assertFalse(isValid);
        assertEquals("Veuillez sélectionner un livre de comptes.", errorMsg.toString());
    }

    @Test
    void testValidateFilters_NullDates() {
        StringBuilder errorMsg = new StringBuilder();
        // Appel via DashboardValidator
        boolean isValid = DashboardValidator.validateFilters(validBook, null, LocalDate.now(), errorMsg::append);

        assertFalse(isValid);
        assertEquals("Les dates de début et de fin sont obligatoires.", errorMsg.toString());
    }

    @Test
    void testValidateFilters_StartAfterEnd() {
        StringBuilder errorMsg = new StringBuilder();
        LocalDate start = LocalDate.of(2026, 1, 10);
        LocalDate end = LocalDate.of(2026, 1, 1);
        // Appel via DashboardValidator
        boolean isValid = DashboardValidator.validateFilters(validBook, start, end, errorMsg::append);

        assertFalse(isValid);
        assertEquals("La date de début ne peut pas être postérieure à la date de fin.", errorMsg.toString());
    }

    @Test
    void testValidateFilters_Valid() {
        StringBuilder errorMsg = new StringBuilder();
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 10);
        // Appel via DashboardValidator
        boolean isValid = DashboardValidator.validateFilters(validBook, start, end, errorMsg::append);

        assertTrue(isValid);
        assertEquals("", errorMsg.toString());
    }
}