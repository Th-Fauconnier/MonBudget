package com.monbudget.controller;

import com.monbudget.dao.BookDao;
import com.monbudget.dao.CategoryDao;
import com.monbudget.dao.OperationDao;
import com.monbudget.dao.RecurrenceRuleDao;
import com.monbudget.model.Book;
import com.monbudget.model.Category;
import com.monbudget.model.Operation;
import com.monbudget.model.enums.OperationStatus;
import com.monbudget.service.*;
import com.monbudget.util.DashboardValidator;
import com.monbudget.util.PeriodUtils;
import com.monbudget.view.cell.*;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

public class DashboardController {

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.FRANCE);

    @FXML private ComboBox<Book> bookComboBox;
    @FXML private ComboBox<String> periodPresetCombo;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private ProgressIndicator loadingIndicator;

    @FXML private Label settledBalanceLabel;
    @FXML private Label netPlannedLabel;
    @FXML private Label projectedBalanceLabel;

    @FXML private LineChart<String, Number> balanceLineChart;
    @FXML private PieChart categoryPieChart;

    @FXML private TableView<Operation> operationsTable;
    @FXML private TableColumn<Operation, LocalDate> dateColumn;
    @FXML private TableColumn<Operation, String> labelColumn;
    @FXML private TableColumn<Operation, Long> categoryColumn;
    @FXML private TableColumn<Operation, BigDecimal> amountColumn;
    @FXML private TableColumn<Operation, OperationStatus> statusColumn;
    @FXML private TableColumn<Operation, Void> actionColumn;

    private DialogService dialogService;
    private DashboardService dashboardService;
    private OperationService operationService;
    private OperationDao operationDao;
    private CategoryDao categoryDao;
    private BookDao bookDao;
    private RecurrenceRuleDao recurrenceRuleDao;
    private Book currentBook;

    public void initData(
            DashboardService dashboardService,
            OperationService operationService,
            DialogService dialogService,
            OperationDao operationDao,
            CategoryDao categoryDao,
            BookDao bookDao,
            RecurrenceRuleDao recurrenceRuleDao,
            Book initialBook
    ) {
        this.dashboardService = dashboardService;
        this.operationService = operationService;
        this.dialogService = dialogService;
        this.operationDao = operationDao;
        this.categoryDao = categoryDao;
        this.bookDao = bookDao;
        this.recurrenceRuleDao = recurrenceRuleDao;
        this.currentBook = initialBook;

        dashboardService.refreshCategoryCache();
        refreshBookList(initialBook);
    }

    @FXML
    public void initialize() {
        setupBookComboBox();
        setupTableColumns();
        setupDefaultDates();

        // Configuration centralisée des dates via PeriodUtils
        PeriodUtils.setupDateBindings(periodPresetCombo, startDatePicker, endDatePicker);
    }

    @FXML
    private void handlePrevMonth() {
        PeriodUtils.shiftPeriod(-1, java.time.temporal.ChronoUnit.MONTHS, startDatePicker, endDatePicker);
    }

    @FXML
    private void handleNextMonth() {
        PeriodUtils.shiftPeriod(1, java.time.temporal.ChronoUnit.MONTHS, startDatePicker, endDatePicker);
    }

    @FXML
    private void handlePrevYear() {
        PeriodUtils.shiftPeriod(-1, java.time.temporal.ChronoUnit.YEARS, startDatePicker, endDatePicker);
    }

    @FXML
    private void handleNextYear() {
        PeriodUtils.shiftPeriod(1, java.time.temporal.ChronoUnit.YEARS, startDatePicker, endDatePicker);
    }

    private void setupBookComboBox() {
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
            bookComboBox.setValue(books.getFirst());
            this.currentBook = books.getFirst();
        }

        loadDashboardDataAsync();
    }

    private void setupDefaultDates() {
        LocalDate now = LocalDate.now();
        startDatePicker.setValue(now.with(TemporalAdjusters.firstDayOfMonth()));
        endDatePicker.setValue(now.with(TemporalAdjusters.lastDayOfMonth()));

    }

    private void setupTableColumns() {
        // Date
        dateColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().operationDate()));
        dateColumn.setCellFactory(col -> new DateTableCell<>());

        // Libellé
        labelColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().label()));

        // Catégorie
        categoryColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().categoryId()));
        categoryColumn.setCellFactory(col -> new CategoryTableCell<>( dashboardService.getCategoryNamesMap() ));

        // Montant
        amountColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().amount()));
        amountColumn.setCellFactory(col -> new CurrencyTableCell<>());

        // Statut
        statusColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().status()));
        statusColumn.setCellFactory(col -> new OperationStatusCell());

        // Action
        actionColumn.setCellFactory(col -> new OperationActionCell(op -> {
            operationService.markAsSettledToday(op);
            loadDashboardDataAsync();
        }));
    }

    @FXML
    private void handleApplyFilters() {
        Book selectedBook = bookComboBox.getValue();
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();

        // La validation s'exécute uniquement ici au clic
        boolean isValid = DashboardValidator.validateFilters(selectedBook, startDate, endDate, errorMsg -> {
            if (!errorMsg.isEmpty()) {
                showError("Erreur de filtre",new IllegalArgumentException( errorMsg ) ); // Méthode d'affichage d'alerte
            }
        });

        if (!isValid) {
            return; // Bloque le rechargement si les critères sont invalides
        }

        // Exécute le chargement de données
        loadDashboardDataAsync();
    }

    @FXML
    public void loadDashboardDataAsync() {
        if (!DashboardValidator.validateFilters(currentBook, startDatePicker.getValue(), endDatePicker.getValue(), msg -> {
            if (!msg.isEmpty()) {
                showError("Paramètres de filtre invalides", new IllegalArgumentException(msg));
            }
        })) {
            return;
        }

        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();

        if (loadingIndicator != null) loadingIndicator.setVisible(true);

        Task<DashboardPayload> task = dashboardService.createLoadTask( currentBook, start, end, dashboardService.getCategoryNamesMap() );

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
        Book created = dialogService.openBookDialog(bookComboBox.getScene().getWindow(), bookDao);
        if (created != null) {
            refreshBookList(created);
        }
    }

    @FXML
    public void handleNewCategory() {
        Category created = dialogService.openCategoryDialog(bookComboBox.getScene().getWindow(), categoryDao);
        if (created != null) {
            dashboardService.refreshCategoryCache();
            loadDashboardDataAsync();
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
        dialogService.openOperationDialog(
                operationsTable.getScene().getWindow(),
                currentBook,
                operationToEdit,
                categoryDao,
                operationDao,
                recurrenceRuleDao
        );
        loadDashboardDataAsync();
    }

    private void showError(String message, Throwable error) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur Dashboard");
        alert.setHeaderText(message);
        alert.setContentText(error != null ? error.getMessage() : "");
        alert.showAndWait();
    }

}