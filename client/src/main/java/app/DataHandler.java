package app;

import exceptions.NoEnoughDataException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import resources.CommandBytes;
import services.*;
import settings.GlobalSettings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class DataHandler {
    private final Path REPOSITORY_DIRECTORY = Paths.get("client-repo");
    private ChannelHandlerContext ctx;
    private ByteBuf byteBuf;
    private Controller controller;
    private FileDownloader downloader;
    private State state;
    private boolean noEnoughBytes;
    private boolean logged;
    private int filesListCount;
    private List<String> filesList;
    private CommandPackage commandPackage;
    public DataHandler(ChannelHandlerContext ctx, ByteBuf byteBuf, Controller controller) {
        this.ctx = ctx;
        this.byteBuf = byteBuf;
        this.controller = controller;
        this.logged = false;
        this.state = State.IDLE;
        controller.setDataHandler(this);
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
        else if (state == State.COMMAND_SELECT) selectCommandState();
            //else if (state == State.DOWNLOAD && logged) fileDownload();
        else if (state == State.FILES_LIST && logged) getFilesList();
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
        commandPackage = new CommandPackage(byteBuf);
        if (CommandBytes.AUTH_OK.check(commandPackage.getCommand())) {
            setAuthSuccess();
        } else if (CommandBytes.REG_OK.check(commandPackage.getCommand())) {
            setRegSuccess();
        } else if (CommandBytes.ERROR.check(commandPackage.getCommand())) {
            setRegAuthError();
        } else if (CommandBytes.FILELIST.check(commandPackage.getCommand()) && logged) {
            filesListCount = commandPackage.getInt();
            filesList = new ArrayList<>();
            state = State.FILES_LIST;
        } else state = State.IDLE;
    }

    private void setRegSuccess() {
        controller.setRegSuccess();
        state = State.IDLE;
    }

    private void setAuthSuccess() {
        controller.setAuthSuccess(commandPackage.getInt());
        downloader = new FileDownloader(REPOSITORY_DIRECTORY, byteBuf);
        state = State.IDLE;
        logged = true;
    }

    private void setRegAuthError() {
        controller.setRegAuthError(commandPackage.getInt());
        state = State.IDLE;
    }

    public void sendRegAuthData(CommandBytes command, String login, String pass) {
        if (!CommandBytes.REG.equals(command) && !CommandBytes.AUTH.equals(command)) return;
        byte[] loginBytes = login.getBytes();
        byte[] passBytes = pass.getBytes();
        DataSocketWriter.sendCommand(ctx, command, (byte) (loginBytes.length), (byte) (passBytes.length));
        DataSocketWriter.sendData(ctx, loginBytes, passBytes);
    }

    public void uploadFiles() throws IOException {
        List<Path> files = Files.list(REPOSITORY_DIRECTORY)
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
        for (Path file : files) {
            System.out.println("Uploading: " + file.getFileName());
            if (FileUploader.upload(ctx, file)) {
                LogService.CLIENT.info("File upload success", file.getFileName().toString());
                System.out.println("File upload success: " + file.getFileName());
            } else {
                LogService.CLIENT.info("File upload failed", file.getFileName().toString());
                System.out.println("File upload failed: " + file.getFileName());
            }
        }
    }

    public List<String> getClientFilesList() {
        try {
            return Files.list(REPOSITORY_DIRECTORY)
                    .map(file -> file.getFileName().toString())
                    .sorted(Comparator.naturalOrder())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LogService.CLIENT.error("Error while getting files list", e.toString());
            return null;
        }
    }

    public void sendFilesListRequest() {
        DataSocketWriter.sendCommand(ctx, CommandBytes.FILELIST);
    }

    private void getFilesList() {
        try {
            while (filesListCount > 0) {
                String filename = downloader.downloadFileName();
                filesList.add(filename);
                filesListCount--;
            }
        } catch (NoEnoughDataException e) {
            noEnoughBytes = true;
        }
        if (filesListCount == 0) {
            controller.updateServerList(filesList);
            state = State.IDLE;
            controller.setButtonsDisable(false);
        }
    }

    private boolean checkAvailableData(int length) {
        if (byteBuf.readableBytes() < length) {
            noEnoughBytes = true;
            return false;
        }
        return true;
    }

    public void closeChannel() {
        //ctx.close();
        ctx.channel().close();
    }

    private enum State {
        IDLE, COMMAND_SELECT, DOWNLOAD, FILES_LIST
    }
}
