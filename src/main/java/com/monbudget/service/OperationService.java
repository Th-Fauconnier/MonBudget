package com.monbudget.service;

import com.monbudget.dao.OperationDao;
import com.monbudget.model.Operation;
import com.monbudget.model.enums.OperationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

public class OperationService {

    private final OperationDao operationDao;

    public OperationService(OperationDao operationDao) {
        this.operationDao = Objects.requireNonNull(operationDao, "OperationDao ne doit pas être null.");
    }

    /**
     * Action rapide de pointage : bascule une opération en 'SETTLED' avec la date du jour.
     */
    public void markAsSettledToday(Operation operation) {
        Objects.requireNonNull(operation, "L'opération ne peut pas être null.");

        Operation updated = new Operation(
                operation.id(),
                operation.bookId(),
                operation.categoryId(),
                operation.paymentMethodId(),
                operation.recurrenceRuleId(),
                operation.label(),
                operation.amount(),
                operation.operationType(),
                OperationStatus.SETTLED,
                operation.operationDate(),
                LocalDate.now(), // Date effective du pointage
                operation.notes(),
                operation.transferOperationId(),
                operation.createdAt(),
                LocalDateTime.now()
        );

        operationDao.update(updated);
    }
}