package com.monbudget.util;

import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;

public final class PeriodUtils {

    private PeriodUtils() {
        // Constructeur privé pour empêcher l'instanciation
    }

    private static boolean isUpdatingDates = false;

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

    /**
     * Configure les écouteurs sur le ComboBox de présélection et les DatePickers
     * pour assurer la synchronisation et éviter les incohérences de dates.
     */
    public static void setupDateBindings(ComboBox<String> periodPresetCombo, DatePicker startDatePicker, DatePicker endDatePicker) {
        if (periodPresetCombo.getItems().isEmpty()) {
            periodPresetCombo.getItems().addAll(
                    "Mois en cours",
                    "Année en cours"
            );
        }

        periodPresetCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || isUpdatingDates) return;
            LocalDate[] dates = getPeriodForPreset(newVal);
            isUpdatingDates = true;
            startDatePicker.setValue(dates[0]);
            endDatePicker.setValue(dates[1]);
            isUpdatingDates = false;
        });

        startDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (isUpdatingDates) return;
            if (newVal != null && endDatePicker.getValue() != null && newVal.isAfter(endDatePicker.getValue())) {
                isUpdatingDates = true;
                endDatePicker.setValue(newVal);
                isUpdatingDates = false;
            }
        });

        endDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (isUpdatingDates) return;
            if (newVal != null && startDatePicker.getValue() != null && newVal.isBefore(startDatePicker.getValue())) {
                isUpdatingDates = true;
                startDatePicker.setValue(newVal);
                isUpdatingDates = false;
            }
        });
    }

    /**
     * Décale la période active (mois ou année précédente/suivante).
     */
    public static void shiftPeriod(long amount, TemporalUnit unit, DatePicker startDatePicker, DatePicker endDatePicker) {
        LocalDate current = startDatePicker.getValue() != null ? startDatePicker.getValue() : LocalDate.now();
        LocalDate target = current.plus(amount, unit);

        isUpdatingDates = true;
        if (unit == ChronoUnit.MONTHS) {
            startDatePicker.setValue(target.withDayOfMonth(1));
            endDatePicker.setValue(target.withDayOfMonth(target.lengthOfMonth()));
        } else {
            startDatePicker.setValue(target.withDayOfYear(1));
            endDatePicker.setValue(target.withDayOfYear(target.lengthOfYear()));
        }
        isUpdatingDates = false;
    }
}