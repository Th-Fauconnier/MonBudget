package com.monbudget.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class BookDialogControllerTest {

    private BookDialogController controller;
    private AtomicReference<String> lastErrorMessage;

    @BeforeEach
    void setUp() {
        controller = new BookDialogController();
        lastErrorMessage = new AtomicReference<>();
    }

    @Test
    @DisplayName("Devrait valider avec succès des entrées correctes")
    void testValidateFieldsSuccess() {
        boolean isValid = controller.validateFields("Compte Principal", "EUR", "150.50", lastErrorMessage::set);

        assertTrue(isValid);
        assertEquals("", lastErrorMessage.get());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("Devrait rejeter un nom de livre vide ou null")
    void testValidateFieldsInvalidName(String invalidName) {
        boolean isValid = controller.validateFields(invalidName, "EUR", "100.00", lastErrorMessage::set);

        assertFalse(isValid);
        assertEquals("Le nom du livre est obligatoire.", lastErrorMessage.get());
    }

    @ParameterizedTest
    @ValueSource(strings = {"EU", "EURO", "US", ""})
    @DisplayName("Devrait rejeter une devise ne faisant pas exactement 3 caractères")
    void testValidateFieldsInvalidCurrency(String invalidCurrency) {
        boolean isValid = controller.validateFields("Mon Compte", invalidCurrency, "100.00", lastErrorMessage::set);

        assertFalse(isValid);
        assertEquals("La devise doit comporter 3 lettres (ex : EUR, USD).", lastErrorMessage.get());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"abc", "10.50.20", " "})
    @DisplayName("Devrait rejeter un solde initial mal formaté ou non numérique")
    void testValidateFieldsInvalidBalance(String invalidBalance) {
        boolean isValid = controller.validateFields("Mon Compte", "EUR", invalidBalance, lastErrorMessage::set);

        assertFalse(isValid);
        assertEquals("Le solde de départ est invalide.", lastErrorMessage.get());
    }
}