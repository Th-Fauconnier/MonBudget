package com.monbudget.view.cell;

import javafx.scene.control.TableCell;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class CurrencyTableCell<S> extends TableCell<S, BigDecimal> {
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.FRANCE);

    public CurrencyTableCell() {
        getStyleClass().add("money-cell");
    }

    @Override
    protected void updateItem(BigDecimal amount, boolean empty) {
        super.updateItem(amount, empty);
        getStyleClass().removeAll("money-cell-expense", "money-cell-income");

        if (empty || amount == null) {
            setText(null);
        } else {
            setText(CURRENCY_FORMAT.format(amount));
            getStyleClass().add(amount.compareTo(BigDecimal.ZERO) < 0 ? "money-cell-expense" : "money-cell-income");
        }
    }
}