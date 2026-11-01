package com.monbudget.controller;

import com.monbudget.model.Book;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class DashboardControllerTest {

    private DashboardController controller;
    private AtomicReference<String> lastErrorMessage;
    private Book validBook;
    private LocalDate startDate;
    private LocalDate endDate;

    @BeforeEach
    void setUp() {
        controller = new DashboardController();
        lastErrorMessage = new AtomicReference<>();
        validBook = new Book(1L, "Personnel", "Compte perso", "EUR", BigDecimal.ZERO, "#3498db", "wallet", false, LocalDateTime.now(), LocalDateTime.now());
        startDate = LocalDate.of(2026, 11, 1);
        endDate = LocalDate.of(2026, 11, 30);
    }

    @Test
    @DisplayName("Devrait valider avec succès des filtres corrects")
    void testValidateFiltersSuccess() {
        boolean isValid = controller.validateFilters(validBook, startDate, endDate, lastErrorMessage::set);

        assertTrue(isValid);
        assertEquals("", lastErrorMessage.get());
    }

    @Test
    @DisplayName("Devrait rejeter si aucun livre n'est sélectionné")
    void testValidateFiltersNullBook() {
        boolean isValid = controller.validateFilters(null, startDate, endDate, lastErrorMessage::set);

        assertFalse(isValid);
        assertEquals("Veuillez sélectionner un livre de comptes.", lastErrorMessage.get());
    }

    @Test
    @DisplayName("Devrait rejeter si les dates sont nulles")
    void testValidateFiltersNullDates() {
        boolean isValid = controller.validateFilters(validBook, null, endDate, lastErrorMessage::set);
        assertFalse(isValid);
        assertEquals("Les dates de début et de fin sont obligatoires.", lastErrorMessage.get());

        boolean isValid2 = controller.validateFilters(validBook, startDate, null, lastErrorMessage::set);
        assertFalse(isValid2);
        assertEquals("Les dates de début et de fin sont obligatoires.", lastErrorMessage.get());
    }

    @Test
    @DisplayName("Devrait rejeter si la date de début est après la date de fin")
    void testValidateFiltersStartAfterEnd() {
        LocalDate laterStart = LocalDate.of(2026, 12, 1);
        boolean isValid = controller.validateFilters(validBook, laterStart, endDate, lastErrorMessage::set);

        assertFalse(isValid);
        assertEquals("La date de début ne peut pas être postérieure à la date de fin.", lastErrorMessage.get());
    }
}