package com.monbudget.controller;

import com.monbudget.dao.BookDao;
import com.monbudget.model.Book;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.function.Consumer;

public class BookDialogController {

    @FXML private TextField nameField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField currencyField;
    @FXML private TextField initialBalanceField;
    @FXML private ColorPicker colorPicker;
    @FXML private Label errorLabel;

    private BookDao bookDao;
    private Book createdBook;

    @FXML
    public void initialize() {
        currencyField.setText("EUR");
        colorPicker.setValue(Color.web("#3498db"));
        initialBalanceField.setText("0.00");

        initialBalanceField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.contains(",")) {
                initialBalanceField.setText(newVal.replace(",", "."));
            }
        });
    }

    public void initData(BookDao bookDao) {
        this.bookDao = bookDao;
    }

    @FXML
    public void handleSave() {
        // Utilisation de la méthode métier en passant les composants graphiques et lesetError
        if (!validateFields(nameField.getText(), currencyField.getText(), initialBalanceField.getText(), errorLabel::setText)) {
            return;
        }

        try {
            String name = nameField.getText().trim();
            String description = descriptionArea.getText() != null ? descriptionArea.getText().trim() : "";
            String currency = currencyField.getText().trim().toUpperCase();
            BigDecimal initialBalance = new BigDecimal(initialBalanceField.getText().trim())
                    .setScale(2, RoundingMode.HALF_EVEN);
            String colorHex = toHex(colorPicker.getValue());

            Book newBook = new Book(
                    0L, name, description, currency, initialBalance,
                    colorHex, "wallet", false, LocalDateTime.now(), LocalDateTime.now()
            );

            this.createdBook = bookDao.save(newBook);
            closeStage();

        } catch (NumberFormatException e) {
            errorLabel.setText("Format du solde initial invalide (ex : 150.00).");
        } catch (Exception e) {
            errorLabel.setText("Erreur d'enregistrement : " + e.getMessage());
        }
    }

    @FXML
    public void handleCancel() {
        closeStage();
    }

    public Book getCreatedBook() {
        return createdBook;
    }

    /**
     * Méthode de validation pure, testable sans JavaFX.
     */
    protected boolean validateFields(String name, String currency, String balance, Consumer<String> errorHandler) {
        if (name == null || name.trim().isEmpty()) {
            errorHandler.accept("Le nom du livre est obligatoire.");
            return false;
        }

        if (currency == null || currency.trim().length() != 3) {
            errorHandler.accept("La devise doit comporter 3 lettres (ex : EUR, USD).");
            return false;
        }

        if (balance == null) {
            errorHandler.accept("Le solde de départ est invalide.");
            return false;
        }

        try {
            new BigDecimal(balance.trim());
        } catch (Exception e) {
            errorHandler.accept("Le solde de départ est invalide.");
            return false;
        }

        errorHandler.accept("");
        return true;
    }

    private boolean validateInputs() {
        return validateFields(nameField.getText(), currencyField.getText(), initialBalanceField.getText(), errorLabel::setText);
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