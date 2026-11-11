package com.monbudget.view.cell;

import com.monbudget.model.Operation;
import com.monbudget.model.enums.OperationStatus;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;

public class OperationStatusCell extends TableCell<Operation, OperationStatus> {

    public OperationStatusCell() {
        getStyleClass().add("col-center");
    }

    @Override
    protected void updateItem(OperationStatus status, boolean empty) {
        super.updateItem(status, empty);

        if (empty || status == null) {
            setGraphic(null);
        } else {
            Label badge = new Label(status == OperationStatus.SETTLED ? "RÉALISÉ" : "EN ATTENTE");
            badge.getStyleClass().addAll("badge-status", status == OperationStatus.SETTLED ? "badge-settled" : "badge-planned");
            setGraphic(badge);
        }
    }
}