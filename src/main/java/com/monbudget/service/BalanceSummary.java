package com.monbudget.service;

import java.math.BigDecimal;

/**
 * Synthèse des indicateurs clés (KPI) d'un livre de comptes.
 *
 * @param initialBalance    Solde de départ du livre
 * @param settledExpenses   Total des dépenses effectivement débitées
 * @param settledIncomes    Total des recettes effectivement encaissées
 * @param currentBalance    Solde effectif actuel (initial + recettes réglées - dépenses réglées)
 * @param plannedExpenses   Total des dépenses prévisionnelles à venir
 * @param plannedIncomes    Total des recettes prévisionnelles à venir
 * @param projectedBalance  Solde prévisionnel estimé (currentBalance + net à venir)
 */
public record BalanceSummary(
        BigDecimal initialBalance,
        BigDecimal settledExpenses,
        BigDecimal settledIncomes,
        BigDecimal currentBalance,
        BigDecimal plannedExpenses,
        BigDecimal plannedIncomes,
        BigDecimal projectedBalance
) {
    public static BalanceSummary empty(BigDecimal initialBalance) {
        BigDecimal zero = BigDecimal.ZERO;
        return new BalanceSummary(initialBalance, zero, zero, initialBalance, zero, zero, initialBalance);
    }

    /**
     * Calcule le solde net des flux à venir (recettes prévues - dépenses prévues).
     */
    public BigDecimal netPlanned() {
        return plannedIncomes.subtract(plannedExpenses);
    }
}