package com.monbudget.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DateHelperTest {

    @Test
    @DisplayName("Devrait parser une chaîne valide en LocalDate")
    void testParseLocalDateValid() {
        LocalDate date = DateHelper.parseLocalDate("2026-10-18");
        assertThat(date).isEqualTo(LocalDate.of(2026, 10, 18));
    }

    @Test
    @DisplayName("Devrait retourner null lors du parsing d'une chaîne nulle ou vide pour LocalDate")
    void testParseLocalDateNullOrBlank() {
        assertThat(DateHelper.parseLocalDate(null)).isNull();
        assertThat(DateHelper.parseLocalDate("")).isNull();
        assertThat(DateHelper.parseLocalDate("   ")).isNull();
    }

    @Test
    @DisplayName("Devrait convertir une LocalDate en chaîne SQL")
    void testToSqlDate() {
        LocalDate date = LocalDate.of(2026, 10, 18);
        assertThat(DateHelper.toSqlDate(date)).isEqualTo("2026-10-18");
        assertThat(DateHelper.toSqlDate(null)).isNull();
    }

    @Test
    @DisplayName("Devrait parser une chaîne LocalDateTime au format ISO ou format SQLite avec espace")
    void testParseLocalDateTimeValid() {
        // Format ISO standard
        LocalDateTime dtIso = DateHelper.parseLocalDateTime("2026-10-18T14:30:00");
        assertThat(dtIso).isEqualTo(LocalDateTime.of(2026, 10, 18, 14, 30, 0));

        // Format SQLite DATETIME (avec espace)
        LocalDateTime dtSql = DateHelper.parseLocalDateTime("2026-10-18 14:30:00");
        assertThat(dtSql).isEqualTo(LocalDateTime.of(2026, 10, 18, 14, 30, 0));
    }

    @Test
    @DisplayName("Devrait retourner null lors du parsing d'une chaîne nulle ou vide pour LocalDateTime")
    void testParseLocalDateTimeNullOrBlank() {
        assertThat(DateHelper.parseLocalDateTime(null)).isNull();
        assertThat(DateHelper.parseLocalDateTime("")).isNull();
        assertThat(DateHelper.parseLocalDateTime("   ")).isNull();
    }

    @Test
    @DisplayName("Devrait convertir une LocalDateTime en chaîne SQL au format SQLite")
    void testToSqlDateTime() {
        LocalDateTime dt = LocalDateTime.of(2026, 10, 18, 14, 30, 0);
        assertThat(DateHelper.toSqlDateTime(dt)).isEqualTo("2026-10-18 14:30:00");
        assertThat(DateHelper.toSqlDateTime(null)).isNull();
    }
}