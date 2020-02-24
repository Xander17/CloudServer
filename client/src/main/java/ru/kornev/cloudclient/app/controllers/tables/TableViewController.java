package ru.kornev.cloudclient.app.controllers.tables;

import javafx.fxml.Initializable;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import ru.kornev.cloudclient.app.controllers.SecondLevelController;
import ru.kornev.cloudclient.services.LogService;
import ru.kornev.cloudcommon.resources.FileRepresentation;

public abstract class TableViewController extends SecondLevelController implements Initializable {

    private TableView<FileRepresentation> list;
    private boolean isOverItem;

    void setRowDeselectListener(TableView<FileRepresentation> tableView) {
        tableView.setRowFactory(t -> {
            final TableRow<FileRepresentation> row = new TableRow<>();
            row.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                final int index = row.getIndex();
                if (index >= tableView.getItems().size()) {
                    tableView.getSelectionModel().clearSelection();
                    isOverItem = false;
                    event.consume();
                } else {
                    isOverItem = true;
                }
            });
            return row;
        });
    }

    boolean isOverItem() {
        return isOverItem;
    }

    public void onClick(MouseEvent mouseEvent) {
        if (mouseEvent.getButton() == MouseButton.SECONDARY) contextMenuHandler();
        else if (mouseEvent.getButton() == MouseButton.PRIMARY) {
            if (getMainController().isDataTransferDisable() || mouseEvent.getClickCount() < 2) return;
            fileHandler();
        }
    }

    void contextMenuHandler() {
        if (list == null) {
            String s = "List field wasn't set in Table List controller. Use setList()";
            LogService.CLIENT.fatal(s);
            throw new RuntimeException(s);
        }
        if (list.getItems().size() == 0) {
            setContextMenuAllFilesDisable(true);
            setContextMenuSingleFileDisable(true);
            return;
        }
        if (getMainController().isDataTransferDisable()) {
            setContextMenuDisable(true);
        } else if (!isOverItem()) {
            setContextMenuDisable(false);
            setContextMenuSingleFileDisable(true);
        } else {
            setContextMenuDisable(false);
        }
    }

    abstract void setContextMenuSingleFileDisable(boolean status);

    abstract void setContextMenuAllFilesDisable(boolean status);

    public abstract void setContextMenuDisable(boolean status);

    public abstract void fileHandler();

    FileRepresentation getSelectedFile() {
        return list.getSelectionModel().getSelectedItem();
    }

    public void setList(TableView<FileRepresentation> list) {
        this.list = list;
    }
}
