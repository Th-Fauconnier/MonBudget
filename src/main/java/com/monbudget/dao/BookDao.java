package com.monbudget.dao;

import com.monbudget.model.Book;
import com.monbudget.util.MoneyHelper;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class BookDao {

    private final Connection connection;

    public BookDao(Connection connection) {
        this.connection = Objects.requireNonNull(connection, "La connexion ne doit pas être null.");
    }

    public Book save(Book book) {
        String sql = """
            INSERT INTO book (name, description, currency, initial_balance, color_hex, icon_name, is_archived)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, book.name());
            stmt.setString(2, book.description());
            stmt.setString(3, book.currency() != null ? book.currency() : "EUR");
            stmt.setLong(4, MoneyHelper.toCents(book.initialBalance()));
            stmt.setString(5, book.colorHex() != null ? book.colorHex() : "#3498db");
            stmt.setString(6, book.iconName() != null ? book.iconName() : "wallet");
            stmt.setInt(7, book.isArchived() ? 1 : 0);

            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    long id = generatedKeys.getLong(1);
                    return findById(id).orElseThrow();
                } else {
                    throw new SQLException("Échec de création du livre, aucun ID généré.");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'enregistrement du livre : " + book.name(), e);
        }
    }

    public Optional<Book> findById(long id) {
        String sql = "SELECT * FROM book WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToBook(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recherche du livre id=" + id, e);
        }
        return Optional.empty();
    }

    public List<Book> findAllActive() {
        String sql = "SELECT * FROM book WHERE is_archived = 0 ORDER BY name ASC";
        List<Book> books = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                books.add(mapRowToBook(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des livres actifs", e);
        }
        return books;
    }

    public void update(Book book) {
        String sql = """
            UPDATE book
            SET name = ?, description = ?, currency = ?, initial_balance = ?,
                color_hex = ?, icon_name = ?, is_archived = ?, updated_at = DATETIME('now')
            WHERE id = ?
        """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, book.name());
            stmt.setString(2, book.description());
            stmt.setString(3, book.currency());
            stmt.setLong(4, MoneyHelper.toCents(book.initialBalance()));
            stmt.setString(5, book.colorHex());
            stmt.setString(6, book.iconName());
            stmt.setInt(7, book.isArchived() ? 1 : 0);
            stmt.setLong(8, book.id());

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la mise à jour du livre id=" + book.id(), e);
        }
    }

    public void delete(long id) {
        String sql = "DELETE FROM book WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression du livre id=" + id, e);
        }
    }

    private Book mapRowToBook(ResultSet rs) throws SQLException {
        return new Book(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("currency"),
                MoneyHelper.fromCents(rs.getLong("initial_balance")),
                rs.getString("color_hex"),
                rs.getString("icon_name"),
                rs.getInt("is_archived") == 1,
                parseDateTime(rs.getString("created_at")),
                parseDateTime(rs.getString("updated_at"))
        );
    }

    private LocalDateTime parseDateTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return LocalDateTime.now();
        }
        // Si la chaîne fait exactement 10 caractères (YYYY-MM-DD), pas d'heure disponible
        if (raw.length() == 10) {
            return LocalDate.parse(raw).atStartOfDay();
        }
        return LocalDateTime.parse(raw.replace(" ", "T"));
    }
}