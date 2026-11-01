package com.monbudget.model;

import com.monbudget.model.enums.Frequency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class SimulationEntryTest {

    @Test
    @DisplayName("Devrait convertir correctement le montant en centimes via amountInCents")
    void testAmountInCents() {
        SimulationEntry entry = new SimulationEntry(
                1L,
                10L,
                "Loyer simulé",
                new BigDecimal("750.50"),
                LocalDate.of(2026, 12, 1),
                Frequency.MONTHLY
        );

        assertEquals(75050L, entry.amountInCents());
    }

    @Test
    @DisplayName("Devrait convertir un montant négatif en centimes négatifs")
    void testAmountInCentsNegative() {
        SimulationEntry entry = new SimulationEntry(
                2L,
                10L,
                "Achat exceptionnel",
                new BigDecimal("-120.00"),
                LocalDate.of(2026, 11, 15),
                Frequency.ONCE
        );

        assertEquals(-12000L, entry.amountInCents());
    }

    @Test
    @DisplayName("Devrait conserver et restituer correctement toutes les propriétés du record")
    void testSimulationEntryProperties() {
        LocalDate date = LocalDate.of(2026, 12, 31);
        SimulationEntry entry = new SimulationEntry(
                5L,
                2L,
                "Prime annuelle",
                new BigDecimal("1500.00"),
                date,
                Frequency.YEARLY
        );

        assertEquals(5L, entry.id());
        assertEquals(2L, entry.scenarioId());
        assertEquals("Prime annuelle", entry.label());
        assertEquals(new BigDecimal("1500.00"), entry.amount());
        assertEquals(date, entry.projectedDate());
        assertEquals(Frequency.YEARLY, entry.frequency());
    }
}