package com.monbudget.util;

import com.monbudget.model.Book;
import java.time.LocalDate;
import java.util.function.Consumer;

public class DashboardValidator {

    public static boolean validateFilters(Book book, LocalDate startDate, LocalDate endDate, Consumer<String> errorHandler) {
        if (book == null) {
            errorHandler.accept("Veuillez sélectionner un livre de comptes.");
            return false;
        }
        if (startDate == null || endDate == null) {
            errorHandler.accept("Les dates de début et de fin sont obligatoires.");
            return false;
        }
        if (startDate.isAfter(endDate)) {
            errorHandler.accept("La date de début ne peut pas être postérieure à la date de fin.");
            return false;
        }
        errorHandler.accept("");
        return true;
    }
}