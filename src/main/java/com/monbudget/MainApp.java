package com.monbudget;

import com.monbudget.controller.DashboardController;
import com.monbudget.dao.BookDao;
import com.monbudget.dao.CategoryDao;
import com.monbudget.dao.DatabaseManager;
import com.monbudget.dao.OperationDao;
import com.monbudget.model.Book;
import com.monbudget.service.BalanceService;
import com.monbudget.service.OperationService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.kordamp.bootstrapfx.BootstrapFX;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;

public class MainApp extends Application {

    // Capture d'erreur critique survenue pendant l'initialisation hors UI Thread
    private Throwable startupError;

    // Ressources et dépendances
    private Connection dbConnection;
    private BookDao bookDao;
    private CategoryDao categoryDao;
    private OperationDao operationDao;
    private BalanceService balanceService;
    private OperationService operationService;

    /**
     * Étape 1 : Initialisation en arrière-plan (Launcher Thread).
     * Les accès disque lourds et les migrations Flyway s'exécutent ici sans bloquer l'UI.
     */
    @Override
    public void init() {
        try {
            // 1. Exécution des migrations Flyway (création/mise à jour du schéma)
            DatabaseManager.initializeDatabase();

            // 2. Récupération de la connexion SQLite (Mode WAL activé)
            this.dbConnection = DatabaseManager.getConnection();

            // 3. Initialisation de la couche DAO
            this.bookDao = new BookDao(dbConnection);
            this.categoryDao = new CategoryDao(dbConnection);
            this.operationDao = new OperationDao(dbConnection);

            // 4. Initialisation des Services métier
            this.balanceService = new BalanceService(operationDao);
            this.operationService = new OperationService(operationDao);

        } catch (Throwable t) {
            // On capture l'exception pour la traiter graphiquement dans start()
            this.startupError = t;
        }
    }

    /**
     * Étape 2 : Construction et affichage de l'interface (JavaFX Application Thread).
     */
    @Override
    public void start(Stage primaryStage) {
        // En cas d'échec critique lors de l'init(), afficher une boîte d'erreur explicite
        if (startupError != null) {
            showStartupErrorDialog(startupError);
            return;
        }

        try {
            // Sélection ou création du premier livre par défaut
            Book activeBook = resolveInitialBook();

            // Chargement de la vue FXML principale
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/monbudget/views/dashboard-view.fxml"));
            Parent root = loader.load();

            // Injection des dépendances dans le contrôleur principal
            DashboardController controller = loader.getController();
            controller.initData(
                    balanceService,
                    operationService,
                    operationDao,
                    categoryDao,
                    bookDao,
                    activeBook
            );

            // Assemblage de la scène et application des styles
            Scene scene = new Scene(root);
            scene.getStylesheets().add(BootstrapFX.bootstrapFXStylesheet());

            var customThemeUrl = getClass().getResource("/css/custom-theme.css");
            if (customThemeUrl != null) {
                scene.getStylesheets().add(customThemeUrl.toExternalForm());
            }

            primaryStage.setTitle("MonBudget - Gestion de Finances Personnelles");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(960);
            primaryStage.setMinHeight(640);
            primaryStage.show();

        } catch (Exception e) {
            showStartupErrorDialog(e);
        }
    }

    /**
     * Étape 3 : Libération des ressources lors de la fermeture de l'application.
     */
    @Override
    public void stop() {
        try {
            if (dbConnection != null && !dbConnection.isClosed()) {
                dbConnection.close();
                System.out.println("Connexion SQLite fermée proprement.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Récupère le premier livre actif en base ou en crée un par défaut si la base est neuve.
     */
    private Book resolveInitialBook() {
        List<Book> activeBooks = bookDao.findAllActive();
        if (!activeBooks.isEmpty()) {
            return activeBooks.get(0);
        }

        // Création d'un premier registre par défaut
        Book defaultBook = Book.create(
                "Vie courante",
                "Compte bancaire principal du quotidien",
                BigDecimal.ZERO,
                "#3498db"
        );
        return bookDao.save(defaultBook);
    }

    /**
     * Affiche une boîte de dialogue standardisée avec panneau technique dépliable.
     */
    private void showStartupErrorDialog(Throwable error) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur système - MonBudget");
        alert.setHeaderText("Impossible d'initialiser l'application ou la base de données.");
        alert.setContentText("Une erreur critique est survenue lors de la configuration locale :\n"
                + (error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName()));

        // Extraction de la pile d'appels technique
        StringWriter sw = new StringWriter();
        error.printStackTrace(new PrintWriter(sw));

        TextArea textArea = new TextArea(sw.toString());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);

        alert.getDialogPane().setExpandableContent(new VBox(textArea));

        // Clôture propre du processus après acquittement
        alert.showAndWait();
        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}