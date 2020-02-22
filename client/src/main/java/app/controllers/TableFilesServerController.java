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

public class TableFilesServerController extends TableViewController {
    @FXML
    private TableView<FileRepresentation> tableFilesServer;
    @FXML
    private TableColumn<FileRepresentation, String> colFileNameServer, colFileSizeServer, colFileDateServer;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        colFileNameServer.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        colFileSizeServer.setCellValueFactory(cellData -> cellData.getValue().lengthProperty());
        colFileDateServer.setCellValueFactory(cellData -> cellData.getValue().dateProperty());
        setRowDeselectListener(tableFilesServer);
    }

    @Override
    public void onClick(MouseEvent mouseEvent) {
        if (getMainController().isDataTransferDisable() || mouseEvent.getClickCount() < 2) return;
        FileRepresentation file = tableFilesServer.getSelectionModel().getSelectedItem();
        if (file == null) return;
        getMainController().addToLog("Downloading request - " + file.getName());
        NetworkForGUIAdapter.getInstance().fileRequest(file.getName());
    }

    public void update(List<FileRepresentation> serverList) {
        ObservableList<FileRepresentation> list = tableFilesServer.getItems();
        Platform.runLater(() -> {
            list.clear();
            list.addAll(serverList);
            getMainController().addToLog("Server list updated");
        });
    }
}
