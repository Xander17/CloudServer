package app.controllers;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;
import resources.FileRepresentation;
import services.NetworkForGUIAdapter;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class TableFilesLocalController extends TableViewController {
    @FXML
    private TableView<FileRepresentation> tableFilesLocal;
    @FXML
    private TableColumn<FileRepresentation, String> colFileNameLocal, colFileSizeLocal, colFileDateLocal;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        colFileNameLocal.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        colFileSizeLocal.setCellValueFactory(cellData -> cellData.getValue().lengthProperty());
        colFileDateLocal.setCellValueFactory(cellData -> cellData.getValue().dateProperty());
        setRowDeselectListener(tableFilesLocal);
    }

    @Override
    public void onClick(MouseEvent mouseEvent) {
        if (getMainController().isDataTransferDisable() || mouseEvent.getClickCount() < 2) return;
        FileRepresentation file = tableFilesLocal.getSelectionModel().getSelectedItem();
        if (file == null) return;
        getMainController().uploadFile(file.getName());
    }

    public void update() {
        List<FileRepresentation> files = NetworkForGUIAdapter.getInstance().getClientFilesList();
        ObservableList<FileRepresentation> list = tableFilesLocal.getItems();
        if (files.size() == 0) {
            getMainController().addToLog("No files in local directory");
            Platform.runLater(list::clear);
        } else {
            Platform.runLater(() -> {
                list.clear();
                list.addAll(files);
                getMainController().addToLog("Client list updated");
            });
        }
    }
}
