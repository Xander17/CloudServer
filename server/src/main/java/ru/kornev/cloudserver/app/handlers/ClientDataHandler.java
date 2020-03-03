package ru.kornev.cloudserver.app.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import ru.kornev.cloudcommon.exceptions.NoEnoughDataException;
import ru.kornev.cloudcommon.resources.CommandBytes;
import ru.kornev.cloudcommon.services.transfer.CommandPackage;
import ru.kornev.cloudcommon.services.transfer.FileDownloader;
import ru.kornev.cloudcommon.settings.GlobalSettings;
import ru.kornev.cloudserver.app.MainServer;
import ru.kornev.cloudserver.services.LogService;

import java.io.IOException;

public class ClientDataHandler {

    private MainServer server;
    private ChannelHandlerContext ctx;
    private ByteBuf byteBuf;

    private State state;
    private CommandPackage commandPackage;
    private AuthHandler authHandler;
    private FilesHandler filesHandler;
    private boolean logged;

    public ClientDataHandler(MainServer server, ChannelHandlerContext ctx, ByteBuf byteBuf) {
        this.server = server;
        this.ctx = ctx;
        this.byteBuf = byteBuf;
        this.commandPackage = new CommandPackage(byteBuf);
        this.authHandler = new AuthHandler(this, ctx);
        this.logged = false;
        this.state = State.IDLE;
    }

    public void handle() {
        if (state == State.WAITING) return;
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
            else if (state == State.COMMAND_SELECT) selectCommandState();
            else if (state == State.REG) {
                authHandler.reg(commandPackage);
                state = State.IDLE;
            } else if (state == State.AUTH) {
                authHandler.auth(commandPackage);
                state = State.IDLE;
            } else if (logged) stateLoggedExecute();
        } catch (IOException e) {
            LogService.SERVER.error(authHandler.getLogin(), "State - " + state.toString(), e.toString());
            LogService.SERVER.error(e);
            ctx.close();
        }
    }

    private void stateLoggedExecute() throws NoEnoughDataException, IOException {
        if (state == State.DOWNLOAD) filesHandler.fileDownload();
        else if (state == State.FILE_REQUEST) filesHandler.fileRequest();
        else if (state == State.DELETE_REQUEST) {
            filesHandler.deleteFile();
            state = State.IDLE;
        }
    }

    private void listenPackageStart() {
        byte b;
        while (byteBuf.readableBytes() > 0) {
            b = byteBuf.readByte();
            if (logged && CommandBytes.PACKAGE_START.check(b)) {
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
        if (CommandBytes.AUTH.check(commandPackage.getCommand())) {
            state = State.AUTH;
        } else if (CommandBytes.REG.check(commandPackage.getCommand())) {
            state = State.REG;
        } else if (logged) {
            selectLoggedCommandState();
        } else {
            state = State.IDLE;
        }
    }

    private void selectLoggedCommandState() throws IOException {
        if (CommandBytes.FILES_LIST.check(commandPackage.getCommand())) {
            filesHandler.sendFilesList();
            state = State.IDLE;
        } else if (CommandBytes.FILES.check(commandPackage.getCommand())) {
            filesHandler.sendAllFiles();
        } else if (CommandBytes.FILE.check(commandPackage.getCommand())) {
            state = State.FILE_REQUEST;
        } else if (CommandBytes.DELETE.check(commandPackage.getCommand())) {
            state = State.DELETE_REQUEST;
        }
    }

    void authSuccess() throws IOException {
        filesHandler = new FilesHandler(this, ctx);
        logged = true;
    }

    void downloadFinish() {
        state = State.IDLE;
    }

    void setWaitingThreadState(boolean status) {
        if (status) state = State.WAITING;
        else state = State.IDLE;
    }

    public void closeChannel() {
        ctx.channel().close();
    }

    public String getLogin() {
        return authHandler.getLogin();
    }

    MainServer getServer() {
        return server;
    }

    ByteBuf getByteBuf() {
        return byteBuf;
    }

    private enum State {
        IDLE, COMMAND_SELECT, AUTH, REG, DOWNLOAD, FILE_REQUEST, DELETE_REQUEST, WAITING
    }
}
