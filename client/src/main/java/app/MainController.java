package app;

import app.controllers.TableFilesLocalController;
import app.controllers.TableFilesServerController;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import resources.CommandBytes;
import resources.FileRepresentation;
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

public class MainController implements Initializable {
    private static MainController controller;

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
    private TextArea taLogs;
    @FXML
    private VBox vBoxListClient, vBoxListServer;
    @FXML
    private TableFilesLocalController tableFilesLocalController;
    @FXML
    private TableFilesServerController tableFilesServerController;

    private DataHandler dataHandler;
    private NetworkThread networkThread;
    private int id;
    private boolean loginState;
    private boolean dataTransferDisable;
    private SimpleDateFormat dateFormat;

    public static MainController getInstance() {
        return controller;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        dateFormat = new SimpleDateFormat("[HH:mm:ss]");
        controller = this;
        loginState = true;
        dataTransferDisable = true;
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
            vBoxListClient.setDisable(status);
            vBoxListServer.setDisable(status);
            taLogs.setDisable(status);
//            mAbout.setDisable(status);
//            mClear.setDisable(status);
            //mSignOut.setDisable(status);
        });
    }

    private void setElementsVisible(boolean status) {
        Platform.runLater(() -> {
            vBoxListClient.setVisible(status);
            vBoxListServer.setVisible(status);
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
        tableFilesLocalController.update();
    }

    public void refreshServerList() {
        dataHandler.sendFilesListRequest();
        addToLog("Server list request");
    }

    public void updateServerList(List<FileRepresentation> serverList) {
        tableFilesServerController.update(serverList);
    }

    public void addToLog(String str) {
        Platform.runLater(() -> taLogs.appendText(dateFormat.format(new Date()) + " " + str + "\n"));
        LogService.CLIENT.info(str);
    }

    public void exitApp() {
        networkThread.interrupt();
        Platform.exit();
    }

    public DataHandler getDataHandler() {
        return dataHandler;
    }

    public void setDataHandler(DataHandler dataHandler) {
        this.dataHandler = dataHandler;
    }

    public boolean isDataTransferDisable() {
        return dataTransferDisable;
    }

    public void setDataTransferDisable(boolean dataTransferDisable) {
        this.dataTransferDisable = dataTransferDisable;
    }
}
