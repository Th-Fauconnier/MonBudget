package com.monbudget.controller;

import com.monbudget.dao.CategoryDao;
import com.monbudget.model.Category;
import com.monbudget.model.enums.OperationType;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.List;

public class CategoryDialogController {

    @FXML private TextField nameField;
    @FXML private ComboBox<OperationType> typeComboBox;
    @FXML private ComboBox<Category> parentCategoryComboBox;
    @FXML private ColorPicker colorPicker;
    @FXML private Label errorLabel;

    private CategoryDao categoryDao;
    private Category createdCategory;

    @FXML
    public void initialize() {
        typeComboBox.setItems(FXCollections.observableArrayList(OperationType.EXPENSE, OperationType.INCOME));
        typeComboBox.setValue(OperationType.EXPENSE);
        colorPicker.setValue(Color.web("#e67e22"));

        // Met à jour la liste des parents disponibles quand le type change
        typeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> refreshParentCategories(newVal));
    }

    public void initData(CategoryDao categoryDao) {
        this.categoryDao = categoryDao;
        refreshParentCategories(typeComboBox.getValue());
    }

    private void refreshParentCategories(OperationType type) {
        if (categoryDao == null || type == null) return;
        List<Category> candidates = categoryDao.findByType(type);
        parentCategoryComboBox.setItems(FXCollections.observableArrayList(candidates));
    }

    @FXML
    public void handleSave() {
        if (!validateInputs()) {
            return;
        }

        try {
            String name = nameField.getText().trim();
            OperationType type = typeComboBox.getValue();
            Category parent = parentCategoryComboBox.getValue();
            Long parentId = parent != null ? parent.id() : null;
            String colorHex = toHex(colorPicker.getValue());

            Category newCategory = new Category(
                    0L,
                    parentId,
                    name,
                    type,
                    colorHex,
                    "tag",
                    false
            );

            this.createdCategory = categoryDao.save(newCategory);
            closeStage();

        } catch (Exception e) {
            errorLabel.setText("Erreur d'enregistrement : " + e.getMessage());
        }
    }

    @FXML
    public void handleCancel() {
        closeStage();
    }

    public Category getCreatedCategory() {
        return createdCategory;
    }

    private boolean validateInputs() {
        if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
            errorLabel.setText("Le libellé de la catégorie est obligatoire.");
            return false;
        }
        if (typeComboBox.getValue() == null) {
            errorLabel.setText("Veuillez sélectionner le type d'opération.");
            return false;
        }
        errorLabel.setText("");
        return true;
    }

    private String toHex(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    private void closeStage() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }
}