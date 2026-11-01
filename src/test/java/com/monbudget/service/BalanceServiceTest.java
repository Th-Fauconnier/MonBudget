package com.monbudget.service;

import com.monbudget.dao.OperationDao;
import com.monbudget.model.Book;
import com.monbudget.model.Operation;
import com.monbudget.model.enums.OperationStatus;
import com.monbudget.model.enums.OperationType;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class BalanceServiceTest {

    private OperationDao operationDaoMock;
    private BalanceService balanceService;
    private Book testBook;
    private LocalDate startDate;
    private LocalDate endDate;

    @BeforeEach
    void setUp() {
        operationDaoMock = Mockito.mock(OperationDao.class);
        balanceService = new BalanceService(operationDaoMock);

        testBook = new Book(
                1L,
                "Compte Courant",
                "Test book",
                "EUR",
                new BigDecimal("1000.00"), // Solde initial
                "#3498db",
                "wallet",
                false,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        startDate = LocalDate.of(2026, 11, 1);
        endDate = LocalDate.of(2026, 11, 30);
    }

    @Test
    @DisplayName("Devrait calculer correctement le résumé financier avec des opérations variées")
    void testComputeSummary() {
        List<Operation> mockOperations = List.of(
                // Réalisé - Dépense : -150.00
                createOp(1L, 101L, new BigDecimal("-150.00"), OperationType.EXPENSE, OperationStatus.SETTLED, LocalDate.of(2026, 11, 5)),
                // Réalisé - Revenu : +500.00
                createOp(2L, null, new BigDecimal("500.00"), OperationType.INCOME, OperationStatus.SETTLED, LocalDate.of(2026, 11, 10)),
                // Prévu - Dépense : -50.00
                createOp(3L, 102L, new BigDecimal("-50.00"), OperationType.EXPENSE, OperationStatus.PLANNED, LocalDate.of(2026, 11, 15)),
                // Prévu - Revenu : +200.00
                createOp(4L, null, new BigDecimal("200.00"), OperationType.INCOME, OperationStatus.PLANNED, LocalDate.of(2026, 11, 20))
        );

        when(operationDaoMock.findByBookAndDateRange(eq(1L), eq(startDate), eq(endDate)))
                .thenReturn(mockOperations);

        BalanceSummary summary = balanceService.computeSummary(testBook, startDate, endDate);

        assertNotNull(summary);
        assertEquals(new BigDecimal("1000.00"), summary.initialBalance());
        assertEquals(new BigDecimal("150.00"), summary.settledExpenses());
        assertEquals(new BigDecimal("500.00"), summary.settledIncomes());
        // Current = 1000 + 500 - 150 = 1350
        assertEquals(new BigDecimal("1350.00"), summary.currentBalance());
        assertEquals(new BigDecimal("50.00"), summary.plannedExpenses());
        assertEquals(new BigDecimal("200.00"), summary.plannedIncomes());
        // Projected = 1350 + 200 - 50 = 1500
        assertEquals(new BigDecimal("1500.00"), summary.projectedBalance());
    }

    @Test
    @DisplayName("Devrait lever une exception si le livre est null dans computeSummary")
    void testComputeSummaryNullBook() {
        assertThrows(NullPointerException.class, () -> {
            balanceService.computeSummary(null, startDate, endDate);
        });
    }

    @Test
    @DisplayName("Devrait construire la série d'historique de solde par ordre chronologique")
    void testBuildBalanceHistorySeries() {
        List<Operation> mockOperations = List.of(
                createOp(1L, null, new BigDecimal("-100.00"), OperationType.EXPENSE, OperationStatus.SETTLED, LocalDate.of(2026, 11, 10)),
                createOp(2L, null, new BigDecimal("300.00"), OperationType.INCOME, OperationStatus.SETTLED, LocalDate.of(2026, 11, 5))
        );

        when(operationDaoMock.findByBookAndDateRange(eq(1L), eq(startDate), eq(endDate)))
                .thenReturn(mockOperations);

        XYChart.Series<String, Number> series = balanceService.buildBalanceHistorySeries(testBook, startDate, endDate);

        assertNotNull(series);
        assertEquals("Trajectoire du solde", series.getName());
        assertEquals(2, series.getData().size());

        // Premier point (05/11) : Initial (1000) + 300 = 1300
        assertEquals("05/11", series.getData().get(0).getXValue());
        assertEquals(1300.0, series.getData().get(0).getYValue());

        // Second point (10/11) : 1300 - 100 = 1200
        assertEquals("10/11", series.getData().get(1).getXValue());
        assertEquals(1200.0, series.getData().get(1).getYValue());
    }

    @Test
    @DisplayName("Devrait retourner une liste vide si aucune dépense pour le PieChart")
    void testBuildExpenseCategoryPieDataEmpty() {
        List<PieChart.Data> pieData = balanceService.buildExpenseCategoryPieData(Collections.emptyList(), Collections.emptyMap());
        assertTrue(pieData.isEmpty());
    }

    @Test
    @DisplayName("Devrait grouper les dépenses de moins de 2% dans 'Autres' pour le PieChart")
    void testBuildExpenseCategoryPieDataWithThreshold() {
        // Total dépenses = 1000. Seuil de 2% = 20.
        // Cat 1 : 900 (90%) -> Conservée
        // Cat 2 : 85 (8.5%) -> Conservée
        // Cat 3 : 15 (1.5%) -> Inférieure à 2%, doit aller dans "Autres"
        List<Operation> operations = List.of(
                createOp(1L, 1L, new BigDecimal("-900.00"), OperationType.EXPENSE, OperationStatus.SETTLED, startDate),
                createOp(2L, 2L, new BigDecimal("-85.00"), OperationType.EXPENSE, OperationStatus.SETTLED, startDate),
                createOp(3L, 3L, new BigDecimal("-15.00"), OperationType.EXPENSE, OperationStatus.SETTLED, startDate)
        );

        Map<Long, String> categoryNames = Map.of(
                1L, "Logement",
                2L, "Alimentation",
                3L, "Loisirs"
        );

        List<PieChart.Data> result = balanceService.buildExpenseCategoryPieData(operations, categoryNames);

        // Doit contenir Logement, Alimentation et Autres (< 2%)
        assertEquals(3, result.size());

        boolean foundOthers = false;
        for (PieChart.Data data : result) {
            if (data.getName().equals("Autres (< 2%)")) {
                foundOthers = true;
                assertEquals(15.0, data.getPieValue());
            }
        }
        assertTrue(foundOthers, "La catégorie 'Autres (< 2%)' doit être présente");
    }

    // Méthode utilitaire pour simplifier la création d'objets Operation de test
    private Operation createOp(Long id, Long categoryId, BigDecimal amount, OperationType type, OperationStatus status, LocalDate date) {
        return new Operation(
                id,
                1L,
                categoryId,
                null,
                null,
                "Opération test",
                amount,
                type,
                status,
                date,
                status == OperationStatus.SETTLED ? date : null,
                "Notes",
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}