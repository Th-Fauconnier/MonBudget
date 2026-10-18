package com.monbudget.model;

import com.monbudget.util.MoneyHelper;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Book(
        Long id,
        String name,
        String description,
        String currency,
        BigDecimal initialBalance,
        String colorHex,
        String iconName,
        boolean isArchived,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    // Constructeur de commodité pour créer un nouveau livre
    public static Book create(String name, String description, BigDecimal initialBalance, String colorHex) {
        return new Book(
                null,
                name,
                description,
                "EUR",
                initialBalance != null ? initialBalance : BigDecimal.ZERO,
                colorHex != null ? colorHex : "#3498db",
                "wallet",
                false,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    public long initialBalanceInCents() {
        return MoneyHelper.toCents(initialBalance);
    }
}