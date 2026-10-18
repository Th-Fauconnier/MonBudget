package com.monbudget.model;

import com.monbudget.util.MoneyHelper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record SimulationScenario(
        Long id,
        String name,
        String description,
        LocalDate targetDate,
        BigDecimal targetAmount,
        LocalDateTime createdAt
) {
    public long targetAmountInCents() {
        return MoneyHelper.toCents(targetAmount);
    }
}