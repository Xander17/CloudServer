package app;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import resources.CommandMessage;
import resources.LoginRegError;
import settings.GlobalSettings;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

import static resources.LoginRegError.LOGIN_EXISTS;

public class Controller implements Initializable {
    @FXML
    private VBox vBoxLogin, vBoxRegistration;
    @FXML
    private TextField tfLogin, tfPassword, tfRegLogin, tfRegPassword;
    @FXML
    private Label lblLoginInfo, lblRegInfo;
    @FXML
    private Button btnLogin, btnReg;

    private DataInputStream in = null;
    private DataOutputStream out = null;
    private Socket socket = null;

    private String id;
    private boolean loginState = true;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        runServerListener();
    }

    private void runServerListener() {
        try {
            socket = new Socket(GlobalSettings.CONNECTION_HOST, GlobalSettings.CONNECTION_PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            new Thread(() -> {
                try {
                    loginRegWindow();
//                    getMessages();
                } catch (IOException ignored) {
                } finally {
                    closeIOStreams();
                }
            }).start();
        } catch (IOException e) {
            System.out.println("Server connection error!");
        }
    }

    private void loginRegWindow() throws IOException {
        String[] inputCommandData;
        while (true) {
            if (!checkCommandStart(in.readByte())) continue;
            inputCommandData = in.readUTF().split(" ", 2);
            if (CommandMessage.AUTH_OK.check(inputCommandData[0]) && vBoxLogin.isVisible()) {
                id = inputCommandData[1];
                setLoginState(false);
                break;
            } else if (CommandMessage.ERROR.check(inputCommandData[0]) && vBoxLogin.isVisible()) {
                setLoginInfo(inputCommandData[1]);
            } else if (CommandMessage.REG_OK.check(inputCommandData[0]) && vBoxRegistration.isVisible()) {
                String login = tfRegLogin.getText().trim();
                String pass = tfRegPassword.getText();
                swapLoginReg(login, pass);
            } else if (CommandMessage.ERROR.check(inputCommandData[0]) && vBoxRegistration.isVisible()) {
                setRegInfo(inputCommandData[1]);
            }
        }
    }

    private void setLoginInfo(String s) {
        LoginRegError error = getErrorString(s);
        setLoginInfo(error);
    }

    private void setLoginInfo(LoginRegError error) {
        Platform.runLater(() -> {
            lblLoginInfo.setText(error.toString());
            tfLogin.requestFocus();
        });
    }

    private void setRegInfo(String s) {
        LoginRegError error = getErrorString(s);
        setRegInfo(error);
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

    private LoginRegError getErrorString(String index) {
        LoginRegError errorString;
        try {
            int i = Integer.parseInt(index);
            if (i < LoginRegError.values().length)
                errorString = LoginRegError.values()[i];
            else errorString = LoginRegError.RESPONSE_ERROR;
        } catch (NumberFormatException e) {
            errorString = LoginRegError.RESPONSE_ERROR;
        }
        return errorString;
    }

    private boolean checkCommandStart(int b) {
        return b == GlobalSettings.COMMAND_START_SIGNAL_BYTE;
    }

    public void loginToServer() {

    }

    public void signUp() {

    }

    private void closeIOStreams() {
        try {
            in.close();
        } catch (IOException | NullPointerException ignored) {
        }
        try {
            out.close();
        } catch (IOException | NullPointerException ignored) {
        }
        try {
            socket.close();
        } catch (IOException | NullPointerException ignored) {
        }
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
}
