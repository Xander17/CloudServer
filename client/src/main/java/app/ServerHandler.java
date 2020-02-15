package app;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import resources.CommandBytes;
import services.DataSocketWriter;
import services.FileDownloader;
import services.FileUploader;
import services.LogService;
import settings.GlobalSettings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;

public class ServerHandler {
    private enum State {
        IDLE, COMMAND_SELECT, DOWNLOAD, FILES_LIST;
    }

    private final Path REPOSITORY_DIRECTORY = Paths.get("client-repo");

    private ChannelHandlerContext ctx;
    private ByteBuf byteBuf;
    private Controller controller;
    private FileDownloader downloader;
    private State state;

    private boolean noEnoughBytes;
    private boolean logged;
    private int filesListCount;

    public ServerHandler(ChannelHandlerContext ctx, ByteBuf byteBuf, Controller controller) {
        this.ctx = ctx;
        this.byteBuf = byteBuf;
        this.controller = controller;
        this.logged = false;
        this.state = State.IDLE;
        controller.setServerHandler(this);
        try {
            if (!Files.exists(REPOSITORY_DIRECTORY)) Files.createDirectory(REPOSITORY_DIRECTORY);
        } catch (Exception e) {
            LogService.CLIENT.error("Error while creating repository directory", e.toString());
        }

    }

    public void handle() {
        noEnoughBytes = false;
        while (byteBuf.readableBytes() > 0 && !noEnoughBytes) {
            stateExecute();
        }
    }

    private void stateExecute() {
        if (state == State.IDLE) listenPackageStart();
        if (state == State.COMMAND_SELECT) selectCommandState();
            //else if (state == State.DOWNLOAD && logged) fileDownload();
        if (state == State.FILES_LIST && logged) getFilesList();
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
        if (CommandBytes.AUTH_OK.check(b)) setAuthSuccess();
        else if (CommandBytes.REG_OK.check(b)) setRegSuccess();
        else if (CommandBytes.ERROR.check(b)) setRegAuthError();
        else if (CommandBytes.FILELIST.check(b) && logged) {
            filesListCount = byteBuf.readInt();
            state = State.FILES_LIST;
        } else state = State.IDLE;
    }

    private void setRegSuccess() {
        byteBuf.readInt();
        controller.setRegSuccess();
    }

    private void setAuthSuccess() {
        controller.setAuthSuccess(byteBuf.readInt());
        try {
            uploadFiles();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setRegAuthError() {
        controller.setRegAuthError(byteBuf.readInt());
    }

    private void getFilesList() {

    }

    public void sendRegAuthData(CommandBytes b, String login, String pass) {
        if (!CommandBytes.REG.equals(b) && !CommandBytes.AUTH.equals(b)) return;
        byte[] loginBytes = login.getBytes();
        byte[] passBytes = pass.getBytes();
        DataSocketWriter.sendCommand(ctx, b, (byte) (loginBytes.length), (byte) (passBytes.length));
        DataSocketWriter.sendData(ctx, loginBytes, passBytes);
    }

    public void uploadFiles() throws IOException {
        Set<Path> files = Files.list(REPOSITORY_DIRECTORY).collect(Collectors.toSet());
        for (Path file : files) {
            System.out.println("Uploading: " + file.getFileName());
            if (FileUploader.upload(ctx, file)) {
                LogService.CLIENT.info("File upload success", file.getFileName().toString());
                System.out.println("File upload success: " + file.getFileName());
            }
            else {
                LogService.CLIENT.info("File upload failed", file.getFileName().toString());
                System.out.println("File upload failed: " + file.getFileName());
            }
        }
    }

    private boolean checkAvailableData(int length) {
        if (byteBuf.readableBytes() < length) {
            noEnoughBytes = true;
            return false;
        }
        return true;
    }
}
