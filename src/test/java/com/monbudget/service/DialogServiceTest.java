package com.monbudget.service;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class DialogServiceTest {

    @BeforeAll
    static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Le toolkit est déjà initialisé si d'autres tests s'exécutent en même temps
        }
    }

    @Test
    void testDialogServiceInitialization() {
        assertDoesNotThrow(() -> new DialogService());
    }
}