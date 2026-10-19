package com.monbudget.controller;

import com.monbudget.dao.BookDao;
import com.monbudget.dao.CategoryDao;
import com.monbudget.dao.DatabaseManager;
import com.monbudget.dao.OperationDao;
import com.monbudget.model.Book;
import com.monbudget.model.Category;
import com.monbudget.model.Operation;
import com.monbudget.model.enums.OperationStatus;
import com.monbudget.model.enums.OperationType;
import com.monbudget.service.BalanceService;
import com.monbudget.service.BalanceSummary;
import com.monbudget.service.OperationService;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

public class DashboardController {

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.FRANCE);

    // En-tête : Sélecteur de livre & filtres
    @FXML private ComboBox<Book> bookComboBox;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private ProgressIndicator loadingIndicator;

    // Cartes KPI
    @FXML private Label settledBalanceLabel;
    @FXML private Label netPlannedLabel;
    @FXML private Label projectedBalanceLabel;

    // Graphiques
    @FXML private LineChart<String, Number> balanceLineChart;
    @FXML private PieChart categoryPieChart;

    // TableView des opérations
    @FXML private TableView<Operation> operationsTable;
    @FXML private TableColumn<Operation, LocalDate> dateColumn;
    @FXML private TableColumn<Operation, String> labelColumn;
    @FXML private TableColumn<Operation, Long> categoryColumn;
    @FXML private TableColumn<Operation, BigDecimal> amountColumn;
    @FXML private TableColumn<Operation, OperationStatus> statusColumn;
    @FXML private TableColumn<Operation, Void> actionColumn;

    // Dépendances injectées
    private BalanceService balanceService;
    private OperationService operationService;
    private OperationDao operationDao;
    private CategoryDao categoryDao;
    private BookDao bookDao;
    private Book currentBook;

    private final Map<Long, String> categoryNamesMap = new HashMap<>();

    @FXML
    public void initialize() {
        setupBookComboBox();
        setupTableColumns();
        setupDefaultDates();
    }

    /**
     * Initialisation et injection de dépendances.
     */
    public void initData(
            BalanceService balanceService,
            OperationService operationService,
            OperationDao operationDao,
            CategoryDao categoryDao,
            BookDao bookDao,
            Book initialBook
    ) {
        this.balanceService = balanceService;
        this.operationService = operationService;
        this.operationDao = operationDao;
        this.categoryDao = categoryDao;
        this.bookDao = bookDao;
        this.currentBook = initialBook;

        refreshCategoryCache();
        refreshBookList(initialBook);
    }

    private void setupBookComboBox() {
        // Affichage textuel dans la zone de sélection fermée
        bookComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Book book) {
                return book != null ? book.name() : "";
            }

            @Override
            public Book fromString(String string) {
                return null;
            }
        });

        // Rendu visuel dans le menu déroulant : pastille de couleur + nom
        bookComboBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Book book, boolean empty) {
                super.updateItem(book, empty);
                if (empty || book == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Circle badge = new Circle(6);
                    try {
                        badge.setFill(Color.web(book.colorHex()));
                    } catch (Exception e) {
                        badge.setFill(Color.web("#3498db"));
                    }
                    Label nameLabel = new Label(book.name());
                    nameLabel.setStyle("-fx-font-weight: bold;");

                    HBox container = new HBox(8, badge, nameLabel);
                    container.setStyle("-fx-alignment: CENTER_LEFT;");
                    setGraphic(container);
                }
            }
        });

        // Écouteur de changement de livre
        bookComboBox.valueProperty().addListener((obs, oldBook, newBook) -> {
            if (newBook != null && !newBook.equals(currentBook)) {
                this.currentBook = newBook;
                loadDashboardDataAsync();
            }
        });
    }

    public void refreshBookList(Book bookToSelect) {
        if (bookDao == null) return;
        List<Book> books = bookDao.findAllActive();
        bookComboBox.setItems(FXCollections.observableArrayList(books));

        if (bookToSelect != null) {
            bookComboBox.setValue(bookToSelect);
            this.currentBook = bookToSelect;
        } else if (!books.isEmpty()) {
            bookComboBox.setValue(books.get(0));
            this.currentBook = books.get(0);
        }

        loadDashboardDataAsync();
    }

    private void setupDefaultDates() {
        LocalDate now = LocalDate.now();
        startDatePicker.setValue(now.with(TemporalAdjusters.firstDayOfMonth()));
        endDatePicker.setValue(now.with(TemporalAdjusters.lastDayOfMonth()));

        startDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> loadDashboardDataAsync());
        endDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> loadDashboardDataAsync());
    }

    private void setupTableColumns() {
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("operationDate"));
        labelColumn.setCellValueFactory(new PropertyValueFactory<>("label"));

        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("categoryId"));
        categoryColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Long categoryId, boolean empty) {
                super.updateItem(categoryId, empty);
                if (empty || categoryId == null) {
                    setText("Sans catégorie");
                } else {
                    setText(categoryNamesMap.getOrDefault(categoryId, "Inconnue"));
                }
            }
        });

        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        amountColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal amount, boolean empty) {
                super.updateItem(amount, empty);
                if (empty || amount == null) {
                    setText(null);
                    getStyleClass().removeAll("money-cell-expense", "money-cell-income");
                } else {
                    setText(CURRENCY_FORMAT.format(amount));
                    getStyleClass().add("money-cell");
                    if (amount.compareTo(BigDecimal.ZERO) < 0) {
                        getStyleClass().add("money-cell-expense");
                        getStyleClass().remove("money-cell-income");
                    } else {
                        getStyleClass().add("money-cell-income");
                        getStyleClass().remove("money-cell-expense");
                    }
                }
            }
        });

        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(OperationStatus status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(status == OperationStatus.SETTLED ? "RÉALISÉ" : "EN ATTENTE");
                    badge.getStyleClass().add("badge-status");
                    badge.getStyleClass().add(status == OperationStatus.SETTLED ? "badge-settled" : "badge-planned");
                    setGraphic(badge);
                }
            }
        });

        actionColumn.setCellFactory(col -> new TableCell<>() {
            private final Button pointButton = new Button("Pointer");

            {
                pointButton.getStyleClass().addAll("btn", "btn-default", "btn-sm");
                pointButton.setOnAction(e -> {
                    Operation op = getTableView().getItems().get(getIndex());
                    if (op.status() == OperationStatus.PLANNED) {
                        operationService.markAsSettledToday(op);
                        loadDashboardDataAsync();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Operation op = getTableView().getItems().get(getIndex());
                    pointButton.setDisable(op.status() == OperationStatus.SETTLED);
                    setGraphic(pointButton);
                }
            }
        });
    }

    @FXML
    public void loadDashboardDataAsync() {
        if (currentBook == null || startDatePicker.getValue() == null || endDatePicker.getValue() == null) {
            return;
        }

        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();

        if (loadingIndicator != null) loadingIndicator.setVisible(true);

        Task<DashboardPayload> task = new Task<>() {
            @Override
            protected DashboardPayload call() {
                List<Operation> operations = operationDao.findByBookAndDateRange(currentBook.id(), start, end);
                BalanceSummary summary = balanceService.computeSummary(currentBook, start, end);
                XYChart.Series<String, Number> series = balanceService.buildBalanceHistorySeries(currentBook, start, end);
                List<PieChart.Data> pieData = balanceService.buildExpenseCategoryPieData(operations, categoryNamesMap);

                return new DashboardPayload(operations, summary, series, pieData);
            }
        };

        task.setOnSucceeded(e -> {
            DashboardPayload payload = task.getValue();

            settledBalanceLabel.setText(CURRENCY_FORMAT.format(payload.summary().currentBalance()));
            netPlannedLabel.setText(CURRENCY_FORMAT.format(payload.summary().netPlanned()));
            projectedBalanceLabel.setText(CURRENCY_FORMAT.format(payload.summary().projectedBalance()));

            operationsTable.setItems(FXCollections.observableArrayList(payload.operations()));

            balanceLineChart.getData().clear();
            balanceLineChart.getData().add(payload.historySeries());

            categoryPieChart.setData(FXCollections.observableArrayList(payload.pieData()));
            for (PieChart.Data slice : categoryPieChart.getData()) {
                Tooltip.install(slice.getNode(), new Tooltip(slice.getName() + " : " + CURRENCY_FORMAT.format(slice.getPieValue())));
            }

            if (loadingIndicator != null) loadingIndicator.setVisible(false);
        });

        task.setOnFailed(e -> {
            if (loadingIndicator != null) loadingIndicator.setVisible(false);
            showError("Erreur lors de la récupération des données", task.getException());
        });

        Thread worker = new Thread(task);
        worker.setDaemon(true);
        worker.start();
    }

    @FXML
    public void handleNewBook() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/monbudget/views/book-dialog-view.fxml"));
            Parent root = loader.load();

            BookDialogController controller = loader.getController();
            controller.initData(bookDao);

            Stage stage = new Stage();
            stage.setTitle("Nouveau livre de comptes");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(bookComboBox.getScene().getWindow());
            stage.setScene(new Scene(root));
            stage.showAndWait();

            Book created = controller.getCreatedBook();
            if (created != null) {
                refreshBookList(created);
            }
        } catch (Exception ex) {
            showError("Impossible d'ouvrir le dialogue de création de compte", ex);
        }
    }

    @FXML
    public void handleNewCategory() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/monbudget/views/category-dialog-view.fxml"));
            Parent root = loader.load();

            CategoryDialogController controller = loader.getController();
            controller.initData(categoryDao);

            Stage stage = new Stage();
            stage.setTitle("Nouvelle catégorie");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(bookComboBox.getScene().getWindow());
            stage.setScene(new Scene(root));
            stage.showAndWait();

            if (controller.getCreatedCategory() != null) {
                refreshCategoryCache();
                loadDashboardDataAsync();
            }
        } catch (Exception ex) {
            showError("Impossible d'ouvrir le dialogue de création de catégorie", ex);
        }
    }

    @FXML
    public void handleNewOperation() {
        openOperationDialog(null);
    }

    @FXML
    public void handleEditOperation() {
        Operation selected = operationsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openOperationDialog(selected);
        }
    }

    @FXML
    public void handleDeleteOperation() {
        Operation selected = operationsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Suppression");
        confirm.setHeaderText(null);
        confirm.setContentText("Confirmez-vous la suppression de l'opération : " + selected.label() + " ?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            operationDao.delete(selected.id());
            loadDashboardDataAsync();
        }
    }

    private void openOperationDialog(Operation operationToEdit) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/monbudget/views/operation-dialog-view.fxml"));
            Parent root = loader.load();

            OperationDialogController controller = loader.getController();
            controller.initData(currentBook, operationToEdit, categoryDao, operationDao);

            Stage dialogStage = new Stage();
            dialogStage.setTitle(operationToEdit == null ? "Nouvelle opération" : "Modifier une opération");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(operationsTable.getScene().getWindow());
            dialogStage.setScene(new Scene(root));
            dialogStage.showAndWait();

            loadDashboardDataAsync();

        } catch (Exception ex) {
            showError("Impossible d'ouvrir le formulaire d'opération", ex);
        }
    }

    private void refreshCategoryCache() {
        categoryNamesMap.clear();
        for (Category cat : categoryDao.findAll()) {
            categoryNamesMap.put(cat.id(), cat.name());
        }
    }

    private void showError(String message, Throwable error) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur Dashboard");
        alert.setHeaderText(message);
        alert.setContentText(error != null ? error.getMessage() : "");
        alert.showAndWait();
    }

    private record DashboardPayload(
            List<Operation> operations,
            BalanceSummary summary,
            XYChart.Series<String, Number> historySeries,
            List<PieChart.Data> pieData
    ) {}
}