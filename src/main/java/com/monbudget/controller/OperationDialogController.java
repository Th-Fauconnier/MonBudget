package com.monbudget.controller;

import com.monbudget.dao.CategoryDao;
import com.monbudget.dao.OperationDao;
import com.monbudget.dao.RecurrenceRuleDao;
import com.monbudget.model.Book;
import com.monbudget.model.Category;
import com.monbudget.model.Operation;
import com.monbudget.model.RecurrenceRule;
import com.monbudget.model.enums.Frequency;
import com.monbudget.model.enums.OperationStatus;
import com.monbudget.model.enums.OperationType;
import com.monbudget.util.UiUtils;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

public class OperationDialogController {

    @FXML private TextField labelField;
    @FXML private TextField amountField;
    @FXML private ComboBox<OperationType> typeComboBox;
    @FXML private ComboBox<Category> categoryComboBox;
    @FXML private DatePicker operationDatePicker;
    @FXML private CheckBox settledCheckBox;
    @FXML private TextArea notesArea;
    @FXML private Label errorLabel;

    // --- champs FXML pour la récurrence ---
    @FXML private CheckBox recurringCheckBox;
    @FXML private VBox recurrenceBox; // Conteneur à afficher/masquer selon la case
    @FXML private ComboBox<Frequency> frequencyComboBox;
    @FXML private Spinner<Integer> intervalSpinner;
    @FXML private DatePicker recurrenceEndDatePicker;

    private Book currentBook;
    private Operation existingOperation;
    private CategoryDao categoryDao;
    private OperationDao operationDao;
    private RecurrenceRuleDao recurrenceRuleDao;

