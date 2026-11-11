package com.monbudget.service;

import com.monbudget.controller.BookDialogController;
import com.monbudget.controller.CategoryDialogController;
import com.monbudget.controller.OperationDialogController;
import com.monbudget.dao.BookDao;
import com.monbudget.dao.CategoryDao;
import com.monbudget.dao.OperationDao;
import com.monbudget.dao.RecurrenceRuleDao;
import com.monbudget.model.Book;
import com.monbudget.model.Category;
import com.monbudget.model.Operation;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DialogService {

    private static final Logger logger = LoggerFactory.getLogger(DialogService.class);

    public Book openBookDialog(Window owner, BookDao bookDao) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/monbudget/views/book-dialog-view.fxml"));
            Parent root = loader.load();

            BookDialogController controller = loader.getController();
            controller.initData(bookDao);

            Stage stage = new Stage();
            stage.setTitle("Nouveau livre de comptes");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(owner);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            return controller.getCreatedBook();
        } catch (Exception ex) {
            logger.error("Impossible d'ouvrir le dialogue de création de compte : {}", ex.getMessage(), ex);
            return null;
        }
    }

    public Category openCategoryDialog(Window owner, CategoryDao categoryDao) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/monbudget/views/category-dialog-view.fxml"));
            Parent root = loader.load();

            CategoryDialogController controller = loader.getController();
            controller.initData(categoryDao);

            Stage stage = new Stage();
            stage.setTitle("Nouvelle catégorie");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(owner);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            return controller.getCreatedCategory();
        } catch (Exception ex) {
            logger.error("Impossible d'ouvrir le dialogue de création de catégorie : {}", ex.getMessage(), ex);
            return null;
        }
    }

    public void openOperationDialog(Window owner, Book currentBook, Operation operationToEdit,
                                    CategoryDao categoryDao, OperationDao operationDao, RecurrenceRuleDao recurrenceRuleDao) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/monbudget/views/operation-dialog-view.fxml"));
            Parent root = loader.load();

            OperationDialogController controller = loader.getController();
            controller.initData(currentBook, operationToEdit, categoryDao, operationDao, recurrenceRuleDao);

            Stage stage = new Stage();
            stage.setTitle(operationToEdit == null ? "Nouvelle opération" : "Modifier une opération");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(owner);
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (Exception ex) {
            logger.error("Impossible d'ouvrir le formulaire d'opération : {}", ex.getMessage(), ex);
        }
    }
}