package com.monbudget.controller;

import com.monbudget.dao.CategoryDao;
import com.monbudget.dao.OperationDao;
import com.monbudget.model.Book;
import com.monbudget.model.Category;
import com.monbudget.model.Operation;
import com.monbudget.model.enums.OperationStatus;
import com.monbudget.model.enums.OperationType;
import com.monbudget.util.UiUtils;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class OperationDialogController {

    @FXML private TextField labelField;
    @FXML private TextField amountField;
    @FXML private ComboBox<OperationType> typeComboBox;
    @FXML private ComboBox<Category> categoryComboBox;
    @FXML private DatePicker operationDatePicker;
    @FXML private CheckBox settledCheckBox;
    @FXML private TextArea notesArea;
    @FXML private Label errorLabel;

    private Book currentBook;
    private Operation existingOperation;
    private CategoryDao categoryDao;
    private OperationDao operationDao;

    @FXML
    public void initialize() {
        // Initialisation du ComboBox de type de flux
        UiUtils.setupOperationTypeComboBox(typeComboBox);

        // Initialisation du ComboBox de catégorie parente
        UiUtils.setupCategoryComboBox(categoryComboBox);

        typeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> updateCategoriesList(newVal));

        // Formatage réactif des montants : conversion automatique ',' -> '.'
        amountField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.contains(",")) {
                amountField.setText(newVal.replace(",", "."));
            }
        });
    }

    public void initData(Book book, Operation operation, CategoryDao categoryDao, OperationDao operationDao) {
        this.currentBook = book;
        this.existingOperation = operation;
        this.categoryDao = categoryDao;
        this.operationDao = operationDao;

        if (operation != null) {
            // Mode Édition
            labelField.setText(operation.label());
            amountField.setText(operation.amount().abs().toPlainString());
            typeComboBox.setValue(operation.operationType());
            operationDatePicker.setValue(operation.operationDate());
            settledCheckBox.setSelected(operation.status() == OperationStatus.SETTLED);
            notesArea.setText(operation.notes());

            updateCategoriesList(operation.operationType());
            if (operation.categoryId() != null) {
                categoryDao.findById(operation.categoryId()).ifPresent(categoryComboBox::setValue);
            }
        } else {
            // Mode Création par défaut
            typeComboBox.setValue(OperationType.EXPENSE);
            operationDatePicker.setValue(LocalDate.now());
            settledCheckBox.setSelected(true);
            updateCategoriesList(OperationType.EXPENSE);
        }
    }

    private void updateCategoriesList(OperationType type) {
        if (type == null || categoryDao == null) return;
        List<Category> categories = categoryDao.findByType(type);
        categoryComboBox.setItems(FXCollections.observableArrayList(categories));
    }

    @FXML
    public void handleSave() {
        if (!validateInputs()) {
            return;
        }

        try {
            String label = labelField.getText().trim();
            BigDecimal rawAmount = new BigDecimal(amountField.getText().trim()).setScale(2, RoundingMode.HALF_EVEN);
            OperationType type = typeComboBox.getValue();

            // Une dépense est négative, un revenu est positif
            BigDecimal finalAmount = (type == OperationType.EXPENSE) ? rawAmount.abs().negate() : rawAmount.abs();

            Category selectedCategory = categoryComboBox.getValue();
            Long categoryId = selectedCategory != null ? selectedCategory.id() : null;

            OperationStatus status = settledCheckBox.isSelected() ? OperationStatus.SETTLED : OperationStatus.PLANNED;
            LocalDate operationDate = operationDatePicker.getValue();
            LocalDate settledAt = (status == OperationStatus.SETTLED) ? LocalDate.now() : null;
            String notes = notesArea.getText() != null ? notesArea.getText().trim() : "";

            if (existingOperation == null) {
                // Création
                Operation newOperation = new Operation(
                        0L,
                        currentBook.id(),
                        categoryId,
                        null,
                        null,
                        label,
                        finalAmount,
                        type,
                        status,
                        operationDate,
                        settledAt,
                        notes,
                        null,
                        LocalDateTime.now(),
                        LocalDateTime.now()
                );
                operationDao.save(newOperation);
            } else {
                // Mise à jour
                Operation updatedOperation = new Operation(
                        existingOperation.id(),
                        currentBook.id(),
                        categoryId,
                        existingOperation.paymentMethodId(),
                        existingOperation.recurrenceRuleId(),
                        label,
                        finalAmount,
                        type,
                        status,
                        operationDate,
                        settledAt,
                        notes,
                        existingOperation.transferOperationId(),
                        existingOperation.createdAt(),
                        LocalDateTime.now()
                );
                operationDao.update(updatedOperation);
            }

            closeStage();

        } catch (NumberFormatException e) {
            errorLabel.setText("Montant invalide. Exemple attendu : 15.50");
        } catch (Exception e) {
            errorLabel.setText("Erreur d'enregistrement : " + e.getMessage());
        }
    }

    @FXML
    public void handleCancel() {
        closeStage();
    }

    private boolean validateInputs() {
        if (labelField.getText() == null || labelField.getText().trim().isEmpty()) {
            errorLabel.setText("Veuillez saisir un libellé.");
            return false;
        }

        String amountText = amountField.getText();
        if (amountText == null || amountText.trim().isEmpty()) {
            errorLabel.setText("Veuillez indiquer un montant.");
            return false;
        }

        try {
            BigDecimal amt = new BigDecimal(amountText.trim());
            if (amt.compareTo(BigDecimal.ZERO) <= 0) {
                errorLabel.setText("Le montant doit être strictement positif.");
                return false;
            }
        } catch (NumberFormatException e) {
            errorLabel.setText("Format du montant incorrect (ex : 29.90).");
            return false;
        }

        if (operationDatePicker.getValue() == null) {
            errorLabel.setText("Veuillez sélectionner une date.");
            return false;
        }

        errorLabel.setText("");
        return true;
    }

    private void closeStage() {
        Stage stage = (Stage) labelField.getScene().getWindow();
        stage.close();
    }
}