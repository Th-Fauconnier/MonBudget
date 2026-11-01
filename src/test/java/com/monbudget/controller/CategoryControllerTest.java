package com.monbudget.controller;

import com.monbudget.model.enums.OperationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CategoryDialogControllerTest {

    private CategoryDialogController controller;
    private AtomicReference<String> lastErrorMessage;

    @BeforeEach
    void setUp() {
        controller = new CategoryDialogController();
        lastErrorMessage = new AtomicReference<>();
    }

    @Test
    @DisplayName("Devrait valider avec succès une catégorie correcte")
    void testValidateFieldsSuccess() {
        boolean isValid = controller.validateFields("Alimentation", OperationType.EXPENSE, lastErrorMessage::set);

        assertTrue(isValid);
        assertEquals("", lastErrorMessage.get());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("Devrait rejeter un nom de catégorie vide, null ou composé d'espaces")
    void testValidateFieldsInvalidName(String invalidName) {
        boolean isValid = controller.validateFields(invalidName, OperationType.EXPENSE, lastErrorMessage::set);

        assertFalse(isValid);
        assertEquals("Le libellé de la catégorie est obligatoire.", lastErrorMessage.get());
    }

    @Test
    @DisplayName("Devrait rejeter une catégorie sans type d'opération sélectionné")
    void testValidateFieldsNullType() {
        boolean isValid = controller.validateFields("Loisirs", null, lastErrorMessage::set);

        assertFalse(isValid);
        assertEquals("Veuillez sélectionner le type d'opération.", lastErrorMessage.get());
    }
}