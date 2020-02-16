package app;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import resources.CommandBytes;
import resources.LoginRegError;
import services.FormatChecker;
import services.LogService;
import services.NetworkThread;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    @FXML
    private VBox vBoxLogin, vBoxRegistration;
    @FXML
    private TextField tfLogin, tfPassword, tfRegLogin, tfRegPassword;
    @FXML
    private Label lblLoginInfo, lblRegInfo;
    @FXML
    private Button btnLogin, btnReg, btnLoginRegSwap;
    @FXML
    private Button btnSendAllToServer, btnGetFilesList, btnReceiveAllFromServer;
    @FXML
    private ListView<String> listFilesClient, listFilesServer;
    @FXML
    private TextArea taLogs;

    private DataHandler dataHandler;
    private NetworkThread networkThread;
    private int id;
    private boolean loginState;
    private boolean dataTransferDisable;
    private SimpleDateFormat dateFormat;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loginState = true;
        dataTransferDisable = true;
        dateFormat = new SimpleDateFormat("[HH:mm:ss]");
        runServerListener();
    }

    private void runServerListener() {
        networkThread = new NetworkThread(this);
        networkThread.start();
    }

    public void setLoginDisable(boolean status) {
        btnLogin.setDisable(status);
        btnLoginRegSwap.setDisable(status);
    }

    public void signUp() {
        btnReg.requestFocus();
        String login = tfRegLogin.getText().trim();
        String pass = tfRegPassword.getText();
        FormatChecker formatChecker = new FormatChecker();
        // TODO: 14.02.2020 проверка на соединение
        //if (!isSocketOpen()) setRegInfo(LoginRegError.NO_CONNECTION);
        if (!formatChecker.checkLoginFormat(login)) setRegInfo(formatChecker.getCurrentError());
        else if (!formatChecker.checkPasswordFormat(pass)) setRegInfo(formatChecker.getCurrentError());
        else if (!login.isEmpty() && !pass.isEmpty()) {
            dataHandler.sendRegAuthData(CommandBytes.REG, login, pass);
        } else {
            Platform.runLater(() -> {
                tfRegLogin.setText(login);
                setRegInfo(LoginRegError.NOT_ENOUGH_DATA);
            });
        }
    }

    public void loginToServer() {
        btnLogin.requestFocus();
        String login = tfLogin.getText().trim();
        String pass = tfPassword.getText();
        // TODO: 14.02.2020 проверка на соединение
        //if (!isSocketOpen()) setLoginInfo(LoginRegError.NO_CONNECTION);
        if (!login.isEmpty() && !pass.isEmpty()) {
            dataHandler.sendRegAuthData(CommandBytes.AUTH, login, pass);
        } else {
            Platform.runLater(() -> {
                tfLogin.setText(login);
                setLoginInfo(LoginRegError.NOT_ENOUGH_DATA);
            });
        }
    }

    public void setRegSuccess() {
        if (!vBoxRegistration.isVisible()) return;
        String login = tfRegLogin.getText().trim();
        String pass = tfRegPassword.getText();
        swapLoginReg(login, pass);
    }

    public void setAuthSuccess(int id) {
        if (!vBoxLogin.isVisible()) return;
        this.id = id;
        setLoginState(false);
        dataTransferDisable = false;
        refreshFilesList();
    }

    public void setRegAuthError(int code) {
        LoginRegError[] errors = LoginRegError.values();
        LoginRegError error;
        if (code >= errors.length || code < 0) error = LoginRegError.RESPONSE_ERROR;
        else error = errors[code];
        if (vBoxLogin.isVisible()) setLoginInfo(error);
        else if (vBoxRegistration.isVisible()) setRegInfo(error);
    }

    private void setLoginInfo(LoginRegError error) {
        Platform.runLater(() -> {
            lblLoginInfo.setText(error.toString());
            tfLogin.requestFocus();
        });
    }

    private void setRegInfo(LoginRegError error) {
        Platform.runLater(() -> {
            lblRegInfo.setText(error.toString());
            if (error == LoginRegError.LOGIN_EXISTS) {
                tfRegLogin.clear();
                tfRegLogin.requestFocus();
            }
        });
    }

    public void passwordFocus() {
        tfPassword.requestFocus();
    }

    public void regPasswordFocus() {
        tfRegPassword.requestFocus();
    }

    public void swapLoginReg() {
        swapLoginReg("", "");
    }

    public void swapLoginReg(String login, String pass) {
        Platform.runLater(() -> {
            vBoxLogin.setVisible(!vBoxLogin.isVisible());
            vBoxRegistration.setVisible(!vBoxLogin.isVisible());
            tfLogin.setText(login);
            tfPassword.setText(pass);
            tfRegLogin.clear();
            tfRegPassword.clear();
            lblRegInfo.setText("");
            lblLoginInfo.setText("");
        });
    }

    private void setLoginState(boolean status) {
        setElementsDisable(status);
        setElementsVisible(!status);
        if (status) lblLoginInfo.setText("");
        vBoxLogin.setVisible(status);
        loginState = status;
    }

    private void setElementsDisable(boolean status) {
        Platform.runLater(() -> {
            listFilesClient.setDisable(status);
            listFilesServer.setDisable(status);
            taLogs.setDisable(status);
//            mAbout.setDisable(status);
//            mClear.setDisable(status);
            //mSignOut.setDisable(status);
        });
    }

    private void setElementsVisible(boolean status) {
        Platform.runLater(() -> {
            listFilesClient.setVisible(status);
            listFilesServer.setVisible(status);
            btnSendAllToServer.setVisible(status);
            btnReceiveAllFromServer.setVisible(status);
            btnGetFilesList.setVisible(status);
            taLogs.setVisible(status);
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

    public void setDataHandler(DataHandler dataHandler) {
        this.dataHandler = dataHandler;
    }

    public void filesListHandler(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() < 2 || dataTransferDisable) return;
        if (mouseEvent.getSource().equals(listFilesClient)) {
            String file = listFilesClient.getSelectionModel().getSelectedItem();
            addToLog("Uploading " + file);
            new Thread(() -> {
                setButtonsDisable(true);
                dataHandler.uploadFile(file);
                refreshServerList();
                setButtonsDisable(false);
            }).start();
        } else if (mouseEvent.getSource().equals(listFilesServer)) {
            String file = listFilesServer.getSelectionModel().getSelectedItem();
            addToLog("Downloading request" + file);
            dataHandler.sendFileRequest(file);
        }
    }

    public void sendAllToServer() {
        new Thread(() -> {
            try {
                setButtonsDisable(true);
                dataHandler.uploadFiles();
                refreshServerList();
                setButtonsDisable(false);
            } catch (IOException e) {
                LogService.CLIENT.error("Ошибка отправки файлов", e.toString());
            }
        }).start();
    }

    public void receiveAllFromServer() {
        setButtonsDisable(true);
        dataHandler.sendAllFilesRequest();
    }

    public void refreshFilesList() {
        refreshFilesList(false);
    }

    public void refreshFilesList(boolean onlyClient) {
        btnGetFilesList.setDisable(true);
        setButtonsDisable(true);
        refreshClientList();
        if (!onlyClient) refreshServerList();
        btnGetFilesList.setDisable(false);
    }

    private void refreshClientList() {
        List<String> files = dataHandler.getClientFilesList();
        ObservableList<String> clientList = listFilesClient.getItems();
        Platform.runLater(() -> {
            clientList.clear();
            clientList.addAll(files);
            addToLog("Client list updated");
        });
    }

    private void refreshServerList() {
        dataHandler.sendFilesListRequest();
        addToLog("Server list request");
    }

    public void updateServerList(List<String> serverList) {
        ObservableList<String> list = listFilesServer.getItems();
        Platform.runLater(() -> {
            list.clear();
            list.addAll(serverList);
            addToLog("Server list updated");
        });
    }

    public void addToLog(String str) {
        Platform.runLater(() -> taLogs.appendText(dateFormat.format(new Date()) + " " + str + "\n"));
        LogService.CLIENT.info(str);
    }

    public void exitApp() {
        //dataHandler.closeChannel();
        networkThread.interrupt();
        Platform.exit();
    }
}
