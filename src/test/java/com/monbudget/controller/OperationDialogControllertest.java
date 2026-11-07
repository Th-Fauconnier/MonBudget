package com.monbudget.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class OperationDialogControllerTest {

    private OperationDialogController controller;
    private AtomicReference<String> lastErrorMessage;
    private LocalDate validDate;

    @BeforeEach
    void setUp() {
        controller = new OperationDialogController();
        lastErrorMessage = new AtomicReference<>();
        validDate = LocalDate.of(2026, 11, 1);
    }

    @Test
    @DisplayName("Devrait valider avec succès une opération correcte")
    void testValidateFieldsSuccess() {
        boolean isValid = controller.validateFields("Courses", "45.90", validDate, false, null, lastErrorMessage::set);

        assertTrue(isValid);
        assertEquals("", lastErrorMessage.get());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("Devrait rejeter un libellé vide, null ou composé d'espaces")
    void testValidateFieldsInvalidLabel(String invalidLabel) {
        boolean isValid = controller.validateFields(invalidLabel, "20.00", validDate, false, null, lastErrorMessage::set);

        assertFalse(isValid);
        assertEquals("Veuillez saisir un libellé.", lastErrorMessage.get());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Devrait rejeter un montant vide ou null")
    void testValidateFieldsEmptyAmount(String emptyAmount) {
        boolean isValid = controller.validateFields("Courses", emptyAmount, validDate, false, null, lastErrorMessage::set);

        assertFalse(isValid);
        assertEquals("Veuillez indiquer un montant.", lastErrorMessage.get());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.00", "-15.50"})
    @DisplayName("Devrait rejeter un montant négatif ou nul")
    void testValidateFieldsNonPositiveAmount(String invalidAmount) {
        boolean isValid = controller.validateFields("Courses", invalidAmount, validDate, false, null, lastErrorMessage::set);

        assertFalse(isValid);
        assertEquals("Le montant doit être strictement positif.", lastErrorMessage.get());
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "10.50.20"})
    @DisplayName("Devrait rejeter un montant mal formaté")
    void testValidateFieldsMalformedAmount(String malformedAmount) {
        boolean isValid = controller.validateFields("Courses", malformedAmount, validDate, false, null, lastErrorMessage::set);

        assertFalse(isValid);
        assertEquals("Format du montant incorrect (ex : 29.90).", lastErrorMessage.get());
    }

    @Test
    @DisplayName("Devrait rejeter une opération sans date sélectionnée")
    void testValidateFieldsNullDate() {
        boolean isValid = controller.validateFields("Courses", "25.00", null, false, null, lastErrorMessage::set);

        assertFalse(isValid);
        assertEquals("Veuillez sélectionner une date.", lastErrorMessage.get());
    }

    @Test
    @DisplayName("Devrait rejeter une récurrence si la date de fin est antérieure à la date de l'opération")
    void testValidateFieldsInvalidRecurrenceEndDate() {
        LocalDate operationDate = LocalDate.of(2026, 11, 10);
        LocalDate endDateBefore = LocalDate.of(2026, 11, 5);

        boolean isValid = controller.validateFields("Abonnement", "10.00", operationDate, true, endDateBefore, lastErrorMessage::set);

        assertFalse(isValid);
        assertEquals("La date de fin de récurrence ne peut pas être antérieure à la date de l'opération.", lastErrorMessage.get());
    }

    @Test
    @DisplayName("Devrait valider avec succès une récurrence si la date de fin est postérieure ou absente")
    void testValidateFieldsValidRecurrenceEndDate() {
        LocalDate operationDate = LocalDate.of(2026, 11, 10);
        LocalDate endDateValid = LocalDate.of(2027, 11, 10);

        boolean isValid = controller.validateFields("Abonnement", "10.00", operationDate, true, endDateValid, lastErrorMessage::set);

        assertTrue(isValid);
        assertEquals("", lastErrorMessage.get());
    }
}