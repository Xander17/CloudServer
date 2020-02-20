package app.controllers;

import app.MainController;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;
import resources.FileRepresentation;

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

    public void onClick(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() < 2) return;
        FileRepresentation file = tableFilesLocal.getSelectionModel().getSelectedItem();
        if (file == null) return;
        MainController.getInstance().addToLog("Uploading file - " + file.getName());
        // TODO: 20.02.2020 сделать что-то, переместить может.. т.к. все методы из другого контроллера
        new Thread(() -> {
            MainController mainController = MainController.getInstance();
            mainController.setButtonsDisable(true);
            mainController.getDataHandler().uploadFile(file.getName());
            mainController.refreshServerList();
            mainController.setButtonsDisable(false);
        }).start();
    }

    public void update() {
        List<FileRepresentation> files = MainController.getInstance().getDataHandler().getClientFilesRepList();
        ObservableList<FileRepresentation> list = tableFilesLocal.getItems();
        Platform.runLater(() -> {
            list.clear();
            list.addAll(files);
            MainController.getInstance().addToLog("Client list updated");
        });
    }
}
