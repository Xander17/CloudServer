package ru.kornev.cloudnetwork.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import ru.kornev.cloudcommon.exceptions.NoEnoughDataException;
import ru.kornev.cloudcommon.resources.CommandBytes;
import ru.kornev.cloudcommon.services.transfer.CommandPackage;
import ru.kornev.cloudcommon.services.transfer.FileDownloader;
import ru.kornev.cloudcommon.settings.GlobalSettings;
import ru.kornev.cloudnetwork.services.LogService;

import java.io.IOException;

class ServerDataHandler {
    private ChannelHandlerContext ctx;
    private ByteBuf byteBuf;
    private State state;
    private boolean logged;
    private CommandPackage commandPackage;
    private AuthHandler authHandler;
    private FilesHandler filesHandler;

    ServerDataHandler(ChannelHandlerContext ctx, ByteBuf byteBuf) {
        this.ctx = ctx;
        this.byteBuf = byteBuf;
        this.authHandler = new AuthHandler(ctx);
        this.commandPackage = new CommandPackage(byteBuf);
        this.logged = false;
        this.state = State.IDLE;
        NetworkService.getInstance().setHandler(this);
    }

    void handle() {
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
            LogService.CLIENT.error("IO Error. Connection closing");
            ctx.channel().close();
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
                state = State.DOWNLOAD;
                CallbackHandler.onDownloadStart();
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
            initUser();
            state = State.IDLE;
            CallbackHandler.onAuthSuccess();
        } else if (CommandBytes.REG_OK.check(commandPackage.getCommand())) {
            state = State.IDLE;
            CallbackHandler.onRegistrationSuccess();
        } else if (CommandBytes.ERROR.check(commandPackage.getCommand())) {
            state = State.IDLE;
            CallbackHandler.onAuthRegError(commandPackage.getIntCommandData());
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

    private void initUser() throws IOException {
        this.filesHandler = new FilesHandler(this, ctx);
        logged = true;
    }

    void downloadFinish() {
        state = State.IDLE;
        CallbackHandler.onDownloadFinish();
    }

    void setWaitingThreadState(boolean status) {
        if (status) state = State.WAITING;
        else state = State.IDLE;
    }


    boolean isApplicationBusy() {
        if (state != State.IDLE) {
            CallbackHandler.onServerMessage("Application is busy");
            return true;
        }
        return false;
    }

    ByteBuf getByteBuf() {
        return byteBuf;
    }

    AuthHandler getAuthHandler() {
        return authHandler;
    }

    FilesHandler getFilesHandler() {
        return filesHandler;
    }

    private enum State {
        IDLE, COMMAND_SELECT, DOWNLOAD, FILES_LIST, WAITING
    }
}
