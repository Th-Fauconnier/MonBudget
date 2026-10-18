package com.monbudget.model;

import com.monbudget.model.enums.Frequency;
import com.monbudget.util.MoneyHelper;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SimulationEntry(
        Long id,
        Long scenarioId,
        String label,
        BigDecimal amount,
        LocalDate projectedDate,
        Frequency frequency
) {
    public long amountInCents() {
        return MoneyHelper.toCents(amount);
    }
}