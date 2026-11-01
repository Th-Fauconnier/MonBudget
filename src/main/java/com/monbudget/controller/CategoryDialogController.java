package com.monbudget.controller;

import com.monbudget.dao.CategoryDao;
import com.monbudget.model.Category;
import com.monbudget.model.enums.OperationType;
import com.monbudget.util.UiUtils;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.List;
import java.util.function.Consumer;

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
        UiUtils.setupOperationTypeComboBox(typeComboBox);
        UiUtils.setupCategoryComboBox(parentCategoryComboBox);

        colorPicker.setValue(Color.web("#e67e22"));

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
        if (!validateFields(nameField.getText(), typeComboBox.getValue(), errorLabel::setText)) {
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

    /**
     * Méthode de validation pure, testable sans lancer JavaFX.
     */
    protected boolean validateFields(String name, OperationType type, Consumer<String> errorHandler) {
        if (name == null || name.trim().isEmpty()) {
            errorHandler.accept("Le libellé de la catégorie est obligatoire.");
            return false;
        }
        if (type == null) {
            errorHandler.accept("Veuillez sélectionner le type d'opération.");
            return false;
        }
        errorHandler.accept("");
        return true;
    }

    private boolean validateInputs() {
        return validateFields(nameField.getText(), typeComboBox.getValue(), errorLabel::setText);
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