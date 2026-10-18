package com.monbudget.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SimulationService {

    /**
     * Point de projection ponctuel.
     */
    public record ProjectionPoint(LocalDate date, BigDecimal simulatedBalance, String description) {}

    /**
     * Projette l'impact d'une dépense ponctuelle majeure sur le solde actuel.
     *
     * @param currentBalance Solde de départ
     * @param purchaseAmount Montant de l'achat envisagé
     * @return Solde résultant
     */
    public BigDecimal simulateOneTimePurchase(BigDecimal currentBalance, BigDecimal purchaseAmount) {
        Objects.requireNonNull(currentBalance);
        Objects.requireNonNull(purchaseAmount);
        return currentBalance.subtract(purchaseAmount.abs()).setScale(2, RoundingMode.HALF_EVEN);
    }

    /**
     * Simule un plan d'épargne mensuel récurent sur un nombre de mois donné.
     *
     * @param startingBalance  Solde initial
     * @param monthlySavings   Montant épargné chaque mois
     * @param monthsCount      Nombre d'échéances
     * @param startDate        Date de départ de la projection
     * @return Liste ordonnée des jalons mensuels
     */
    public List<ProjectionPoint> simulateMonthlySavingsPlan(
            BigDecimal startingBalance,
            BigDecimal monthlySavings,
            int monthsCount,
            LocalDate startDate
    ) {
        if (monthsCount <= 0) {
            throw new IllegalArgumentException("Le nombre de mois doit être supérieur à 0.");
        }

        List<ProjectionPoint> points = new ArrayList<>();
        BigDecimal current = startingBalance;
        LocalDate cursorDate = startDate;

        for (int i = 1; i <= monthsCount; i++) {
            cursorDate = cursorDate.plusMonths(1);
            current = current.add(monthlySavings.abs());
            points.add(new ProjectionPoint(
                    cursorDate,
                    current.setScale(2, RoundingMode.HALF_EVEN),
                    String.format("Échéance %d/%d (+%s €)", i, monthsCount, monthlySavings.toPlainString())
            ));
        }

        return points;
    }
}