package app;

import app.controllers.LoginWindowController;
import app.controllers.TableFilesLocalController;
import app.controllers.TableFilesServerController;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import resources.FileRepresentation;
import services.GUIForNetworkAdapter;
import services.LogService;
import services.NetworkForGUIAdapter;
import services.NetworkThread;

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
    private VBox vBoxListClient, vBoxListServer;
    @FXML
    private TableFilesLocalController tableFilesLocalController;
    @FXML
    private TableFilesServerController tableFilesServerController;
    @FXML
    private LoginWindowController loginWindowController;

    private NetworkThread networkThread;
    private boolean loginState;
    private boolean dataTransferDisable;
    private SimpleDateFormat dateFormat;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        dateFormat = new SimpleDateFormat("[HH:mm:ss]");
        loginState = true;
        dataTransferDisable = true;
        setupControllers();
        new GUIForNetworkAdapter(this);
        runServerListener();
    }

    private void setupControllers(){
        tableFilesServerController.setMainController(this);
        tableFilesLocalController.setMainController(this);
        loginWindowController.setMainController(this);
    }

    private void runServerListener() {
        networkThread = new NetworkThread();
        networkThread.start();
    }

    public void setLoginDisable(boolean status) {
        loginWindowController.setLoginDisable(status);
    }

    public void setRegistrationSuccess() {
        loginWindowController.setRegSuccess();
    }

    public void setRegAuthError(int errorCode) {
        loginWindowController.setRegAuthError(errorCode);
    }

    public void setAuthorizationSuccess() {
        if (!loginState) return;
        setLoginState(false);
        dataTransferDisable = false;
        refreshFilesLists();
    }

    private void setLoginState(boolean status) {
        loginWindowController.setLoginState(false);
        setElementsDisable(status);
        setElementsVisible(!status);
        loginState = status;
    }

    public void setDownloadComplete() {
        refreshFilesLists(true);
        setButtonsDisable(false);
    }

    private void setElementsDisable(boolean status) {
        Platform.runLater(() -> {
            paneMainView.setDisable(status);
//            mAbout.setDisable(status);
//            mClear.setDisable(status);
            //mSignOut.setDisable(status);
        });
    }

    private void setElementsVisible(boolean status) {
        Platform.runLater(() -> {
            paneMainView.setVisible(status);
        });
    }

    public void setButtonsDisable(boolean status) {
        dataTransferDisable = status;
        Platform.runLater(() -> {
            btnSendAllToServer.setDisable(status);
            btnReceiveAllFromServer.setDisable(status);
            btnGetFilesList.setDisable(status);
        });
    }

    public void uploadAllFiles() {
        new Thread(() -> {
            uploadStart();
            NetworkForGUIAdapter.getInstance().uploadFiles();
            uploadComplete();
        }).start();
    }

    public void uploadFile(String filename){
        addToLog("Uploading file - " + filename);
        new Thread(() -> {
            uploadStart();
            NetworkForGUIAdapter.getInstance().uploadFile(filename);
            uploadComplete();
        }).start();
    }

    private void uploadStart(){
        setButtonsDisable(true);
    }

    private void uploadComplete(){
        refreshServerList();
        setButtonsDisable(false);
    }

    public void downloadAllFilesFromServer() {
        setButtonsDisable(true);
        NetworkForGUIAdapter.getInstance().requestServerFiles();
    }

    public void refreshFilesLists() {
        refreshFilesLists(false);
    }

    public void refreshFilesLists(boolean onlyClient) {
        btnGetFilesList.setDisable(true);
        setButtonsDisable(true);
        refreshClientList();
        if (!onlyClient) refreshServerList();
        btnGetFilesList.setDisable(false);
    }

    private void refreshClientList() {
        tableFilesLocalController.update();
    }

    public void refreshServerList() {
        NetworkForGUIAdapter.getInstance().requestServerFilesList();
        addToLog("Server list request");
    }

    public void updateServerList(List<FileRepresentation> serverList) {
        tableFilesServerController.update(serverList);
        setButtonsDisable(false);
    }

    public void addToLog(String str) {
        Platform.runLater(() -> taLogs.appendText(dateFormat.format(new Date()) + " " + str + "\n"));
        LogService.CLIENT.info(str);
    }

    public void exitApp() {
        networkThread.interrupt();
        Platform.exit();
    }

    public boolean isDataTransferDisable() {
        return dataTransferDisable;
    }

    public void setDataTransferDisable(boolean dataTransferDisable) {
        this.dataTransferDisable = dataTransferDisable;
    }
}
