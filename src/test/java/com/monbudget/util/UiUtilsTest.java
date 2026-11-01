package com.monbudget.util;

import com.monbudget.model.enums.OperationType;
import javafx.application.Platform;
import javafx.scene.control.ComboBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class UiUtilsTest {

    @BeforeAll
    static void initToolkit() {
        // Initialise le Toolkit JavaFX de manière sécurisée pour les tests
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Le toolkit est déjà initialisé, on peut continuer
        }
    }

    @Test
    @DisplayName("Devrait configurer correctement le ComboBox de type d'opération")
    void testSetupOperationTypeComboBox() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        AtomicReference<ComboBox<OperationType>> comboBoxHolder = new AtomicReference<>();

        Platform.runLater(() -> {
            ComboBox<OperationType> comboBox = new ComboBox<>();
            UiUtils.setupOperationTypeComboBox(comboBox);
            comboBoxHolder.set(comboBox);
            latch.countDown();
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS), "L'initialisation JavaFX a expiré");

        ComboBox<OperationType> comboBox = comboBoxHolder.get();
        assertNotNull(comboBox);
        assertEquals(2, comboBox.getItems().size(), "Le ComboBox doit contenir exactement EXPENSE et INCOME");
        assertTrue(comboBox.getItems().contains(OperationType.EXPENSE));
        assertTrue(comboBox.getItems().contains(OperationType.INCOME));
        assertEquals(OperationType.EXPENSE, comboBox.getValue(), "La valeur initiale par défaut doit être EXPENSE");
    }
}