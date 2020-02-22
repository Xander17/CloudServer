package services;

import app.DataHandler;
import resources.FileRepresentation;

import java.util.List;

public class NetworkForGUIAdapter {
    private static NetworkForGUIAdapter instance;
    private DataHandler handler;

    public NetworkForGUIAdapter(DataHandler handler) {
        if (handler == null) {
            String error = "NetworkForGUIAdapter creation error. Data handler is null.";
            LogService.CLIENT.error(error);
            throw new NullPointerException(error);
        }
        this.handler = handler;
        instance = this;
    }

    public static NetworkForGUIAdapter getInstance() {
        if (instance == null) {
            String error = "NetworkForGUIAdapter wasn't created. Create new NetworkForGUIAdapter firstly.";
            LogService.CLIENT.error(error);
            throw new NullPointerException(error);
        }
        return instance;
    }

    public void signIn(String login, String pass) {
        handler.signIn(login, pass);
    }

    public void signUp(String login, String pass) {
        handler.signUp(login, pass);
    }

    public void uploadFiles() {
        handler.uploadFiles();
    }

    public void requestServerFiles() {
        handler.sendAllFilesRequest();
    }

    public void requestServerFilesList() {
        handler.sendFilesListRequest();
    }

    public List<FileRepresentation> getClientFilesList() {
        return handler.getClientFilesList();
    }

    public void uploadFile(String fileName) {
        handler.uploadFile(fileName);
    }

    public void fileRequest(String fileName) {
        handler.sendFileRequest(fileName);
    }
}
