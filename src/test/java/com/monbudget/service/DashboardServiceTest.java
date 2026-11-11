package com.monbudget.service;

import com.monbudget.dao.CategoryDao;
import com.monbudget.dao.OperationDao;
import com.monbudget.model.Book;
import com.monbudget.model.Category;
import com.monbudget.model.Operation;
import com.monbudget.model.enums.OperationType;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DashboardServiceTest {

    private OperationDao operationDao;
    private BalanceService balanceService;
    private CategoryDao categoryDao;
    private DashboardService dashboardService;

    @BeforeAll
    static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Le toolkit est déjà initialisé si plusieurs tests tournent, on ignore l'erreur
        }
    }

    @BeforeEach
    void setUp() {
        operationDao = mock(OperationDao.class);
        balanceService = mock(BalanceService.class);
        categoryDao = mock(CategoryDao.class);
        dashboardService = new DashboardService(operationDao, balanceService, categoryDao);
    }

    @Test
    void testCreateLoadTaskPopulatesPayloadCorrectly() throws Exception {
        Book book = new Book(
                1L,
                "Personnel",
                null,
                "EUR",
                BigDecimal.ZERO,
                "#3498db",
                "wallet",
                false,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);
        Map<Long, String> categoryNamesMap = Collections.emptyMap();

        List<Operation> mockOperations = Collections.emptyList();
        BalanceSummary mockSummary = mock(BalanceSummary.class);
        XYChart.Series<String, Number> mockSeries = new XYChart.Series<>();
        List<PieChart.Data> mockPieData = Collections.emptyList();

        when(operationDao.findByBookAndDateRange(book.id(), start, end)).thenReturn(mockOperations);
        when(balanceService.computeSummary(book, start, end)).thenReturn(mockSummary);
        when(balanceService.buildBalanceHistorySeries(book, start, end)).thenReturn(mockSeries);
        when(balanceService.buildExpenseCategoryPieData(mockOperations, categoryNamesMap)).thenReturn(mockPieData);

        Task<DashboardPayload> task = dashboardService.createLoadTask(book, start, end, categoryNamesMap);

        task.run();
        DashboardPayload payload = task.get();

        assertNotNull(payload);
        assertEquals(mockOperations, payload.operations());
        assertEquals(mockSummary, payload.summary());
        assertEquals(mockSeries, payload.historySeries());
        assertEquals(mockPieData, payload.pieData());

        verify(operationDao).findByBookAndDateRange(book.id(), start, end);
        verify(balanceService).computeSummary(book, start, end);
        verify(balanceService).buildBalanceHistorySeries(book, start, end);
        verify(balanceService).buildExpenseCategoryPieData(mockOperations, categoryNamesMap);
    }

    @Test
    void testRefreshCategoryCache() {
        Category cat1 = new Category(1L, null, "Alimentation", OperationType.EXPENSE, "#000000", "icon", false);
        when(categoryDao.findAll()).thenReturn(List.of(cat1));

        dashboardService.refreshCategoryCache();

        Map<Long, String> cache = dashboardService.getCategoryNamesMap();
        assertEquals(1, cache.size());
        assertEquals("Alimentation", cache.get(1L));
        verify(categoryDao).findAll();
    }
}