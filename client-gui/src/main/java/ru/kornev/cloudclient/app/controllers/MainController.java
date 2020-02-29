package ru.kornev.cloudclient.app.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import ru.kornev.cloudclient.app.controllers.tables.TableFilesLocalController;
import ru.kornev.cloudclient.app.controllers.tables.TableFilesServerController;
import ru.kornev.cloudcommon.resources.FileRepresentation;
import ru.kornev.cloudcommon.services.transfer.Progress;
import ru.kornev.cloudnetwork.network.Network;
import ru.kornev.cloudnetwork.network.NetworkService;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML
    private BorderPane paneMainView;
    @FXML
    private Button btnSendAllToServer, btnGetFilesList, btnReceiveAllFromServer;
    @FXML
    private TextArea taLogs;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label lblProgress;
    @FXML
    private TableFilesLocalController tableFilesLocalController;
    @FXML
    private TableFilesServerController tableFilesServerController;
    @FXML
    private LoginWindowController loginWindowController;

    private Network networkThread;
    private boolean loginState;
    private boolean dataTransferDisable;
    private SimpleDateFormat dateFormat;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        dateFormat = new SimpleDateFormat("[HH:mm:ss]");
        loginState = true;
        dataTransferDisable = true;
        setupControllers();
        setupListeners();
        runServer();
    }

    private void setupControllers() {
        tableFilesServerController.setMainController(this);
        tableFilesLocalController.setMainController(this);
        loginWindowController.setMainController(this);
    }

    private void runServer() {
        networkThread = new Network();
        networkThread.start();
    }

    private void setupListeners() {
        NetworkService service = NetworkService.getInstance();
        service.setOnConnectionEstablished(this::onConnectionEstablished);
        service.setOnRegistrationSuccess(this::onRegistrationSuccess);
        service.setOnAuthSuccess(this::onAuthorizationSuccess);
        service.setOnAuthRegError(this::onRegAuthError);
        service.setOnUploadFinish(this::onUploadComplete);
        service.setOnDownloadStart(this::onDownloadStart);
        service.setOnDownloadFinish(this::onDownloadComplete);
        service.setOnUploadFileNotExists(this::onUploadFileNotExists);
        service.setOnFileListDownloadFinish(this::onServerFilesListDownloadFinish);
        service.setOnProgressPropertyRegister(this::onProgressPropertyRegister);
        service.setOnServerMessage(this::onServerMessage);
    }

    public void onConnectionEstablished() {
        setLoginDisable(false);
    }

    private void setLoginDisable(boolean status) {
        loginWindowController.setLoginDisable(status);
    }

    private void onRegistrationSuccess() {
        loginWindowController.setRegSuccess();
    }

    private void onRegAuthError(int errorCode) {
        loginWindowController.setRegAuthError(errorCode);
    }

    private void onAuthorizationSuccess() {
        if (!loginState) return;
        setLoginState(false);
        dataTransferDisable = false;
        refreshFilesLists();
    }

    private void onUploadComplete() {
        requestServerList();
        setGUIActionsDisable(false);
    }

    private void onDownloadStart() {
        setGUIActionsDisable(true);
    }

    private void onDownloadComplete() {
        refreshClientList();
        setGUIActionsDisable(false);
    }

    private void onUploadFileNotExists() {
        refreshClientList();
    }

    private void onServerFilesListDownloadFinish(List<FileRepresentation> serverList) {
        tableFilesServerController.update(serverList);
        setGUIActionsDisable(false);
    }

    private void onProgressPropertyRegister(Progress progress) {
        Platform.runLater(() -> {
            progressBar.progressProperty().bind(progress.progressProperty());
            lblProgress.textProperty().bind(progress.stringProgressProperty());
        });
    }

    public void onServerMessage(String msg) {
        Platform.runLater(() -> taLogs.appendText(dateFormat.format(new Date()) + " " + msg + "\n"));
    }

    private void setLoginState(boolean status) {
        loginWindowController.setLoginState(false);
        setElementsDisable(status);
        setElementsVisible(!status);
        loginState = status;
    }

    private void setElementsDisable(boolean status) {
        Platform.runLater(() -> paneMainView.setDisable(status));
    }

    private void setElementsVisible(boolean status) {
        Platform.runLater(() -> paneMainView.setVisible(status));
    }

    private void setGUIActionsDisable(boolean status) {
        dataTransferDisable = status;
        Platform.runLater(() -> {
            btnSendAllToServer.setDisable(status);
            btnReceiveAllFromServer.setDisable(status);
            btnGetFilesList.setDisable(status);
            tableFilesLocalController.setContextMenuDisable(status);
            tableFilesServerController.setContextMenuDisable(status);
        });
    }

    public void uploadAllFiles() {
        uploadStart();
        NetworkService.getInstance().uploadAllFiles();
    }

    public void uploadFile(FileRepresentation file) {
        uploadStart();
        NetworkService.getInstance().uploadFile(file);
    }

    private void uploadStart() {
        setGUIActionsDisable(true);
    }

    public void requestAllFilesFromServer() {
        setGUIActionsDisable(true);
        NetworkService.getInstance().requestAllFilesFromServer();
    }

    public void refreshFilesLists() {
        btnGetFilesList.setDisable(true);
        setGUIActionsDisable(true);
        refreshClientList();
        requestServerList();
        btnGetFilesList.setDisable(false);
    }

    public void refreshClientList() {
        tableFilesLocalController.update();
    }

    public void requestServerList() {
        NetworkService.getInstance().requestServerFilesList();
    }

    public void exitApp() {
        networkThread.interrupt();
        Platform.exit();
    }

    public boolean isDataTransferDisable() {
        return dataTransferDisable;
    }
}