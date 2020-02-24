package app.controllers.tables;

import app.controllers.SecondLevelController;
import javafx.fxml.Initializable;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import resources.FileRepresentation;
import services.LogService;

public abstract class TableViewController extends SecondLevelController implements Initializable {

    private boolean isOverItem;
    TableView<FileRepresentation> list;

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
            String s = "List field wasn't set in Table List controller";
            LogService.CLIENT.fatal(s);
            throw new RuntimeException(s);
        }
        if (list.getItems().size() == 0) {
            setContextMenuAllFilesDisable(true);
            setContextMenuSingleFileDisable(true);
            return;
        }
        setContextMenuAllFilesDisable(false);
        if (!isOverItem() || getMainController().isDataTransferDisable()) {
            setContextMenuSingleFileDisable(true);
        } else setContextMenuSingleFileDisable(false);
    }

    abstract void setContextMenuSingleFileDisable(boolean status);

    abstract void setContextMenuAllFilesDisable(boolean status);

    public abstract void setContextMenuDisable(boolean status);

    public abstract void fileHandler();

    FileRepresentation getSelectedFile() {
        return list.getSelectionModel().getSelectedItem();
    }

}
