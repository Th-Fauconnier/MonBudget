package com.monbudget.dao;

import com.monbudget.model.RecurrenceRule;
import java.sql.Connection;

public class RecurrenceRuleDao {

    private final Connection connection;

    public RecurrenceRuleDao(Connection connection) {
        this.connection = connection;
    }

    public RecurrenceRule save(RecurrenceRule rule) {
        // Squelette pour le TDD : le test va compiler mais échouer à l'exécution
        throw new UnsupportedOperationException("Méthode non implémentée");
    }
}