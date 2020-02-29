package ru.kornev.cloudnetwork.network;

import ru.kornev.cloudcommon.callbacks.MessageCallback;
import ru.kornev.cloudcommon.resources.FileRepresentation;
import ru.kornev.cloudnetwork.callbacks.ErrorCodeCallback;
import ru.kornev.cloudnetwork.callbacks.FileListCallback;
import ru.kornev.cloudnetwork.callbacks.ProgressCallback;
import ru.kornev.cloudnetwork.callbacks.VoidCallback;
import ru.kornev.cloudnetwork.services.LogService;

import java.util.List;

public class NetworkService {
    private static NetworkService instance;
    private ServerDataHandler handler;

    private NetworkService() {
    }

    public static NetworkService getInstance() {
        if (instance == null) {
            instance = new NetworkService();
        }
        return instance;
    }

    void setHandler(ServerDataHandler handler) {
        if (handler == null) {
            String error = "NetworkForGUIAdapter error. Data handler is null.";
            LogService.CLIENT.error(error);
            throw new NullPointerException(error);
        }
        if (this.handler == null) {
            this.handler = handler;
        }
    }

    public void signIn(String login, String pass) {
        if (handler.isApplicationBusy()) return;
        handler.getAuthHandler().signIn(login, pass);
    }

    public void signUp(String login, String pass) {
        if (handler.isApplicationBusy()) return;
        handler.getAuthHandler().signUp(login, pass);
    }

    public void requestServerFilesList() {
        if (handler.isApplicationBusy()) return;
        handler.getFilesHandler().sendFilesListRequest();
    }

    public void requestAllFilesFromServer() {
        if (handler.isApplicationBusy()) return;
        handler.getFilesHandler().sendAllFilesRequest();
    }

    public void requestFileFromServer(String fileName) {
        if (handler.isApplicationBusy()) return;
        handler.getFilesHandler().sendFileRequest(fileName);
    }

    public List<FileRepresentation> getLocalFilesList() {
        return handler.getFilesHandler().getLocalFilesList();
    }

    public void uploadAllFiles() {
        if (handler.isApplicationBusy()) return;
        handler.getFilesHandler().uploadFiles();
    }

    public void uploadFile(FileRepresentation file) {
        if (handler.isApplicationBusy()) return;
        handler.getFilesHandler().uploadFile(file);
    }

    public void deleteLocalFile(FileRepresentation file) {
        if (handler.isApplicationBusy()) return;
        handler.getFilesHandler().deleteLocalFile(file);
    }

    public void deleteFileFromServer(FileRepresentation file) {
        if (handler.isApplicationBusy()) return;
        handler.getFilesHandler().deleteFileFromServer(file);
    }

    public void setOnRegistrationSuccess(VoidCallback onRegistrationSuccess) {
        CallbackHandler.setOnRegistrationSuccess(onRegistrationSuccess);
    }

    public void setOnAuthSuccess(VoidCallback onAuthSuccess) {
        CallbackHandler.setOnAuthSuccess(onAuthSuccess);
    }

    public void setOnAuthRegError(ErrorCodeCallback onAuthRegError) {
        CallbackHandler.setOnAuthRegError(onAuthRegError);
    }

    public void setOnUploadFinish(VoidCallback onUploadFinish) {
        CallbackHandler.setOnUploadFinish(onUploadFinish);
    }

    public void setOnDownloadStart(VoidCallback onDownloadStart) {
        CallbackHandler.setOnDownloadStart(onDownloadStart);
    }

    public void setOnDownloadFinish(VoidCallback onDownloadFinish) {
        CallbackHandler.setOnDownloadFinish(onDownloadFinish);
    }

    public void setOnUploadFileNotExists(VoidCallback onUploadFileNotExists) {
        CallbackHandler.setOnUploadFileNotExists(onUploadFileNotExists);
    }

    public void setOnFileListDownloadFinish(FileListCallback onFileListDownloadFinish) {
        CallbackHandler.setOnFileListDownloadFinish(onFileListDownloadFinish);
    }

    public void setOnProgressPropertyRegister(ProgressCallback onProgressPropertyRegister) {
        CallbackHandler.setOnProgressPropertyRegister(onProgressPropertyRegister);
    }

    public void setOnServerMessage(MessageCallback onServerMessage) {
        CallbackHandler.setOnServerMessage(onServerMessage);
    }

    public void setOnConnectionEstablished(VoidCallback onConnectionEstablished) {
        CallbackHandler.setOnConnectionEstablished(onConnectionEstablished);
    }
}
