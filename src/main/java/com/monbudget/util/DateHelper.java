package com.monbudget.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DateHelper {

    private static final DateTimeFormatter SQL_DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateHelper() {
        // Constructeur privé pour empêcher l'instanciation
    }

    // --- LocalDate ---
    public static LocalDate parseLocalDate(String value) {
        return value != null && !value.isBlank() ? LocalDate.parse(value) : null;
    }

    public static String toSqlDate(LocalDate date) {
        return date != null ? date.toString() : null;
    }

    // --- LocalDateTime ---
    public static LocalDateTime parseLocalDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.contains(" ") ? value.replace(" ", "T") : value;
        return LocalDateTime.parse(normalized);
    }

    public static String toSqlDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        // Utilisation d'un formatter explicite pour inclure les secondes (:00)
        return dateTime.format(SQL_DATETIME_FORMATTER);
    }
}