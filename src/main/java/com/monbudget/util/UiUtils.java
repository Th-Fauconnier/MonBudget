package com.monbudget.util;

import com.monbudget.model.Category;
import com.monbudget.model.enums.OperationType;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Callback;
import javafx.util.StringConverter;

public final class UiUtils {

    private UiUtils() {
        // Constructeur privé pour empêcher l'instanciation
    }

    /**
     * 1. Configure le ComboBox pour les types d'opération (EXPENSE / INCOME)
     *    avec les fonds pastel et les couleurs respectives des options.
     */
    public static void setupOperationTypeComboBox(ComboBox<OperationType> comboBox) {
        comboBox.setItems(FXCollections.observableArrayList(OperationType.EXPENSE, OperationType.INCOME));

        Callback<javafx.scene.control.ListView<OperationType>, ListCell<OperationType>> typeCellFactory = lv -> new ListCell<>() {
            @Override
            protected void updateItem(OperationType type, boolean empty) {
                super.updateItem(type, empty);
                getStyleClass().removeAll("cell-type-expense", "cell-type-income");

                if (empty || type == null) {
                    setText(null);
                } else {
                    if (type == OperationType.EXPENSE) {
                        setText("Dépense");
                        getStyleClass().add("cell-type-expense");
                    } else if (type == OperationType.INCOME) {
                        setText("Revenu");
                        getStyleClass().add("cell-type-income");
                    } else {
                        setText(type.name());
                    }
                }
            }
        };

        comboBox.setCellFactory(typeCellFactory);
        comboBox.setButtonCell(typeCellFactory.call(null));

        // Écouteur pour adapter dynamiquement le fond du ComboBox principal
        comboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateComboBoxBackgroundStyle(comboBox, newVal);
        });

        // Valeur par défaut initiale
        if (comboBox.getValue() == null) {
            comboBox.setValue(OperationType.EXPENSE);
        }
        updateComboBoxBackgroundStyle(comboBox, comboBox.getValue());
    }

    private static void updateComboBoxBackgroundStyle(ComboBox<OperationType> comboBox, OperationType type) {
        comboBox.getStyleClass().removeAll("type-expense", "type-income");

        if (type == OperationType.EXPENSE) {
            comboBox.getStyleClass().add("type-expense");
        } else if (type == OperationType.INCOME) {
            comboBox.getStyleClass().add("type-income");
        }
    }

    /**
     * 2. Configure le ComboBox pour les catégories
     *    avec la pastille de couleur, le nom et le type coloré (Dépense/Revenu).
     */
    public static void setupCategoryComboBox(ComboBox<Category> comboBox) {
        // Affichage textuel dans la zone de sélection fermée
        comboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Category category) {
                return category != null ? category.name() : "";
            }

            @Override
            public Category fromString(String string) {
                return null;
            }
        });

        // Rendu visuel dans le menu déroulant : pastille + nom + type coloré
        comboBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Category category, boolean empty) {
                super.updateItem(category, empty);
                if (empty || category == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    // Pastille de couleur
                    Circle colorBadge = new Circle(6);
                    try {
                        colorBadge.setFill(Color.web(category.colorHex()));
                    } catch (Exception e) {
                        colorBadge.setFill(Color.web("#bdc3c7"));
                    }

                    // Nom de la catégorie
                    Label nameLabel = new Label(category.name());
                    nameLabel.getStyleClass().add("category-name");

                    // Type de catégorie (Dépense / Revenu)
                    Label typeLabel = new Label();
                    typeLabel.getStyleClass().addAll("category-type");

                    String typeStr = category.type() != null ? category.type().name() : "";
                    typeLabel.getStyleClass().removeAll("category-type-expense", "category-type-income");

                    if (typeStr.contains("EXPENSE")) {
                        typeLabel.setText("(Dépense)");
                        typeLabel.getStyleClass().add("category-type-expense");
                    } else if (typeStr.contains("INCOME")) {
                        typeLabel.setText("(Revenu)");
                        typeLabel.getStyleClass().add("category-type-income");
                    } else {
                        typeLabel.setText("(" + typeStr + ")");
                    }

                    // Conteneur horizontal
                    HBox container = new HBox(8, colorBadge, nameLabel, typeLabel);
                    container.setStyle("-fx-alignment: CENTER_LEFT;");
                    setGraphic(container);
                    setText(null);
                }
            }
        });

        // Application du même rendu pour le bouton fermé du ComboBox
        comboBox.setButtonCell(comboBox.getCellFactory().call(null));
    }
}