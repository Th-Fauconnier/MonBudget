package com.monbudget.dao;

import com.monbudget.model.Category;
import com.monbudget.model.enums.OperationType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class CategoryDao {

    private final Connection connection;

    public CategoryDao(Connection connection) {
        this.connection = Objects.requireNonNull(connection, "La connexion ne doit pas être null.");
    }

    public Category save(Category category) {
        String sql = """
            INSERT INTO category (parent_id, name, type, color_hex, icon_name, is_system)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (category.parentId() != null) {
                stmt.setLong(1, category.parentId());
            } else {
                stmt.setNull(1, Types.INTEGER);
            }
            stmt.setString(2, category.name());
            stmt.setString(3, category.type().name());
            stmt.setString(4, category.colorHex());
            stmt.setString(5, category.iconName());
            stmt.setInt(6, category.isSystem() ? 1 : 0);

            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    long id = generatedKeys.getLong(1);
                    return findById(id).orElseThrow();
                } else {
                    throw new SQLException("Échec de création de la catégorie.");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'enregistrement de la catégorie : " + category.name(), e);
        }
    }

    public Optional<Category> findById(long id) {
        String sql = "SELECT * FROM category WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToCategory(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recherche de la catégorie id=" + id, e);
        }
        return Optional.empty();
    }

    public List<Category> findAll() {
        String sql = "SELECT * FROM category ORDER BY name ASC";
        List<Category> categories = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                categories.add(mapRowToCategory(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des catégories", e);
        }
        return categories;
    }

    public List<Category> findByType(OperationType type) {
        String sql = "SELECT * FROM category WHERE type = ? ORDER BY name ASC";
        List<Category> categories = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, type.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    categories.add(mapRowToCategory(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du filtrage des catégories par type=" + type, e);
        }
        return categories;
    }

    private Category mapRowToCategory(ResultSet rs) throws SQLException {
        Long parentId = rs.getObject("parent_id") != null ? rs.getLong("parent_id") : null;
        return new Category(
                rs.getLong("id"),
                parentId,
                rs.getString("name"),
                OperationType.valueOf(rs.getString("type")),
                rs.getString("color_hex"),
                rs.getString("icon_name"),
                rs.getInt("is_system") == 1
        );
    }
}