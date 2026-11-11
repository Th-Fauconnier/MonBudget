package com.monbudget.service;

import com.monbudget.dao.CategoryDao;
import com.monbudget.dao.OperationDao;
import com.monbudget.model.Book;
import com.monbudget.model.Category;
import com.monbudget.model.Operation;
import javafx.concurrent.Task;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardService {

    private final CategoryDao categoryDao;
    private final OperationDao operationDao;
    private final BalanceService balanceService;
    private final Map<Long, String> categoryNamesMap = new HashMap<>();

    public DashboardService( OperationDao operationDao, BalanceService balanceService, CategoryDao categoryDao ) {
        this.operationDao = operationDao;
        this.balanceService = balanceService;
        this.categoryDao = categoryDao;
    }

    public void refreshCategoryCache() {
        categoryNamesMap.clear();
        for (Category cat : categoryDao.findAll()) {
            categoryNamesMap.put(cat.id(), cat.name());
        }
    }

    public Map<Long, String> getCategoryNamesMap() {
        return categoryNamesMap;
    }

    public Task<DashboardPayload> createLoadTask(Book book, LocalDate start, LocalDate end, Map<Long, String> categoryNamesMap) {
        return new Task<>() {
            @Override
            protected DashboardPayload call() {
                List<Operation> operations = operationDao.findByBookAndDateRange(book.id(), start, end);
                BalanceSummary summary = balanceService.computeSummary(book, start, end);
                XYChart.Series<String, Number> series = balanceService.buildBalanceHistorySeries(book, start, end);
                List<PieChart.Data> pieData = balanceService.buildExpenseCategoryPieData(operations, categoryNamesMap);

                return new DashboardPayload(operations, summary, series, pieData);
            }
        };
    }
}