package ru.kornev.cloudclient.app.controllers.tables;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import ru.kornev.cloudcommon.resources.FileRepresentation;
import ru.kornev.cloudnetwork.network.NetworkService;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class TableFilesServerController extends TableViewController {
    @FXML
    private TableView<FileRepresentation> tableFilesServer;
    @FXML
    private TableColumn<FileRepresentation, String> colFileNameServer, colFileSizeServer, colFileDateServer;
    @FXML
    private MenuItem mDownloadFile, mDeleteFile, mDownloadAll, mRefresh;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        colFileNameServer.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        colFileSizeServer.setCellValueFactory(cellData -> cellData.getValue().lengthProperty());
        colFileDateServer.setCellValueFactory(cellData -> cellData.getValue().dateProperty());
        setRowDeselectListener(tableFilesServer);
        setList(tableFilesServer);
    }

    public void update(List<FileRepresentation> serverList) {
        ObservableList<FileRepresentation> list = tableFilesServer.getItems();
        if (serverList.size() == 0) {
            getMainController().onServerMessage("No files on server");
            Platform.runLater(list::clear);
        } else {
            Platform.runLater(() -> {
                list.clear();
                list.addAll(serverList);
                getMainController().onServerMessage("Server list updated");
            });
        }
    }

    @Override
    void setContextMenuSingleFileDisable(boolean status) {
        mDownloadFile.setDisable(status);
        mDeleteFile.setDisable(status);
    }

    @Override
    void setContextMenuAllFilesDisable(boolean status) {
        mDownloadAll.setDisable(status);
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
        NetworkService.getInstance().requestFileFromServer(file.getName());
    }

    public void delete() {
        FileRepresentation file = getSelectedFile();
        if (file == null) return;
        NetworkService.getInstance().deleteFileFromServer(file);
        getMainController().requestServerList();
    }

    public void downloadAllRequest() {
        getMainController().requestAllFilesFromServer();
    }

    public void refresh() {
        getMainController().requestServerList();
    }
}
