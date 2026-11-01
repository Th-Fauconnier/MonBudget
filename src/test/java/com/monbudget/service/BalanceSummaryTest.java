package com.monbudget.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class BalanceSummaryTest {

    @Test
    @DisplayName("Devrait créer un résumé vide avec le solde initial et des zéros pour les flux")
    void testEmptySummary() {
        BigDecimal initial = new BigDecimal("1500.00");
        BalanceSummary summary = BalanceSummary.empty(initial);

        assertNotNull(summary);
        assertEquals(initial, summary.initialBalance());
        assertEquals(BigDecimal.ZERO, summary.settledExpenses());
        assertEquals(BigDecimal.ZERO, summary.settledIncomes());
        assertEquals(initial, summary.currentBalance());
        assertEquals(BigDecimal.ZERO, summary.plannedExpenses());
        assertEquals(BigDecimal.ZERO, summary.plannedIncomes());
        assertEquals(initial, summary.projectedBalance());
    }

    @Test
    @DisplayName("Devrait calculer un net prévisionnel positif lorsque les revenus prévus dépassent les dépenses prévues")
    void testNetPlannedPositive() {
        BalanceSummary summary = new BalanceSummary(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("150.00"), // plannedExpenses
                new BigDecimal("400.00"), // plannedIncomes
                BigDecimal.ZERO
        );

        BigDecimal net = summary.netPlanned();
        assertEquals(new BigDecimal("250.00"), net);
    }

    @Test
    @DisplayName("Devrait calculer un net prévisionnel négatif lorsque les dépenses prévues dépassent les revenus prévus")
    void testNetPlannedNegative() {
        BalanceSummary summary = new BalanceSummary(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("600.00"), // plannedExpenses
                new BigDecimal("200.00"), // plannedIncomes
                BigDecimal.ZERO
        );

        BigDecimal net = summary.netPlanned();
        assertEquals(new BigDecimal("-400.00"), net);
    }

    @Test
    @DisplayName("Devrait retourner zéro si les revenus et dépenses prévisionnels s'équilibrent")
    void testNetPlannedZero() {
        BalanceSummary summary = new BalanceSummary(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("250.00"), // plannedExpenses
                new BigDecimal("250.00"), // plannedIncomes
                BigDecimal.ZERO
        );

        BigDecimal net = summary.netPlanned();
        assertEquals(0, BigDecimal.ZERO.compareTo(net));
    }
}