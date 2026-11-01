package com.monbudget.model;

import com.monbudget.model.enums.OperationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CategoryTest {

    @Test
    @DisplayName("Devrait identifier qu'une catégorie est une sous-catégorie si le parentId est présent")
    void testIsSubCategoryTrue() {
        Category subCategory = new Category(
                2L,
                1L, // parentId non null -> sous-catégorie
                "Alimentation",
                OperationType.EXPENSE,
                "#2ecc71",
                "cart",
                false
        );

        assertTrue(subCategory.isSubCategory());
    }

    @Test
    @DisplayName("Devrait identifier qu'une catégorie n'est pas une sous-catégorie si le parentId est null")
    void testIsSubCategoryFalse() {
        Category parentCategory = new Category(
                1L,
                null, // parentId null -> catégorie principale
                "Dépenses courantes",
                OperationType.EXPENSE,
                "#e74c3c",
                "wallet",
                false
        );

        assertFalse(parentCategory.isSubCategory());
    }

    @Test
    @DisplayName("Devrait conserver correctement toutes les propriétés du record")
    void testCategoryProperties() {
        Category category = new Category(
                5L,
                null,
                "Salaire",
                OperationType.INCOME,
                "#3498db",
                "briefcase",
                true
        );

        assertEquals(5L, category.id());
        assertNull(category.parentId());
        assertEquals("Salaire", category.name());
        assertEquals(OperationType.INCOME, category.type());
        assertEquals("#3498db", category.colorHex());
        assertEquals("briefcase", category.iconName());
        assertTrue(category.isSystem());
    }
}