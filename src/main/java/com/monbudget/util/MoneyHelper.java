package com.monbudget.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyHelper {

    private MoneyHelper() {}

    /** Convertit des centimes SQL (long) en BigDecimal avec 2 décimales. Ex: 1540L -> 15.40 */
    public static BigDecimal fromCents(long cents) {
        return BigDecimal.valueOf(cents).movePointLeft(2).setScale(2, RoundingMode.HALF_EVEN);
    }

    /** Convertit un BigDecimal en centimes SQL (long). Ex: 15.40 -> 1540L */
    public static long toCents(BigDecimal amount) {
        if (amount == null) return 0L;
        return amount.setScale(2, RoundingMode.HALF_EVEN).movePointRight(2).longValueExact();
    }
}