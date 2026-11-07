package com.monbudget.dao;

import com.monbudget.model.RecurrenceRule;
import com.monbudget.util.DateHelper;

import java.sql.*;
import java.util.Objects;

public class RecurrenceRuleDao {
    private final Connection connection;

    public RecurrenceRuleDao(Connection connection) {
        this.connection = Objects.requireNonNull(connection, "La connexion ne doit pas être null.");
    }

    public RecurrenceRule save(RecurrenceRule rule) {
        String sql = """
            INSERT INTO recurrence_rule (frequency, interval, start_date, end_date, day_of_month)
            VALUES (?, ?, ?, ?, ?)
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, rule.frequency().name());
            stmt.setInt(2, rule.interval());

            stmt.setString(3, DateHelper.toSqlDate(rule.startDate()));
            stmt.setString(4, DateHelper.toSqlDate(rule.endDate()));

            if (rule.dayOfMonth() != null) {
                stmt.setInt(5, rule.dayOfMonth());
            } else {
                stmt.setNull(5, Types.INTEGER);
            }

            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    long generatedId = keys.getLong(1);
                    return new RecurrenceRule(
                            generatedId,
                            rule.frequency(),
                            rule.interval(),
                            rule.startDate(),
                            rule.endDate(),
                            rule.dayOfMonth()
                    );
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la sauvegarde de la règle de récurrence", e);
        }
        throw new RuntimeException("Échec de la génération de l'ID pour RecurrenceRule");
    }
}