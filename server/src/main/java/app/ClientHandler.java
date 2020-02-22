package app;

import exceptions.NoEnoughDataException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import resources.CommandBytes;
import resources.LoginRegError;
import services.*;
import services.transfer.CommandPackage;
import services.transfer.DataSocketWriter;
import services.transfer.FileDownloader;
import services.transfer.FileUploader;
import settings.GlobalSettings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ClientHandler {

    private MainServer server;
    private ChannelHandlerContext ctx;
    private ByteBuf byteBuf;
    private String remoteAddress;
    private FileDownloader downloader;
    private String login;
    private Integer id;
    private Path rootDir;
    private State state;
    private boolean noEnoughBytes;
    private CommandPackage commandPackage;
    private boolean logged;

    public ClientHandler(MainServer server, ChannelHandlerContext ctx, ByteBuf byteBuf) {
        this.server = server;
        this.ctx = ctx;
        this.remoteAddress = ctx.channel().remoteAddress().toString();
        this.byteBuf = byteBuf;
        this.commandPackage = new CommandPackage(byteBuf);
        this.state = State.IDLE;
        this.logged = false;
    }

    public void handle() {
        noEnoughBytes = false;
        while (byteBuf.readableBytes() > 0 && !noEnoughBytes) {
            stateExecute();
        }
    }

    private void stateExecute() {
        try {
            if (state == State.IDLE) listenPackageStart();
            else if (state == State.COMMAND_SELECT) selectCommandState();
            else if (state == State.REG) reg();
            else if (state == State.AUTH) auth();
            else if (state == State.DOWNLOAD && logged) fileDownload();
            else if (state == State.FILE_REQUEST && logged) resolveFileRequest();
        } catch (IOException e) {
            LogService.SERVER.error(login, e.toString());
        } catch (NoEnoughDataException e) {
            noEnoughBytes = true;
        }
    }

    private void listenPackageStart() {
        byte b;
        while (byteBuf.readableBytes() > 0) {
            b = byteBuf.readByte();
            if (logged && CommandBytes.PACKAGE_START.check(b)) {
                downloader.reset();
                state = State.DOWNLOAD;
                break;
            } else if (CommandBytes.COMMAND_START.check(b)) {
                state = State.COMMAND_SELECT;
                break;
            }
        }
    }

    private void selectCommandState() throws NoEnoughDataException, IOException {
        checkAvailableData(GlobalSettings.COMMAND_DATA_LENGTH + 1);
        commandPackage.load();
        if (CommandBytes.AUTH.check(commandPackage.getCommand())) {
            state = State.AUTH;
        } else if (CommandBytes.REG.check(commandPackage.getCommand())) {
            state = State.REG;
        } else if (CommandBytes.FILES_LIST.check(commandPackage.getCommand()) && logged) {
            sendFilesList();
        } else if (CommandBytes.FILES.check(commandPackage.getCommand()) && logged) {
            sendAllFiles();
        } else if (CommandBytes.FILE.check(commandPackage.getCommand()) && logged) {
            state = State.FILE_REQUEST;
        } else {
            state = State.IDLE;
        }
    }

    private void reg() throws NoEnoughDataException {
        checkAvailableData(commandPackage.getByte(0) + commandPackage.getByte(1));
        String incomingLogin = byteBuf.readCharSequence(commandPackage.getByte(0), StandardCharsets.UTF_8).toString();
        String incomingPass = passwordFormat(byteBuf.readCharSequence(commandPackage.getByte(1), StandardCharsets.UTF_8).toString());
        LogService.AUTH.info("Registration attempt", remoteAddress, "Login", incomingLogin);
        LoginRegError error = AuthService.registerAndEchoMsg(incomingLogin, incomingPass);
        if (error == null) {
            DataSocketWriter.sendCommand(ctx, CommandBytes.REG_OK);
            LogService.AUTH.info("Registration success", remoteAddress, "Login", incomingLogin);
        } else sendLoginRegError(error);
        state = State.IDLE;
    }

    private void auth() throws NoEnoughDataException, IOException {
        checkAvailableData(commandPackage.getByte(0) + commandPackage.getByte(1));
        String incomingLogin = byteBuf.readCharSequence(commandPackage.getByte(0), StandardCharsets.UTF_8).toString();
        String incomingPass = passwordFormat(byteBuf.readCharSequence(commandPackage.getByte(1), StandardCharsets.UTF_8).toString());
        LogService.AUTH.info("Auth attempt", remoteAddress, "Login", incomingLogin);
        id = AuthService.checkLogin(incomingLogin, incomingPass);
        if (id == null) sendLoginRegError(LoginRegError.INCORRECT_LOGIN_PASS);
        else if (server.isUserOnline(incomingLogin)) sendLoginRegError(LoginRegError.LOGGED_ALREADY);
        else {
            login = incomingLogin;
            logged = true;
            DataSocketWriter.sendCommand(ctx, CommandBytes.AUTH_OK, id);
            setUserRepo();
            downloader = new FileDownloader(rootDir, byteBuf);
            LogService.AUTH.info("Auth success", remoteAddress, "Login", login);
        }
        state = State.IDLE;
    }

    // TODO: 14.02.2020 переделать на пересылку шифрованного пароля и избавиться от этоого метода
    private String passwordFormat(String pass) {
        return pass.trim()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("'", "\\'");
    }

    private void sendFilesList() throws IOException {
        LogService.USERS.info("Sending files list to user", login);
        List<Path> list = Files.list(rootDir).sorted(Comparator.naturalOrder()).collect(Collectors.toList());
        DataSocketWriter.sendCommand(ctx, CommandBytes.FILES_LIST, list.size());
        for (Path file : list) {
            FileUploader.sendFileInfo(ctx, file);
        }
        LogService.USERS.info("Complete Sending files list to user", login);
        state = State.IDLE;
    }

    private void sendAllFiles() throws IOException {
        LogService.USERS.info("Start uploading all files to user", login);
        sendFilesList();
        sendFiles();
        LogService.USERS.info("Complete uploading all files to user", login);
    }

    // TODO: 16.02.2020 перенести в FileUploader после настройки логгера для модуля common
    private void sendFiles() throws IOException {
        List<Path> files = Files.list(rootDir)
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
        for (Path file : files) {
            sendFile(file);
        }
    }

    private void sendFile(Path file) {
        LogService.USERS.info("Uploading: " + file.getFileName().toString());
        if (FileUploader.upload(ctx, file)) {
            LogService.USERS.info("File upload success", file.getFileName().toString());
        } else {
            LogService.USERS.info("File upload failed", file.getFileName().toString());
        }
    }

    private void resolveFileRequest() throws NoEnoughDataException {
        String filename = downloader.downloadFileInfo().getName();
        LogService.USERS.info("Request file from user", login, filename);
        Path file = rootDir.resolve(filename);
        sendFile(file);
        state = State.IDLE;
    }

    private void sendLoginRegError(LoginRegError err) {
        LogService.AUTH.warn(remoteAddress, err.toString());
        DataSocketWriter.sendCommand(ctx, CommandBytes.ERROR, err.ordinal());
    }

    private void fileDownload() throws NoEnoughDataException {
        int result = downloader.download();
        if (result == 1) state = State.IDLE;
        else if (result == -1) {
            downloader.reset();
            state = State.IDLE;
        }
    }

    private void setUserRepo() throws IOException {
        rootDir = server.getRootDir().resolve(login);
        if (Files.notExists(rootDir)) Files.createDirectory(rootDir);
    }

    private void checkAvailableData(int length) throws NoEnoughDataException {
        if (byteBuf.readableBytes() < length) throw new NoEnoughDataException();
    }

    public void closeChannel() {
        ctx.channel().close();
    }

    public String getLogin() {
        return login;
    }

    private enum State {
        IDLE, COMMAND_SELECT, AUTH, REG, DOWNLOAD, FILE_REQUEST
    }
}