    @FXML
    public void initialize() {
        UiUtils.setupOperationTypeComboBox(typeComboBox);
        UiUtils.setupCategoryComboBox(categoryComboBox);

        // Initialisation de la combobox de fréquence
        frequencyComboBox.setItems(FXCollections.observableArrayList(Frequency.values()));
        frequencyComboBox.setValue(Frequency.MONTHLY);

        // Initialisation du spinner d'intervalle (min 1, max 365, valeur par défaut 1)
        intervalSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 365, 1));

        // Gestion de l'affichage dynamique du bloc de récurrence
        recurrenceBox.managedProperty().bind(recurringCheckBox.selectedProperty());
        recurrenceBox.visibleProperty().bind(recurringCheckBox.selectedProperty());

        typeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> updateCategoriesList(newVal));

        amountField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.contains(",")) {
                amountField.setText(newVal.replace(",", "."));
            }
        });
    }

    public void initData(Book book, Operation operation, CategoryDao categoryDao, OperationDao operationDao, RecurrenceRuleDao recurrenceRuleDao) {
        this.currentBook = book;
        this.existingOperation = operation;
        this.categoryDao = categoryDao;
        this.operationDao = operationDao;
        this.recurrenceRuleDao = recurrenceRuleDao;

        if (operation != null) {
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

            // Gestion de la récurrence existante
            if (operation.recurrenceRuleId() != null && recurrenceRuleDao != null) {
                recurrenceRuleDao.findById(operation.recurrenceRuleId()).ifPresent(rule -> {
                    recurringCheckBox.setSelected(true);
                    frequencyComboBox.setValue(rule.frequency());
                    intervalSpinner.getValueFactory().setValue(rule.interval());
                    recurrenceEndDatePicker.setValue(rule.endDate());
                });
            }
        } else {
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
        if (!validateFields(
                labelField.getText(),
                amountField.getText(),
                operationDatePicker.getValue(),
                recurringCheckBox.isSelected(),
                recurrenceEndDatePicker.getValue(),
                errorLabel::setText
        )) {
            return;
        }

        try {
            String label = labelField.getText().trim();
            BigDecimal rawAmount = new BigDecimal(amountField.getText().trim()).setScale(2, RoundingMode.HALF_EVEN);
            OperationType type = typeComboBox.getValue();

            BigDecimal finalAmount = (type == OperationType.EXPENSE) ? rawAmount.abs().negate() : rawAmount.abs();

            Category selectedCategory = categoryComboBox.getValue();
            Long categoryId = selectedCategory != null ? selectedCategory.id() : null;

            OperationStatus status = settledCheckBox.isSelected() ? OperationStatus.SETTLED : OperationStatus.PLANNED;
            LocalDate operationDate = operationDatePicker.getValue();
            LocalDate settledAt = (status == OperationStatus.SETTLED) ? LocalDate.now() : null;
            String notes = notesArea.getText() != null ? notesArea.getText().trim() : "";

            // --- Gestion de la Règle de Récurrence ---
            Long recurrenceRuleId = null;
            if (recurringCheckBox.isSelected() && recurrenceRuleDao != null) {
                Frequency freq = frequencyComboBox.getValue();
                int interval = intervalSpinner.getValue();
                LocalDate startDate = operationDate;
                LocalDate endDate = recurrenceEndDatePicker.getValue();
                Integer dayOfMonth = startDate.getDayOfMonth(); // Par défaut basé sur le jour de la date de début

                RecurrenceRule newRule = new RecurrenceRule(
                        null, freq, interval, startDate, endDate, dayOfMonth
                );
                RecurrenceRule savedRule = recurrenceRuleDao.save(newRule);
                recurrenceRuleId = savedRule.id();
            }

            if (existingOperation == null) {
                Operation newOperation = new Operation(
                        0L, currentBook.id(), categoryId, null, recurrenceRuleId,
                        label, finalAmount, type, status, operationDate,
                        settledAt, notes, null, LocalDateTime.now(), LocalDateTime.now()
                );
                operationDao.save(newOperation);
            } else {
                Operation updatedOperation = new Operation(
                        existingOperation.id(), currentBook.id(), categoryId,
                        existingOperation.paymentMethodId(), recurrenceRuleId != null ? recurrenceRuleId : existingOperation.recurrenceRuleId(),
                        label, finalAmount, type, status, operationDate,
                        settledAt, notes, existingOperation.transferOperationId(),
                        existingOperation.createdAt(), LocalDateTime.now()
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

    /**
     * Méthode de validation pure, testable sans lancer JavaFX.
     */
    protected boolean validateFields(String label, String amountText, LocalDate operationDate, boolean isRecurring, LocalDate recurrenceEndDate, Consumer<String> errorHandler) {
        if (label == null || label.trim().isEmpty()) {
            errorHandler.accept("Veuillez saisir un libellé.");
            return false;
        }

        if (amountText == null || amountText.trim().isEmpty()) {
            errorHandler.accept("Veuillez indiquer un montant.");
            return false;
        }

        try {
            BigDecimal amt = new BigDecimal(amountText.trim());
            if (amt.compareTo(BigDecimal.ZERO) <= 0) {
                errorHandler.accept("Le montant doit être strictement positif.");
                return false;
            }
        } catch (NumberFormatException e) {
            errorHandler.accept("Format du montant incorrect (ex : 29.90).");
            return false;
        }

        if (operationDate == null) {
            errorHandler.accept("Veuillez sélectionner une date.");
            return false;
        }

        // --- validation de la récurrence ---
        if (isRecurring && recurrenceEndDate != null && recurrenceEndDate.isBefore(operationDate)) {
            errorHandler.accept("La date de fin de récurrence ne peut pas être antérieure à la date de l'opération.");
            return false;
        }

        errorHandler.accept("");
        return true;
    }

    private boolean validateInputs() {
        return validateFields(labelField.getText(), amountField.getText(), operationDatePicker.getValue(), recurringCheckBox.isSelected(), recurrenceEndDatePicker.getValue(), errorLabel::setText);
    }

    private void closeStage() {
        Stage stage = (Stage) labelField.getScene().getWindow();
        stage.close();
    }
}