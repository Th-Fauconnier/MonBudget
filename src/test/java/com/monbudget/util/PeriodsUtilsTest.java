package com.monbudget.util;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PeriodUtilsTest {

    @Test
    void testCurrentMonth() {
        LocalDate[] period = PeriodUtils.getPeriodForPreset("Mois en cours");
        LocalDate now = LocalDate.now();

        assertEquals(now.withDayOfMonth(1), period[0]);
        assertEquals(now.withDayOfMonth(now.lengthOfMonth()), period[1]);
    }

    @Test
    void testCurrentYear() {
        LocalDate[] period = PeriodUtils.getPeriodForPreset("Année en cours");
        LocalDate now = LocalDate.now();

        assertEquals(now.withDayOfYear(1), period[0]);
        assertEquals(now.withDayOfYear(now.lengthOfYear()), period[1]);
    }

    @Test
    void testDefaultFallbackForNullOrUnknown() {
        LocalDate now = LocalDate.now();

        LocalDate[] periodNull = PeriodUtils.getPeriodForPreset(null);
        assertEquals(now.withDayOfMonth(1), periodNull[0]);
        assertEquals(now.withDayOfMonth(now.lengthOfMonth()), periodNull[1]);

        LocalDate[] periodUnknown = PeriodUtils.getPeriodForPreset("Inconnu");
        assertEquals(now.withDayOfMonth(1), periodUnknown[0]);
        assertEquals(now.withDayOfMonth(now.lengthOfMonth()), periodUnknown[1]);
    }
}