package app;

import exceptions.NoEnoughDataException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import resources.CommandBytes;
import resources.LoginRegError;
import services.*;
import settings.GlobalSettings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class ClientHandler {

    private enum State {
        IDLE, COMMAND_SELECT, AUTH, REG, DOWNLOAD, FILES_LIST;
    }

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
    private byte[] commandData;
    private boolean logged;

    public ClientHandler(MainServer server, ChannelHandlerContext ctx, ByteBuf byteBuf) {
        this.server = server;
        this.ctx = ctx;
        this.remoteAddress = ctx.channel().remoteAddress().toString();
        this.byteBuf = byteBuf;
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
        if (state == State.IDLE) listenPackageStart();
        else if (state == State.COMMAND_SELECT) selectCommandState();
        else if (state == State.REG) reg();
        else if (state == State.AUTH) auth();
        else if (state == State.DOWNLOAD && logged) fileDownload();
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

    private void selectCommandState() {
        checkAvailableData(GlobalSettings.COMMAND_DATA_LENGTH + 1);
        byte b = byteBuf.readByte();
        if (CommandBytes.AUTH.check(b)) state = State.AUTH;
        else if (CommandBytes.REG.check(b)) state = State.REG;
        else if (CommandBytes.FILELIST.check(b) && logged) state = State.FILES_LIST;
        else {
            state = State.IDLE;
            return;
        }
        commandData = new byte[GlobalSettings.COMMAND_DATA_LENGTH];
        byteBuf.readBytes(commandData);
    }

    private void reg() {
        checkAvailableData(commandData[0] + commandData[1]);
        try {
            String incomingLogin = byteBuf.readCharSequence(commandData[0], StandardCharsets.UTF_8).toString();
            String incomingPass = passwordFormat(byteBuf.readCharSequence(commandData[1], StandardCharsets.UTF_8).toString());
            LogService.AUTH.info("Registration attempt", remoteAddress, "Login", incomingLogin);
            LoginRegError error = AuthService.registerAndEchoMsg(incomingLogin, incomingPass);
            if (error == null) {
                DataSocketWriter.sendCommand(ctx, CommandBytes.REG_OK);
                LogService.AUTH.info("Registration success", remoteAddress, "Login", incomingLogin);
            } else sendLoginRegError(error);
        } catch (Exception e) {
            LogService.USERS.fatal(remoteAddress, e.getLocalizedMessage());
        } finally {
            state = State.IDLE;
        }
    }

    private void auth() {
        checkAvailableData(commandData[0] + commandData[1]);
        try {
            String incomingLogin = byteBuf.readCharSequence(commandData[0], StandardCharsets.UTF_8).toString();
            String incomingPass = passwordFormat(byteBuf.readCharSequence(commandData[1], StandardCharsets.UTF_8).toString());
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
        } catch (IOException e) {
            LogService.SERVER.error(login, e.toString());
        } catch (NoEnoughDataException e) {
            LogService.USERS.fatal(remoteAddress, e.getLocalizedMessage());
        } finally {
            state = State.IDLE;
        }
    }

    // TODO: 14.02.2020 переделать на пересылку шифрованного пароля и избавиться от этоого метода
    private String passwordFormat(String pass) {
        return pass.trim()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("'", "\\'");
    }

    private void sendFilesList() throws IOException, NoEnoughDataException, InterruptedException {
        List<Path> list = Files.list(rootDir).collect(Collectors.toList());
        DataSocketWriter.sendCommand(ctx, CommandBytes.FILELIST, list.size());
        for (Path file : list) {
            FileUploader.writeFileInfo(ctx, file);
        }
    }

    private void sendLoginRegError(LoginRegError err) throws NoEnoughDataException {
        LogService.AUTH.warn(remoteAddress, err.toString());
        DataSocketWriter.sendCommand(ctx, CommandBytes.ERROR, err.ordinal());
    }

    private void fileDownload() {
        try {
            int result = downloader.download();
            if (result == 1) state = State.IDLE;
                // TODO: 14.02.2020 обработать ошибку
            else if (result == -1) {
                downloader.reset();
                state = State.IDLE;
            }
        } catch (NoEnoughDataException e) {
            noEnoughBytes = true;
        }
    }

//        } catch (NoSuchAlgorithmException e) {
//            LogService.SERVER.error(login, "Checksum algorithm error", e.toString());
//            LogService.USERS.error(login, "Checksum algorithm error", e.toString());
//        }
//
//    private boolean downloadFile() throws IOException, NoSuchAlgorithmException {
//        LogService.USERS.info(login, "Package start checked");
//        LogService.USERS.info(login, "Downloading", name);
//        if (!downloadFileData(name)) {
//            LogService.USERS.info(login, "Download failed", name);
//            return false;
//        }
//        LogService.USERS.info(login, "Download complete", name);
//        return true;
//    }
//
//
//

    private void setUserRepo() throws IOException {
        rootDir = MainServer.REPOSITORY_ROOT.resolve(login);
        if (Files.notExists(rootDir)) Files.createDirectory(rootDir);
    }

    private boolean checkAvailableData(int length) {
        if (byteBuf.readableBytes() < length) {
            noEnoughBytes = true;
            return false;
        }
        return true;
    }

    public void closeChannel() {
        ctx.channel().close();
    }

    public String getLogin() {
        return login;
    }
}
