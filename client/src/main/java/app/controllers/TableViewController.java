package app.controllers;

import javafx.fxml.Initializable;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;
import resources.FileRepresentation;

public abstract class TableViewController implements Initializable {

    void setRowDeselectListener(TableView<FileRepresentation> tableView) {
        tableView.setRowFactory(t -> {
            final TableRow<FileRepresentation> row = new TableRow<>();
            row.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                final int index = row.getIndex();
                if (index >= tableView.getItems().size()) {
                    tableView.getSelectionModel().clearSelection();
                    event.consume();
                }
            });
            return row;
        });
    }

    public abstract void onClick(MouseEvent mouseEvent);
}
