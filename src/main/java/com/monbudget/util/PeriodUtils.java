package com.monbudget.util;

import java.time.LocalDate;

public class PeriodUtils {

    public static LocalDate[] getPeriodForPreset(String preset) {
        if (preset == null) {
            return getDefaultPeriod();
        }

        LocalDate now = LocalDate.now();
        return switch (preset) {
            case "Mois en cours" -> new LocalDate[]{
                    now.withDayOfMonth(1),
                    now.withDayOfMonth(now.lengthOfMonth())
            };
            case "Année en cours" -> new LocalDate[]{
                    now.withDayOfYear(1),
                    now.withDayOfYear(now.lengthOfYear())
            };
            default -> new LocalDate[]{
                    now.withDayOfMonth(1),
                    now.withDayOfMonth(now.lengthOfMonth())
            };
        };
    }

    private static LocalDate[] getDefaultPeriod() {
        LocalDate now = LocalDate.now();
        return new LocalDate[]{
                now.withDayOfMonth(1),
                now.withDayOfMonth(now.lengthOfMonth())
        };
    }
}