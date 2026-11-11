package com.monbudget.util;

import javafx.application.Platform;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

class PeriodUtilsTest {

    private ComboBox<String> presetCombo;
    private DatePicker startDatePicker;
    private DatePicker endDatePicker;

    @BeforeAll
    static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Toolkit déjà initialisé
        }
    }

    @BeforeEach
    void setUp() {
        presetCombo = new ComboBox<>();
        startDatePicker = new DatePicker();
        endDatePicker = new DatePicker();
        PeriodUtils.setupDateBindings(presetCombo, startDatePicker, endDatePicker);
    }

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

    @Test
    void testPresetSelectionUpdatesDates() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            presetCombo.setValue("Mois en cours");
            latch.countDown();
        });
        latch.await(); // Attend que le thread JavaFX ait fini d'exécuter la modification

        LocalDate now = LocalDate.now();
        assertEquals(now.withDayOfMonth(1), startDatePicker.getValue());
        assertEquals(now.withDayOfMonth(now.lengthOfMonth()), endDatePicker.getValue());
    }

    @Test
    void testShiftPeriodMonths() {
        LocalDate initialStart = LocalDate.of(2026, 1, 1);
        startDatePicker.setValue(initialStart);
        endDatePicker.setValue(LocalDate.of(2026, 1, 31));

        PeriodUtils.shiftPeriod(1, ChronoUnit.MONTHS, startDatePicker, endDatePicker);

        assertEquals(LocalDate.of(2026, 2, 1), startDatePicker.getValue());
        assertEquals(LocalDate.of(2026, 2, 28), endDatePicker.getValue()); // 2026 n'est pas bissextile
    }
}