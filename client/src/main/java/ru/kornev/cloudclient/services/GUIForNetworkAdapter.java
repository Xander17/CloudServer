package ru.kornev.cloudclient.services;

import ru.kornev.cloudclient.app.MainController;
import ru.kornev.cloudcommon.resources.FileRepresentation;
import ru.kornev.cloudcommon.services.transfer.resources.Progress;

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

    public void afterConnectionInit() {
        controller.afterConnectionInit();
    }

    public void setDownloadInProgress() {
        controller.setGUIActionsDisable(true);
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

    public void setDownloadComplete(boolean status) {
        controller.setDownloadComplete(status);
    }

    public void filesListGettingComplete(List<FileRepresentation> filesList) {
        controller.updateServerList(filesList);
    }

    public void localFileNotExist(String filename){
        controller.localFileNotExist(filename);
    }

    public void log(String s) {
        controller.addToLog(s);
    }

    public void setupProgress(Progress progress) {
        controller.setupProgress(progress);
    }
}
