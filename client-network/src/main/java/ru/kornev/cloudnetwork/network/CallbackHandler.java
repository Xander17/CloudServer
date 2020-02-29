package ru.kornev.cloudnetwork.network;

import ru.kornev.cloudcommon.callbacks.MessageCallback;
import ru.kornev.cloudcommon.resources.FileRepresentation;
import ru.kornev.cloudcommon.services.transfer.Progress;
import ru.kornev.cloudnetwork.callbacks.ErrorCodeCallback;
import ru.kornev.cloudnetwork.callbacks.FileListCallback;
import ru.kornev.cloudnetwork.callbacks.ProgressCallback;
import ru.kornev.cloudnetwork.callbacks.VoidCallback;

import java.util.List;

class CallbackHandler {

    private static VoidCallback onConnectionEstablished;
    private static VoidCallback onUploadFinish;
    private static VoidCallback onUploadFileNotExists;
    private static FileListCallback onFileListDownloadFinish;
    private static ProgressCallback onProgressPropertyRegister;
    private static MessageCallback onServerMessage;
    private static VoidCallback onRegistrationSuccess;
    private static VoidCallback onAuthSuccess;
    private static ErrorCodeCallback onAuthRegError;
    private static VoidCallback onDownloadStart;
    private static VoidCallback onDownloadFinish;

    static void onConnectionEstablished() {
        if (onConnectionEstablished != null) onConnectionEstablished.callback();
    }

    static void onUploadFinish() {
        if (onUploadFinish != null) onUploadFinish.callback();
    }

    static void onUploadFileNotExists() {
        if (onUploadFileNotExists != null) onUploadFileNotExists.callback();
    }

    static void onFileListDownloadFinish(List<FileRepresentation> list) {
        if (onFileListDownloadFinish != null) onFileListDownloadFinish.callback(list);
    }

    static void onProgressPropertyRegister(Progress progress) {
        if (onProgressPropertyRegister != null) onProgressPropertyRegister.callback(progress);
    }

    static void onServerMessage(String msg) {
        if (onServerMessage != null) onServerMessage.callback(msg);
    }

    static void onRegistrationSuccess() {
        if (onRegistrationSuccess != null) onRegistrationSuccess.callback();
    }

    static void onAuthSuccess() {
        if (onAuthSuccess != null) onAuthSuccess.callback();
    }

    static void onAuthRegError(int errorCode) {
        if (onAuthRegError != null) onAuthRegError.callback(errorCode);
    }

    static void onDownloadStart() {
        if (onDownloadStart != null) onDownloadStart.callback();
    }

    static void onDownloadFinish() {
        if (onDownloadFinish != null) onDownloadFinish.callback();
    }

    static void setOnUploadFinish(VoidCallback onUploadFinish) {
        CallbackHandler.onUploadFinish = onUploadFinish;
    }

    static void setOnUploadFileNotExists(VoidCallback onUploadFileNotExists) {
        CallbackHandler.onUploadFileNotExists = onUploadFileNotExists;
    }

    static void setOnFileListDownloadFinish(FileListCallback onFileListDownloadFinish) {
        CallbackHandler.onFileListDownloadFinish = onFileListDownloadFinish;
    }

    static void setOnProgressPropertyRegister(ProgressCallback onProgressPropertyRegister) {
        CallbackHandler.onProgressPropertyRegister = onProgressPropertyRegister;
    }

    static void setOnRegistrationSuccess(VoidCallback onRegistrationSuccess) {
        CallbackHandler.onRegistrationSuccess = onRegistrationSuccess;
    }

    static void setOnAuthSuccess(VoidCallback onAuthSuccess) {
        CallbackHandler.onAuthSuccess = onAuthSuccess;
    }

    static void setOnAuthRegError(ErrorCodeCallback onAuthRegError) {
        CallbackHandler.onAuthRegError = onAuthRegError;
    }

    static void setOnDownloadStart(VoidCallback onDownloadStart) {
        CallbackHandler.onDownloadStart = onDownloadStart;
    }

    static void setOnDownloadFinish(VoidCallback onDownloadFinish) {
        CallbackHandler.onDownloadFinish = onDownloadFinish;
    }

    static MessageCallback getOnServerMessage() {
        return onServerMessage;
    }

    static void setOnServerMessage(MessageCallback onServerMessage) {
        CallbackHandler.onServerMessage = onServerMessage;
    }

    public static void setOnConnectionEstablished(VoidCallback onConnectionEstablished) {
        CallbackHandler.onConnectionEstablished = onConnectionEstablished;
    }
}