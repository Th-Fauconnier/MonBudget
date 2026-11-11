package com.monbudget.view.cell;

import javafx.scene.control.TableCell;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateTableCell<S> extends TableCell<S, LocalDate> {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public DateTableCell() {
        getStyleClass().add("col-center");
    }

    @Override
    protected void updateItem(LocalDate date, boolean empty) {
        super.updateItem(date, empty);
        setText(empty || date == null ? null : date.format(DATE_FORMATTER));
    }
}