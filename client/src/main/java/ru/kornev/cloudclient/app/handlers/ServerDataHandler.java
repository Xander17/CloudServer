package ru.kornev.cloudclient.app.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import ru.kornev.cloudclient.services.GUIForNetworkAdapter;
import ru.kornev.cloudclient.services.LogService;
import ru.kornev.cloudclient.services.NetworkForGUIAdapter;
import ru.kornev.cloudcommon.exceptions.NoEnoughDataException;
import ru.kornev.cloudcommon.resources.CommandBytes;
import ru.kornev.cloudcommon.resources.FileRepresentation;
import ru.kornev.cloudcommon.services.transfer.CommandPackage;
import ru.kornev.cloudcommon.services.transfer.FileDownloader;
import ru.kornev.cloudcommon.services.transfer.resources.Progress;
import ru.kornev.cloudcommon.settings.GlobalSettings;

import java.io.IOException;
import java.util.List;

public class ServerDataHandler {
    private ChannelHandlerContext ctx;
    private ByteBuf byteBuf;
    private State state;
    private boolean logged;
    private CommandPackage commandPackage;
    private AuthHandler authHandler;
    private FilesHandler filesHandler;

    public ServerDataHandler(ChannelHandlerContext ctx, ByteBuf byteBuf) {
        this.ctx = ctx;
        this.byteBuf = byteBuf;
        this.authHandler = new AuthHandler(this, ctx);
        this.commandPackage = new CommandPackage(byteBuf);
        this.logged = false;
        this.state = State.IDLE;
        NetworkForGUIAdapter.setHandler(this);
    }

    public void handle() {
        try {
            while (byteBuf.readableBytes() > 0) {
                stateExecute();
            }
        } catch (NoEnoughDataException ignored) {
        }
    }

    private void stateExecute() throws NoEnoughDataException {
        try {
            if (state == State.IDLE) listenPackageStart();
            if (state == State.COMMAND_SELECT) selectCommandState();
            if (logged) stateLoggedExecute();
        } catch (IOException e) {
            // TODO: 26.02.2020 обработать ошибку
            LogService.CLIENT.error(e);
        }
    }

    private void stateLoggedExecute() throws NoEnoughDataException {
        if (state == State.DOWNLOAD) filesHandler.fileDownload();
        else if (state == State.FILES_LIST) {
            filesHandler.getFilesList();
            state = State.IDLE;
        }
    }

    private void listenPackageStart() {
        byte b;
        while (byteBuf.readableBytes() > 0) {
            b = byteBuf.readByte();
            if (logged && CommandBytes.PACKAGE_START.check(b)) {
                filesHandler.downloadPrepare();
                state = State.DOWNLOAD;
                break;
            } else if (CommandBytes.COMMAND_START.check(b)) {
                state = State.COMMAND_SELECT;
                break;
            }
        }
    }

    private void selectCommandState() throws NoEnoughDataException, IOException {
        FileDownloader.checkAvailableData(byteBuf, GlobalSettings.COMMAND_DATA_LENGTH + 1);
        commandPackage.load();
        if (CommandBytes.AUTH_OK.check(commandPackage.getCommand())) {
            authHandler.setAuthSuccess();
            state = State.IDLE;
        } else if (CommandBytes.REG_OK.check(commandPackage.getCommand())) {
            authHandler.setRegSuccess();
            state = State.IDLE;
        } else if (CommandBytes.ERROR.check(commandPackage.getCommand())) {
            authHandler.setRegAuthError(commandPackage);
            state = State.IDLE;
        } else if (logged) {
            selectLoggedCommandState();
        } else state = State.IDLE;
    }

    private void selectLoggedCommandState() {
        if (CommandBytes.FILES_LIST.check(commandPackage.getCommand()) && logged) {
            filesHandler.filesListGettingPrepare(commandPackage);
            state = State.FILES_LIST;
        }
    }

    public void signIn(String login, String pass) {
        authHandler.signIn(login, pass);
    }

    public void signUp(String login, String pass) {
        authHandler.signUp(login, pass);
    }

    void authSuccess() throws IOException {
        this.filesHandler = new FilesHandler(this, ctx);
        logged = true;
    }

    void downloadFinish() {
        state = State.IDLE;
    }

    public void sendFilesListRequest() {
        filesHandler.sendFilesListRequest();
    }

    public void sendAllFilesRequest() {
        filesHandler.sendAllFilesRequest();
    }

    public void sendFileRequest(String filename) {
        filesHandler.sendFileRequest(filename);
    }

    public List<FileRepresentation> getClientFilesList() {
        return filesHandler.getClientFilesList();
    }

    public void uploadFiles() {
        filesHandler.uploadFiles();
    }

    public void uploadFile(String filename) {
        filesHandler.uploadFile(filename);
    }

    public void setupProgressProperty(Progress progress) {
        GUIForNetworkAdapter.getInstance().setupProgress(progress);
    }

    public void deleteLocalFile(FileRepresentation file) {
        filesHandler.deleteLocalFile(file);
    }

    public void deleteFileFromServer(FileRepresentation file) {
        filesHandler.deleteFileFromServer(file);
    }

    ByteBuf getByteBuf() {
        return byteBuf;
    }

    private enum State {
        IDLE, COMMAND_SELECT, DOWNLOAD, FILES_LIST
    }
}
