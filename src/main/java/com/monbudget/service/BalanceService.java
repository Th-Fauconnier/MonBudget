package com.monbudget.service;

import com.monbudget.dao.OperationDao;
import com.monbudget.model.Book;
import com.monbudget.model.Operation;
import com.monbudget.model.enums.OperationStatus;
import com.monbudget.model.enums.OperationType;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class BalanceService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM");
    private final OperationDao operationDao;

    public BalanceService(OperationDao operationDao) {
        this.operationDao = Objects.requireNonNull(operationDao, "OperationDao ne doit pas être null.");
    }

    /**
     * Calcule le résumé consolidé des indicateurs financiers d'un livre pour une période donnée.
     */
    public BalanceSummary computeSummary(Book book, LocalDate startDate, LocalDate endDate) {
        Objects.requireNonNull(book, "Le livre ne peut pas être null.");
        List<Operation> operations = operationDao.findByBookAndDateRange(book.id(), startDate, endDate);

        BigDecimal initialBal = book.initialBalance() != null ? book.initialBalance() : BigDecimal.ZERO;

        BigDecimal settledExpenses = BigDecimal.ZERO;
        BigDecimal settledIncomes = BigDecimal.ZERO;
        BigDecimal plannedExpenses = BigDecimal.ZERO;
        BigDecimal plannedIncomes = BigDecimal.ZERO;

        for (Operation op : operations) {
            BigDecimal amount = op.amount().abs();

            if (op.status() == OperationStatus.SETTLED) {
                if (op.operationType() == OperationType.EXPENSE) {
                    settledExpenses = settledExpenses.add(amount);
                } else if (op.operationType() == OperationType.INCOME) {
                    settledIncomes = settledIncomes.add(amount);
                }
            } else if (op.status() == OperationStatus.PLANNED) {
                if (op.operationType() == OperationType.EXPENSE) {
                    plannedExpenses = plannedExpenses.add(amount);
                } else if (op.operationType() == OperationType.INCOME) {
                    plannedIncomes = plannedIncomes.add(amount);
                }
            }
        }

        BigDecimal currentBalance = initialBal.add(settledIncomes).subtract(settledExpenses);
        BigDecimal projectedBalance = currentBalance.add(plannedIncomes).subtract(plannedExpenses);

        return new BalanceSummary(
                initialBal,
                settledExpenses,
                settledIncomes,
                currentBalance,
                plannedExpenses,
                plannedIncomes,
                projectedBalance
        );
    }

    /**
     * Construit les points de la série pour un LineChart illustrant l'évolution cumulée du solde.
     */
    public XYChart.Series<String, Number> buildBalanceHistorySeries(Book book, LocalDate startDate, LocalDate endDate) {
        // Encapsulation dans un ArrayList pour garantir qu'elle est modifiable (évite les UnsupportedOperationException)
        List<Operation> operations = new ArrayList<>(operationDao.findByBookAndDateRange(book.id(), startDate, endDate));
        operations.sort(Comparator.comparing(Operation::operationDate));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Trajectoire du solde");

        BigDecimal runningBalance = book.initialBalance() != null ? book.initialBalance() : BigDecimal.ZERO;

        // Regroupement par date pour consolider les mouvements journaliers
        Map<LocalDate, BigDecimal> dailyDelta = new TreeMap<>();
        for (Operation op : operations) {
            BigDecimal delta = op.operationType() == OperationType.EXPENSE ? op.amount().abs().negate() : op.amount().abs();
            dailyDelta.merge(op.operationDate(), delta, BigDecimal::add);
        }

        for (Map.Entry<LocalDate, BigDecimal> entry : dailyDelta.entrySet()) {
            runningBalance = runningBalance.add(entry.getValue());
            String formattedDate = entry.getKey().format(DATE_FORMATTER);
            series.getData().add(new XYChart.Data<>(formattedDate, runningBalance.doubleValue()));
        }

        return series;
    }

    /**
     * Prépare les données pour le PieChart en agrégeant les dépenses par libellé de catégorie.
     * Applique la règle UI : regroupe les tranches < 2 % sous « Autres ».
     */
    public List<PieChart.Data> buildExpenseCategoryPieData(List<Operation> operations, Map<Long, String> categoryNames) {
        Map<String, BigDecimal> categoryTotals = new HashMap<>();
        BigDecimal totalExpenses = BigDecimal.ZERO;

        for (Operation op : operations) {
            if (op.operationType() == OperationType.EXPENSE) {
                BigDecimal amt = op.amount().abs();
                String catName = (op.categoryId() != null && categoryNames.containsKey(op.categoryId()))
                        ? categoryNames.get(op.categoryId())
                        : "Sans catégorie";

                categoryTotals.merge(catName, amt, BigDecimal::add);
                totalExpenses = totalExpenses.add(amt);
            }
        }

        if (totalExpenses.compareTo(BigDecimal.ZERO) == 0) {
            return Collections.emptyList();
        }

        List<PieChart.Data> result = new ArrayList<>();
        BigDecimal othersTotal = BigDecimal.ZERO;
        BigDecimal twoPercentThreshold = totalExpenses.multiply(new BigDecimal("0.02"));

        for (Map.Entry<String, BigDecimal> entry : categoryTotals.entrySet()) {
            if (entry.getValue().compareTo(twoPercentThreshold) < 0) {
                othersTotal = othersTotal.add(entry.getValue());
            } else {
                result.add(new PieChart.Data(entry.getKey(), entry.getValue().doubleValue()));
            }
        }

        if (othersTotal.compareTo(BigDecimal.ZERO) > 0) {
            result.add(new PieChart.Data("Autres (< 2%)", othersTotal.doubleValue()));
        }

        return result;
    }
}