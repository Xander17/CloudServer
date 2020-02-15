package app;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import resources.CommandBytes;
import resources.LoginRegError;
import services.FormatChecker;
import services.LogService;
import services.NetworkThread;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;

import static resources.LoginRegError.LOGIN_EXISTS;
import static resources.LoginRegError.RESPONSE_ERROR;

public class Controller implements Initializable {
    @FXML
    private VBox vBoxLogin, vBoxRegistration;
    @FXML
    private TextField tfLogin, tfPassword, tfRegLogin, tfRegPassword;
    @FXML
    private Label lblLoginInfo, lblRegInfo;
    @FXML
    private Button btnLogin, btnReg;

    private ServerHandler serverHandler;
    private NetworkThread networkThread;
    private int id;
    private boolean loginState = true;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        runServerListener();
    }

    // TODO: 15.02.2020 посмотреть, почему клиент не отключается после закрытия окна
    private void runServerListener() {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        networkThread = new NetworkThread(this, countDownLatch);
        //networkThread.setDaemon(true);
        networkThread.start();
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            LogService.SERVER.error(e);
        }
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
            serverHandler.sendRegAuthData(CommandBytes.REG, login, pass);
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
            serverHandler.sendRegAuthData(CommandBytes.AUTH, login, pass);
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
    }

    public void setRegAuthError(int code) {
        LoginRegError[] errors = LoginRegError.values();
        LoginRegError error;
        if (code >= errors.length || code < 0) error = RESPONSE_ERROR;
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
            if (error == LOGIN_EXISTS) {
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
//        Platform.runLater(() -> {
//            btnSend.setDisable(status);
//            tfMessage.setDisable(status);
//            taChat.setDisable(status);
//            listUsers.setDisable(status);
//            mAbout.setDisable(status);
//            mClear.setDisable(status);
//            // mSignOut.setDisable(status);
//        });
    }

    private void setElementsVisible(boolean status) {
//        btnSend.setVisible(status);
//        tfMessage.setVisible(status);
//        taChat.setVisible(status);
//        listUsers.setVisible(status);
    }

    public void setServerHandler(ServerHandler serverHandler) {
        this.serverHandler = serverHandler;
    }
}
