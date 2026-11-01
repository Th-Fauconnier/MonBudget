package com.monbudget.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SimulationScenarioTest {

    @Test
    @DisplayName("Devrait convertir correctement le montant cible en centimes via targetAmountInCents")
    void testTargetAmountInCents() {
        SimulationScenario scenario = new SimulationScenario(
                1L,
                "Achat Maison",
                ",Apport personnel",
                LocalDate.of(2030, 6, 15),
                new BigDecimal("25000.50"),
                LocalDateTime.now()
        );

        assertEquals(2500050L, scenario.targetAmountInCents());
    }

    @Test
    @DisplayName("Devrait conserver et restituer correctement toutes les propriétés du record")
    void testSimulationScenarioProperties() {
        LocalDate targetDate = LocalDate.of(2028, 12, 31);
        LocalDateTime createdAt = LocalDateTime.of(2026, 11, 1, 12, 0);

        SimulationScenario scenario = new SimulationScenario(
                10L,
                "Voyage",
                "Vacances d'été",
                targetDate,
                new BigDecimal("3500.00"),
                createdAt
        );

        assertEquals(10L, scenario.id());
        assertEquals("Voyage", scenario.name());
        assertEquals("Vacances d'été", scenario.description());
        assertEquals(targetDate, scenario.targetDate());
        assertEquals(new BigDecimal("3500.00"), scenario.targetAmount());
        assertEquals(createdAt, scenario.createdAt());
    }
}