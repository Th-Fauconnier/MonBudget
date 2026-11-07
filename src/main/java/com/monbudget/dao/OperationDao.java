package com.monbudget.dao;

import com.monbudget.model.Operation;
import com.monbudget.model.enums.OperationStatus;
import com.monbudget.model.enums.OperationType;
import com.monbudget.util.DateHelper;
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

            if (operation.categoryId() != null) stmt.setLong(2, operation.categoryId());
            else stmt.setNull(2, Types.INTEGER);

            if (operation.paymentMethodId() != null) stmt.setLong(3, operation.paymentMethodId());
            else stmt.setNull(3, Types.INTEGER);

            if (operation.recurrenceRuleId() != null) {
                stmt.setLong(4, operation.recurrenceRuleId());
            } else {
                stmt.setNull(4, Types.INTEGER);
            }

            stmt.setString(5, operation.label());
            stmt.setLong(6, MoneyHelper.toCents(operation.amount()));
            stmt.setString(7, operation.operationType().name());
            stmt.setString(8, operation.status().name());

            stmt.setString(9, DateHelper.toSqlDate(operation.operationDate()));
            stmt.setString(10, DateHelper.toSqlDate(operation.settledAt()));

            stmt.setString(11, operation.notes());

            if (operation.transferOperationId() != null) stmt.setLong(12, operation.transferOperationId());
            else stmt.setNull(12, Types.INTEGER);

            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    long generatedId = keys.getLong(1);
                    return findById(generatedId).orElseThrow(() ->
                            new RuntimeException("Erreur lors de la relecture de l'opération créée")
                    );
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la sauvegarde de l'opération", e);
        }
        throw new RuntimeException("Échec de la génération de l'ID pour Operation");
    }

    public Optional<Operation> findById(long id) {
        String sql = "SELECT * FROM operation WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToOperation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération de l'opération id=" + id, e);
        }
        return Optional.empty();
    }

    public List<Operation> findByBookAndDateRange(long bookId, LocalDate start, LocalDate end) {
        String sql = """
            SELECT * FROM operation 
            WHERE book_id = ? AND operation_date BETWEEN ? AND ?
            ORDER BY operation_date DESC, id DESC
        """;
        List<Operation> operations = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, bookId);
            stmt.setString(2, DateHelper.toSqlDate(start));
            stmt.setString(3, DateHelper.toSqlDate(end));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    operations.add(mapResultSetToOperation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du filtrage des opérations pour le livre=" + bookId, e);
        }
        return operations;
    }

    public void update(Operation operation) {
        String sql = """
            UPDATE operation SET
                category_id = ?, payment_method_id = ?, recurrence_rule_id = ?,
                label = ?, amount = ?, operation_type = ?, status = ?,
                operation_date = ?, settled_at = ?, notes = ?, transfer_operation_id = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            if (operation.categoryId() != null) stmt.setLong(1, operation.categoryId());
            else stmt.setNull(1, Types.INTEGER);

            if (operation.paymentMethodId() != null) stmt.setLong(2, operation.paymentMethodId());
            else stmt.setNull(2, Types.INTEGER);

            if (operation.recurrenceRuleId() != null) {
                stmt.setLong(3, operation.recurrenceRuleId());
            } else {
                stmt.setNull(3, Types.INTEGER);
            }

            stmt.setString(4, operation.label());
            stmt.setLong(5, MoneyHelper.toCents(operation.amount()));
            stmt.setString(6, operation.operationType().name());
            stmt.setString(7, operation.status().name());
            stmt.setString(8, DateHelper.toSqlDate(operation.operationDate()));
            stmt.setString(9, DateHelper.toSqlDate(operation.settledAt()));
            stmt.setString(10, operation.notes());

            if (operation.transferOperationId() != null) stmt.setLong(11, operation.transferOperationId());
            else stmt.setNull(11, Types.INTEGER);

            stmt.setLong(12, operation.id());

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

    private Operation mapResultSetToOperation(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        long bookId = rs.getLong("book_id");

        Long categoryId = rs.getLong("category_id");
        if (rs.wasNull()) categoryId = null;

        Long paymentMethodId = rs.getLong("payment_method_id");
        if (rs.wasNull()) paymentMethodId = null;

        Long recurrenceRuleId = rs.getLong("recurrence_rule_id");
        if (rs.wasNull()) recurrenceRuleId = null;

        String label = rs.getString("label");
        long amountInCents = rs.getLong("amount");

        OperationType type = OperationType.valueOf(rs.getString("operation_type"));
        OperationStatus status = OperationStatus.valueOf(rs.getString("status"));

        LocalDate operationDate = DateHelper.parseLocalDate(rs.getString("operation_date"));
        LocalDate settledAt = DateHelper.parseLocalDate(rs.getString("settled_at"));

        LocalDateTime createdAt = DateHelper.parseLocalDateTime(rs.getString("created_at"));
        LocalDateTime updatedAt = DateHelper.parseLocalDateTime(rs.getString("updated_at"));

        String notes = rs.getString("notes");

        Long transferOpId = rs.getLong("transfer_operation_id");
        if (rs.wasNull()) transferOpId = null;

        return new Operation(
                id, bookId, categoryId, paymentMethodId, recurrenceRuleId,
                label, MoneyHelper.fromCents(amountInCents),
                type, status, operationDate, settledAt, notes,
                transferOpId, createdAt, updatedAt
        );
    }
}