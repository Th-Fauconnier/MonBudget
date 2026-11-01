package com.monbudget.service;

import com.monbudget.dao.OperationDao;
import com.monbudget.model.Operation;
import com.monbudget.model.enums.OperationStatus;
import com.monbudget.model.enums.OperationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

class OperationServiceTest {

    private OperationDao operationDaoMock;
    private OperationService operationService;

    @BeforeEach
    void setUp() {
        operationDaoMock = Mockito.mock(OperationDao.class);
        operationService = new OperationService(operationDaoMock);
    }

    @Test
    @DisplayName("Devrait lever une NullPointerException si le DAO est null à l'instanciation")
    void testConstructorNullDao() {
        assertThrows(NullPointerException.class, () -> new OperationService(null));
    }

    @Test
    @DisplayName("Devrait lever une NullPointerException si l'opération passée est null")
    void testMarkAsSettledTodayNullOperation() {
        assertThrows(NullPointerException.class, () -> operationService.markAsSettledToday(null));
    }

    @Test
    @DisplayName("Devrait basculer l'opération au statut SETTLED avec la date du jour et appeler le DAO update")
    void testMarkAsSettledTodaySuccess() {
        // Opération initiale en attente (PLANNED)
        LocalDate initialDate = LocalDate.of(2026, 11, 1);
        Operation plannedOperation = new Operation(
                10L,
                1L,
                2L,
                null,
                null,
                "Courses",
                new BigDecimal("-45.50"),
                OperationType.EXPENSE,
                OperationStatus.PLANNED,
                initialDate,
                null, // settledAt initialement null
                "Notes de test",
                null,
                LocalDateTime.of(2026, 11, 1, 10, 0),
                LocalDateTime.of(2026, 11, 1, 10, 0)
        );

        operationService.markAsSettledToday(plannedOperation);

        // On capture l'objet passé au DAO pour vérifier ses modifications
        ArgumentCaptor<Operation> operationCaptor = ArgumentCaptor.forClass(Operation.class);
        verify(operationDaoMock).update(operationCaptor.capture());

        Operation updatedOperation = operationCaptor.getValue();

        assertNotNull(updatedOperation);
        assertEquals(10L, updatedOperation.id());
        assertEquals(OperationStatus.SETTLED, updatedOperation.status());
        assertEquals(LocalDate.now(), updatedOperation.settledAt()); // Doit être la date du jour
        assertEquals(initialDate, updatedOperation.operationDate()); // La date d'opération ne change pas
        assertEquals("Courses", updatedOperation.label());
        assertEquals(new BigDecimal("-45.50"), updatedOperation.amount());
        assertNotNull(updatedOperation.updatedAt());
    }
}