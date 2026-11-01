package com.monbudget.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SimulationServiceTest {

    private SimulationService simulationService;

    @BeforeEach
    void setUp() {
        simulationService = new SimulationService();
    }

    @Test
    @DisplayName("Devrait calculer correctement le solde après un achat ponctuel")
    void testSimulateOneTimePurchaseSuccess() {
        BigDecimal current = new BigDecimal("1500.00");
        BigDecimal purchase = new BigDecimal("350.50");

        BigDecimal result = simulationService.simulateOneTimePurchase(current, purchase);

        assertEquals(new BigDecimal("1149.50"), result);
    }

    @Test
    @DisplayName("Devrait gérer la valeur absolue du montant d'achat même s'il est négatif")
    void testSimulateOneTimePurchaseWithNegativeAmount() {
        BigDecimal current = new BigDecimal("1000.00");
        BigDecimal purchase = new BigDecimal("-200.00");

        BigDecimal result = simulationService.simulateOneTimePurchase(current, purchase);

        assertEquals(new BigDecimal("800.00"), result);
    }

    @Test
    @DisplayName("Devrait lever une NullPointerException si les arguments sont null pour l'achat ponctuel")
    void testSimulateOneTimePurchaseNullArgs() {
        assertThrows(NullPointerException.class, () -> simulationService.simulateOneTimePurchase(null, BigDecimal.TEN));
        assertThrows(NullPointerException.class, () -> simulationService.simulateOneTimePurchase(BigDecimal.TEN, null));
    }

    @Test
    @DisplayName("Devrait générer un plan d'épargne mensuel correct avec le nombre de mois et les soldes progressifs")
    void testSimulateMonthlySavingsPlanSuccess() {
        BigDecimal startingBalance = new BigDecimal("1000.00");
        BigDecimal monthlySavings = new BigDecimal("150.00");
        int monthsCount = 3;
        LocalDate startDate = LocalDate.of(2026, 11, 1);

        List<SimulationService.ProjectionPoint> points = simulationService.simulateMonthlySavingsPlan(
                startingBalance, monthlySavings, monthsCount, startDate
        );

        assertNotNull(points);
        assertEquals(3, points.size());

        // Mois 1 : 1000 + 150 = 1150 (01/12/2026)
        assertEquals(LocalDate.of(2026, 12, 1), points.get(0).date());
        assertEquals(new BigDecimal("1150.00"), points.get(0).simulatedBalance());
        assertEquals("Échéance 1/3 (+150.00 €)", points.get(0).description());

        // Mois 2 : 1150 + 150 = 1300 (01/01/2027)
        assertEquals(LocalDate.of(2027, 1, 1), points.get(1).date());
        assertEquals(new BigDecimal("1300.00"), points.get(1).simulatedBalance());

        // Mois 3 : 1300 + 150 = 1450 (01/02/2027)
        assertEquals(LocalDate.of(2027, 2, 1), points.get(2).date());
        assertEquals(new BigDecimal("1450.00"), points.get(2).simulatedBalance());
    }

    @Test
    @DisplayName("Devrait lever une IllegalArgumentException si le nombre de mois est inférieur ou égal à 0")
    void testSimulateMonthlySavingsPlanInvalidMonths() {
        LocalDate startDate = LocalDate.now();
        assertThrows(IllegalArgumentException.class, () ->
                simulationService.simulateMonthlySavingsPlan(BigDecimal.TEN, BigDecimal.TEN, 0, startDate)
        );
        assertThrows(IllegalArgumentException.class, () ->
                simulationService.simulateMonthlySavingsPlan(BigDecimal.TEN, BigDecimal.TEN, -1, startDate)
        );
    }
}