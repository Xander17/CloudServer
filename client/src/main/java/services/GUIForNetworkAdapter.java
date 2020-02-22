package services;

import app.MainController;
import resources.FileRepresentation;

import java.util.List;

public class GUIForNetworkAdapter {
    private static GUIForNetworkAdapter instance;
    private MainController controller;

    private GUIForNetworkAdapter(MainController controller) {
        this.controller = controller;
    }

    public static void setController(MainController controller) {
        if (controller == null) {
            String error = "GUIForNetworkAdapter error. Main controller is null";
            LogService.CLIENT.error(error);
            throw new NullPointerException(error);
        }
        if (instance == null) instance = new GUIForNetworkAdapter(controller);
    }

    public static GUIForNetworkAdapter getInstance() {
        if (instance == null) {
            String error = "GUINetworkAdapter wasn't created. Create new GUINetworkAdapter firstly.";
            LogService.CLIENT.error(error);
            throw new NullPointerException(error);
        }
        return instance;
    }

    public void setConnectionEstablishedState() {
        controller.setLoginDisable(false);
    }

    public void setDownloadInProgressState() {
        controller.setButtonsDisable(true);
    }

    public void setRegistrationSuccess() {
        controller.setRegistrationSuccess();
    }

    public void setRegAuthError(int errorCode) {
        controller.setRegAuthError(errorCode);
    }

    public void setAuthorizationSuccess() {
        controller.setAuthorizationSuccess();
    }

    public void setDownloadComplete() {
        controller.setDownloadComplete();
    }

    public void updateServerFilesList(List<FileRepresentation> filesList) {
        controller.updateServerList(filesList);
    }

    public void log(String s) {
        controller.addToLog(s);
    }
}
