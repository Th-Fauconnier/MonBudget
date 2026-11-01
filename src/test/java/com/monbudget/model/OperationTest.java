package com.monbudget.model;

import com.monbudget.model.enums.OperationStatus;
import com.monbudget.model.enums.OperationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class OperationTest {

    private Operation plannedOperation;

    @BeforeEach
    void setUp() {
        plannedOperation = new Operation(
                1L,
                1L,
                2L,
                null,
                null,
                "Courses",
                new BigDecimal("45.50"),
                OperationType.EXPENSE,
                OperationStatus.PLANNED,
                LocalDate.of(2026, 11, 1),
                null,
                "Notes",
                null,
                LocalDateTime.of(2026, 11, 1, 10, 0),
                LocalDateTime.of(2026, 11, 1, 10, 0)
        );
    }

    @Test
    @DisplayName("Devrait identifier correctement le statut via isPlanned et isSettled")
    void testStatusHelpers() {
        assertTrue(plannedOperation.isPlanned());
        assertFalse(plannedOperation.isSettled());

        Operation settledOperation = plannedOperation.markAsSettled(LocalDate.now());
        assertFalse(settledOperation.isPlanned());
        assertTrue(settledOperation.isSettled());
    }

    @Test
    @DisplayName("Devrait marquer l'opération comme réglée avec la date spécifiée via markAsSettled")
    void testMarkAsSettledWithCustomDate() {
        LocalDate customSettlementDate = LocalDate.of(2026, 11, 2);
        Operation settled = plannedOperation.markAsSettled(customSettlementDate);

        assertEquals(OperationStatus.SETTLED, settled.status());
        assertEquals(customSettlementDate, settled.settledAt());
        assertEquals(plannedOperation.label(), settled.label());
        assertEquals(plannedOperation.amount(), settled.amount());
        assertNotNull(settled.updatedAt());
    }

    @Test
    @DisplayName("Devrait utiliser la date du jour si la date de règlement passée à markAsSettled est null")
    void testMarkAsSettledWithNullDateDefaultsToToday() {
        Operation settled = plannedOperation.markAsSettled(null);

        assertEquals(OperationStatus.SETTLED, settled.status());
        assertEquals(LocalDate.now(), settled.settledAt());
    }

    @Test
    @DisplayName("Devrait convertir correctement le montant en centimes via MoneyHelper")
    void testAmountInCents() {
        Operation op = new Operation(
                1L, 1L, null, null, null, "Test",
                new BigDecimal("12.34"),
                OperationType.INCOME, OperationStatus.SETTLED,
                LocalDate.now(), null, "", null, null, null
        );

        assertEquals(1234L, op.amountInCents());
    }
}