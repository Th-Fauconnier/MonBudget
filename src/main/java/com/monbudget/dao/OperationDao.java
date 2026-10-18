package com.monbudget.dao;

import com.monbudget.model.Operation;
import com.monbudget.model.enums.OperationStatus;
import com.monbudget.model.enums.OperationType;
import com.monbudget.util.MoneyHelper;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class OperationDao {

    private final Connection connection;

    public OperationDao(Connection connection) {
        this.connection = Objects.requireNonNull(connection, "La connexion ne doit pas être null.");
    }

    public Operation save(Operation operation) {
        String sql = """
            INSERT INTO operation (
                book_id, category_id, payment_method_id, recurrence_rule_id,
                label, amount, operation_type, status, operation_date, settled_at,
                notes, transfer_operation_id
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, operation.bookId());
            setNullableLong(stmt, 2, operation.categoryId());
            setNullableLong(stmt, 3, operation.paymentMethodId());
            setNullableLong(stmt, 4, operation.recurrenceRuleId());
            stmt.setString(5, operation.label());
            stmt.setLong(6, MoneyHelper.toCents(operation.amount()));
            stmt.setString(7, operation.operationType().name());
            stmt.setString(8, operation.status().name());
            stmt.setString(9, operation.operationDate().toString());
            stmt.setString(10, operation.settledAt() != null ? operation.settledAt().toString() : null);
            stmt.setString(11, operation.notes());
            setNullableLong(stmt, 12, operation.transferOperationId());

            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    long id = generatedKeys.getLong(1);
                    return findById(id).orElseThrow();
                } else {
                    throw new SQLException("Échec de création de l'opération, aucun ID retourné.");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'enregistrement de l'opération : " + operation.label(), e);
        }
    }

    public Optional<Operation> findById(long id) {
        String sql = "SELECT * FROM operation WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToOperation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération de l'opération id=" + id, e);
        }
        return Optional.empty();
    }

    public List<Operation> findByBookAndDateRange(long bookId, LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT * FROM operation
            WHERE book_id = ?
              AND operation_date >= ?
              AND operation_date <= ?
            ORDER BY operation_date DESC, id DESC
        """;

        List<Operation> operations = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, bookId);
            stmt.setString(2, startDate.toString());
            stmt.setString(3, endDate.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    operations.add(mapRowToOperation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du filtrage des opérations pour le livre=" + bookId, e);
        }
        return operations;
    }

    public void update(Operation operation) {
        String sql = """
            UPDATE operation
            SET book_id = ?, category_id = ?, payment_method_id = ?, recurrence_rule_id = ?,
                label = ?, amount = ?, operation_type = ?, status = ?,
                operation_date = ?, settled_at = ?, notes = ?, transfer_operation_id = ?,
                updated_at = DATETIME('now')
            WHERE id = ?
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, operation.bookId());
            setNullableLong(stmt, 2, operation.categoryId());
            setNullableLong(stmt, 3, operation.paymentMethodId());
            setNullableLong(stmt, 4, operation.recurrenceRuleId());
            stmt.setString(5, operation.label());
            stmt.setLong(6, MoneyHelper.toCents(operation.amount()));
            stmt.setString(7, operation.operationType().name());
            stmt.setString(8, operation.status().name());
            stmt.setString(9, operation.operationDate().toString());
            stmt.setString(10, operation.settledAt() != null ? operation.settledAt().toString() : null);
            stmt.setString(11, operation.notes());
            setNullableLong(stmt, 12, operation.transferOperationId());
            stmt.setLong(13, operation.id());

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la mise à jour de l'opération id=" + operation.id(), e);
        }
    }

    public void delete(long id) {
        String sql = "DELETE FROM operation WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression de l'opération id=" + id, e);
        }
    }

    private Operation mapRowToOperation(ResultSet rs) throws SQLException {
        Long categoryId = rs.getObject("category_id") != null ? rs.getLong("category_id") : null;
        Long paymentMethodId = rs.getObject("payment_method_id") != null ? rs.getLong("payment_method_id") : null;
        Long recurrenceRuleId = rs.getObject("recurrence_rule_id") != null ? rs.getLong("recurrence_rule_id") : null;
        Long transferOpId = rs.getObject("transfer_operation_id") != null ? rs.getLong("transfer_operation_id") : null;

        // 1. Date métier pure (ex: 2026-10-18)
        LocalDate operationDate = LocalDate.parse(rs.getString("operation_date").substring(0, 10));

        // 2. Date de pointage (optionnelle)
        String settledAtRaw = rs.getString("settled_at");
        LocalDate settledAt = (settledAtRaw != null && !settledAtRaw.isBlank())
                ? LocalDate.parse(settledAtRaw.substring(0, 10))
                : null;

        // 3. Horodatages d'audit robustes (gèrent "YYYY-MM-DD" et "YYYY-MM-DD HH:mm:ss")
        LocalDateTime createdAt = parseTimestamp(rs.getString("created_at"));
        LocalDateTime updatedAt = parseTimestamp(rs.getString("updated_at"));

        return new Operation(
                rs.getLong("id"),
                rs.getLong("book_id"),
                categoryId,
                paymentMethodId,
                recurrenceRuleId,
                rs.getString("label"),
                MoneyHelper.fromCents(rs.getLong("amount")),
                OperationType.valueOf(rs.getString("operation_type")),
                OperationStatus.valueOf(rs.getString("status")),
                LocalDate.parse(rs.getString("operation_date")),
                settledAt,
                rs.getString("notes"),
                transferOpId,
                createdAt,
                updatedAt
        );
    }

    // Méthode de parsing tolérante pour SQLite
    private LocalDateTime parseTimestamp(String raw) {
        if (raw == null || raw.isBlank()) {
            return LocalDateTime.now();
        }
        // Si SQLite a stocké "2026-10-18", on complète avec minuit
        if (raw.length() == 10) {
            return LocalDate.parse(raw).atStartOfDay();
        }
        // Gère le format SQLite standard "YYYY-MM-DD HH:mm:ss"
        return LocalDateTime.parse(raw.replace(" ", "T"));
    }

    private void setNullableLong(PreparedStatement stmt, int index, Long value) throws SQLException {
        if (value != null) {
            stmt.setLong(index, value);
        } else {
            stmt.setNull(index, Types.INTEGER);
        }
    }
}