package com.monbudget.view.cell;

import com.monbudget.model.Operation;
import com.monbudget.model.enums.OperationStatus;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import java.util.function.Consumer;

public class OperationActionCell extends TableCell<Operation, Void> {
    private final Button pointButton = new Button("Pointer");

    public OperationActionCell(Consumer<Operation> onPointAction) {
        getStyleClass().add("col-center");
        pointButton.getStyleClass().addAll("btn", "btn-default", "btn-sm");
        pointButton.setOnAction(e -> {
            Operation op = getTableView().getItems().get(getIndex());
            if (op != null) {
                onPointAction.accept(op);
            }
        });
    }

    @Override
    protected void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
            setGraphic(null);
        } else {
            Operation op = getTableView().getItems().get(getIndex());
            pointButton.setDisable(op == null || op.status() == OperationStatus.SETTLED);
            setGraphic(pointButton);
        }
    }
}