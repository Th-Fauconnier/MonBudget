package com.monbudget.model;

import com.monbudget.model.enums.Frequency;
import java.time.LocalDate;

public record RecurrenceRule(
        Long id,
        Frequency frequency,
        int interval,
        LocalDate startDate,
        LocalDate endDate,
        Integer dayOfMonth
) {}