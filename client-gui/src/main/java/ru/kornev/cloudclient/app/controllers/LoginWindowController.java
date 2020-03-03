package ru.kornev.cloudclient.app.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import ru.kornev.cloudcommon.resources.LoginRegError;
import ru.kornev.cloudcommon.services.FormatChecker;
import ru.kornev.cloudnetwork.network.NetworkService;

public class LoginWindowController extends SecondLevelController {
    @FXML
    private StackPane loginWindow;
    @FXML
    private VBox vBoxLogin, vBoxRegistration;
    @FXML
    private TextField tfLogin, tfPassword, tfRegLogin, tfRegPassword;
    @FXML
    private Label lblLoginInfo, lblRegInfo;
    @FXML
    private Button btnLogin, btnReg, btnLoginRegSwap;

    public void setLoginDisable(boolean status) {
        btnLogin.setDisable(status);
        btnLoginRegSwap.setDisable(status);
    }

    public void signUp() {
        btnReg.requestFocus();
        String login = tfRegLogin.getText().trim();
        String pass = tfRegPassword.getText();
        FormatChecker formatChecker = new FormatChecker();
        if (!formatChecker.checkLoginFormat(login)) setRegInfo(formatChecker.getCurrentError());
        else if (!formatChecker.checkPasswordFormat(pass)) setRegInfo(formatChecker.getCurrentError());
        else if (!login.isEmpty() && !pass.isEmpty()) {
            NetworkService.getInstance().signUp(login, pass);
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
        if (!login.isEmpty() && !pass.isEmpty()) {
            NetworkService.getInstance().signIn(login, pass);
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

    public void setRegAuthError(int errorCode) {
        LoginRegError[] errors = LoginRegError.values();
        LoginRegError error;
        if (errorCode >= errors.length || errorCode < 0) error = LoginRegError.RESPONSE_ERROR;
        else error = errors[errorCode];
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

    public void setLoginState(boolean status) {
        loginWindow.setVisible(status);
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

    private void swapLoginReg(String login, String pass) {
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
}
