package com.monbudget.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MoneyHelperTest {

    @ParameterizedTest
    @CsvSource({
            "1540, 15.40",
            "100, 1.00",
            "0, 0.00",
            "123456, 1234.56"
    })
    @DisplayName("Devrait convertir des centimes long en BigDecimal correct")
    void testFromCents(long cents, String expectedAmount) {
        BigDecimal result = MoneyHelper.fromCents(cents);
        assertEquals(new BigDecimal(expectedAmount), result);
    }

    @ParameterizedTest
    @CsvSource({
            "15.40, 1540",
            "1.00, 100",
            "0.00, 0",
            "1234.56, 123456"
    })
    @DisplayName("Devrait convertir un BigDecimal en centimes long correct")
    void testToCents(String amountStr, long expectedCents) {
        BigDecimal amount = new BigDecimal(amountStr);
        long result = MoneyHelper.toCents(amount);
        assertEquals(expectedCents, result);
    }

    @Test
    @DisplayName("Devrait retourner 0L si le BigDecimal est null pour toCents")
    void testToCentsWithNull() {
        assertEquals(0L, MoneyHelper.toCents(null));
    }
}