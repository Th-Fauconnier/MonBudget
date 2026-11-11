package com.monbudget.view.cell;

import javafx.scene.control.TableCell;
import java.util.Map;

public class CategoryTableCell<S> extends TableCell<S, Long> {
    private final Map<Long, String> categoryNamesMap;

    public CategoryTableCell(Map<Long, String> categoryNamesMap) {
        this.categoryNamesMap = categoryNamesMap;
    }

    @Override
    protected void updateItem(Long categoryId, boolean empty) {
        super.updateItem(categoryId, empty);
        setText(empty || categoryId == null ? "Sans catégorie" : categoryNamesMap.getOrDefault(categoryId, "Inconnue"));
    }
}