package com.monbudget.dao;

import com.monbudget.model.Category;
import com.monbudget.model.enums.OperationType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CategoryDaoTest {

    private Connection connection;
    private CategoryDao categoryDao;

    @BeforeEach
    void setUp() throws Exception {
        // Connexion SQLite en mémoire
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");

        // Création du schéma de la table category (simulant les migrations Flyway)
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE category (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    parent_id INTEGER,
                    name TEXT NOT NULL,
                    type TEXT NOT NULL,
                    color_hex TEXT DEFAULT '#3498db',
                    icon_name TEXT DEFAULT 'tag',
                    is_system INTEGER DEFAULT 0,
                    FOREIGN KEY (parent_id) REFERENCES category(id) ON DELETE CASCADE
                )
            """);
        }

        categoryDao = new CategoryDao(connection);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Test
    @DisplayName("Devrait sauvegarder une catégorie principale et la retrouver par ID")
    void testSaveAndFindById() {
        // Given
        Category category = new Category(
                null, null, "Alimentation", OperationType.EXPENSE, "#e74c3c", "shopping", false
        );

        // When
        Category saved = categoryDao.save(category);

        // Then
        assertNotNull(saved.id());
        Optional<Category> found = categoryDao.findById(saved.id());

        assertTrue(found.isPresent());
        assertEquals("Alimentation", found.get().name());
        assertEquals(OperationType.EXPENSE, found.get().type());
        assertEquals("#e74c3c", found.get().colorHex());
        assertFalse(found.get().isSystem());
        assertFalse(found.get().isSubCategory());
    }

    @Test
    @DisplayName("Devrait sauvegarder une sous-catégorie rattachée à un parent")
    void testSaveSubCategory() {
        // Given : création d'une catégorie parente
        Category parent = categoryDao.save(new Category(
                null, null, "Logement", OperationType.EXPENSE, "#3498db", "home", false
        ));

        // When : création de la sous-catégorie
        Category subCategory = new Category(
                null, parent.id(), "Loyer", OperationType.EXPENSE, "#2980b9", "key", false
        );
        Category savedSub = categoryDao.save(subCategory);

        // Then
        assertNotNull(savedSub.id());
        assertEquals(parent.id(), savedSub.parentId());
        assertTrue(savedSub.isSubCategory());
    }

    @Test
    @DisplayName("Devrait récupérer toutes les catégories triées par ordre alphabétique")
    void testFindAll() {
        // Given
        categoryDao.save(new Category(null, null, "Transport", OperationType.EXPENSE, "#f1c40f", "car", false));
        categoryDao.save(new Category(null, null, "Salaire", OperationType.INCOME, "#2ecc71", "briefcase", true));
        categoryDao.save(new Category(null, null, "Alimentation", OperationType.EXPENSE, "#e74c3c", "food", false));

        // When
        List<Category> allCategories = categoryDao.findAll();

        // Then
        assertEquals(3, allCategories.size());
        // Vérification du tri alphabétique par nom (ASC)
        assertEquals("Alimentation", allCategories.get(0).name());
        assertEquals("Salaire", allCategories.get(1).name());
        assertEquals("Transport", allCategories.get(2).name());
    }

    @Test
    @DisplayName("Devrait filtrer correctement les catégories par type d'opération")
    void testFindByType() {
        // Given
        categoryDao.save(new Category(null, null, "Restaurant", OperationType.EXPENSE, "#e74c3c", "food", false));
        categoryDao.save(new Category(null, null, "Freelance", OperationType.INCOME, "#2ecc71", "laptop", false));
        categoryDao.save(new Category(null, null, "Virement Interne", OperationType.TRANSFER, "#95a5a6", "swap", false));
        categoryDao.save(new Category(null, null, "Courses", OperationType.EXPENSE, "#e74c3c", "basket", false));

        // When
        List<Category> expenses = categoryDao.findByType(OperationType.EXPENSE);
        List<Category> incomes = categoryDao.findByType(OperationType.INCOME);
        List<Category> transfers = categoryDao.findByType(OperationType.TRANSFER);

        // Then
        assertEquals(2, expenses.size());
        assertTrue(expenses.stream().allMatch(c -> c.type() == OperationType.EXPENSE));
        assertEquals("Courses", expenses.get(0).name()); // Trié par nom ASC
        assertEquals("Restaurant", expenses.get(1).name());

        assertEquals(1, incomes.size());
        assertEquals(OperationType.INCOME, incomes.getFirst().type());

        assertEquals(1, transfers.size());
        assertEquals(OperationType.TRANSFER, transfers.getFirst().type());
    }
}