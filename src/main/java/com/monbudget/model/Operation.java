package com.monbudget.model;

import com.monbudget.model.enums.OperationStatus;
import com.monbudget.model.enums.OperationType;
import com.monbudget.util.MoneyHelper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record Operation(
        Long id,
        Long bookId,
        Long categoryId,
        Long paymentMethodId,
        Long recurrenceRuleId,
        String label,
        BigDecimal amount,
        OperationType operationType,
        OperationStatus status,
        LocalDate operationDate,
        LocalDate settledAt,
        String notes,
        Long transferOperationId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public boolean isSettled() {
        return status == OperationStatus.SETTLED;
    }

    public boolean isPlanned() {
        return status == OperationStatus.PLANNED;
    }

    /** Permet de basculer en un clic l'opération en statut effectif (SETTLED) */
    public Operation markAsSettled(LocalDate settlementDate) {
        return new Operation(
                id, bookId, categoryId, paymentMethodId, recurrenceRuleId,
                label, amount, operationType, OperationStatus.SETTLED,
                operationDate, settlementDate != null ? settlementDate : LocalDate.now(),
                notes, transferOperationId, createdAt, LocalDateTime.now()
        );
    }

    public long amountInCents() {
        return MoneyHelper.toCents(amount);
    }
}