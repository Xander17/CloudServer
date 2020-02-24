package ru.kornev.cloudclient.app.controllers.tables;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import ru.kornev.cloudclient.services.NetworkForGUIAdapter;
import ru.kornev.cloudcommon.resources.FileRepresentation;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class TableFilesLocalController extends TableViewController {
    @FXML
    private TableView<FileRepresentation> tableFilesLocal;
    @FXML
    private TableColumn<FileRepresentation, String> colFileNameLocal, colFileSizeLocal, colFileDateLocal;
    @FXML
    private MenuItem mUploadFile, mDeleteFile, mUploadAll, mRefresh;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        colFileNameLocal.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        colFileSizeLocal.setCellValueFactory(cellData -> cellData.getValue().lengthProperty());
        colFileDateLocal.setCellValueFactory(cellData -> cellData.getValue().dateProperty());
        setRowDeselectListener(tableFilesLocal);
        setList(tableFilesLocal);
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
            });
        }
    }

    @Override
    void setContextMenuSingleFileDisable(boolean status) {
        mUploadFile.setDisable(status);
        mDeleteFile.setDisable(status);
    }

    @Override
    void setContextMenuAllFilesDisable(boolean status) {
        mUploadAll.setDisable(status);
    }

    @Override
    public void setContextMenuDisable(boolean status) {
        setContextMenuSingleFileDisable(status);
        setContextMenuAllFilesDisable(status);
        mRefresh.setDisable(status);
    }

    @Override
    public void fileHandler() {
        FileRepresentation file = getSelectedFile();
        if (file == null) return;
        getMainController().uploadFile(file.getName());
    }

    public void delete() {
        FileRepresentation file = getSelectedFile();
        if (file == null) return;
        NetworkForGUIAdapter.getInstance().deleteLocalFile(file);
        getMainController().refreshClientList();
    }

    public void uploadAll() {
        getMainController().uploadAllFiles();
    }

    public void refresh() {
        getMainController().refreshClientList();
    }
}
