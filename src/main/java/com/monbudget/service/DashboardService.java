package com.monbudget.service;

import com.monbudget.dao.OperationDao;
import com.monbudget.model.Book;
import com.monbudget.model.Operation;
import javafx.concurrent.Task;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class DashboardService {

    private final OperationDao operationDao;
    private final BalanceService balanceService;

    public DashboardService(OperationDao operationDao, BalanceService balanceService) {
        this.operationDao = operationDao;
        this.balanceService = balanceService;
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