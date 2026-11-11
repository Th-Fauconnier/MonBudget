package com.monbudget.service;

import com.monbudget.model.Operation;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import java.util.List;

public record DashboardPayload(
        List<Operation> operations,
        BalanceSummary summary,
        XYChart.Series<String, Number> historySeries,
        List<PieChart.Data> pieData
) {}