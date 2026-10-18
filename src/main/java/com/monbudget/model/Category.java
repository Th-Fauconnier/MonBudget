package com.monbudget.model;

import com.monbudget.model.enums.OperationType;

public record Category(
        Long id,
        Long parentId,
        String name,
        OperationType type, // EXPENSE ou INCOME
        String colorHex,
        String iconName,
        boolean isSystem
) {
    public boolean isSubCategory() {
        return parentId != null;
    }
}