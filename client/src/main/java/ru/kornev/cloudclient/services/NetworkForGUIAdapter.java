package ru.kornev.cloudclient.services;

import ru.kornev.cloudclient.app.handlers.ServerDataHandler;
import ru.kornev.cloudcommon.resources.FileRepresentation;

import java.util.List;

public class NetworkForGUIAdapter {
    private static NetworkForGUIAdapter instance;
    private ServerDataHandler handler;

    private NetworkForGUIAdapter(ServerDataHandler handler) {
        this.handler = handler;
    }

    public static void setHandler(ServerDataHandler handler) {
        if (handler == null) {
            String error = "NetworkForGUIAdapter error. Data handler is null.";
            LogService.CLIENT.error(error);
            throw new NullPointerException(error);
        }
        if (instance == null) instance = new NetworkForGUIAdapter(handler);
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

    public void requestFile(String fileName) {
        handler.sendFileRequest(fileName);
    }

    public void deleteLocalFile(FileRepresentation file) {
        handler.deleteLocalFile(file);
    }

    public void deleteFileFromServer(FileRepresentation file) {
        handler.deleteFileFromServer(file);
    }
}
