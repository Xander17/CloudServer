package app;

import exceptions.NoEnoughDataException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import resources.ClientSettings;
import resources.CommandBytes;
import resources.FileRepresentation;
import services.*;
import services.settings.Settings;
import services.transfer.CommandPackage;
import services.transfer.DataSocketWriter;
import services.transfer.FileDownloader;
import services.transfer.FileUploader;
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
    private ChannelHandlerContext ctx;
    private ByteBuf byteBuf;
    private FileDownloader downloader;
    private State state;
    private boolean noEnoughBytes;
    private boolean logged;
    private int filesListCount;
    private List<FileRepresentation> filesList;
    private CommandPackage commandPackage;
    private Path repoPath;

    public DataHandler(ChannelHandlerContext ctx, ByteBuf byteBuf) {
        this.ctx = ctx;
        this.byteBuf = byteBuf;
        this.commandPackage = new CommandPackage(byteBuf);
        this.repoPath = Paths.get(Settings.get(ClientSettings.ROOT_DIRECTORY));
        this.logged = false;
        this.state = State.IDLE;
        try {
            if (!Files.exists(repoPath)) Files.createDirectory(repoPath);
        } catch (Exception e) {
            LogService.CLIENT.error("Error while creating repository directory", e.toString());
        }
        NetworkForGUIAdapter.setHandler(this);
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
            if (state == State.COMMAND_SELECT) selectCommandState();
            if (state == State.DOWNLOAD && logged) fileDownload();
            else if (state == State.FILES_LIST && logged) getFilesList();
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
                GUIForNetworkAdapter.getInstance().setDownloadInProgressState();
                state = State.DOWNLOAD;
                break;
            } else if (CommandBytes.COMMAND_START.check(b)) {
                state = State.COMMAND_SELECT;
                break;
            }
        }
    }

    private void selectCommandState() throws NoEnoughDataException {
        checkAvailableData(GlobalSettings.COMMAND_DATA_LENGTH + 1);
        commandPackage.load();
        if (CommandBytes.AUTH_OK.check(commandPackage.getCommand())) {
            setAuthSuccess();
        } else if (CommandBytes.REG_OK.check(commandPackage.getCommand())) {
            setRegSuccess();
        } else if (CommandBytes.ERROR.check(commandPackage.getCommand())) {
            setRegAuthError();
        } else if (CommandBytes.FILES_LIST.check(commandPackage.getCommand()) && logged) {
            filesListCount = commandPackage.getInt();
            filesList = new ArrayList<>();
            state = State.FILES_LIST;
        } else state = State.IDLE;
    }

    private void setRegSuccess() {
        GUIForNetworkAdapter.getInstance().setRegistrationSuccess();
        state = State.IDLE;
    }

    private void setAuthSuccess() {
        GUIForNetworkAdapter.getInstance().setAuthorizationSuccess();
        downloader = new FileDownloader(repoPath, byteBuf);
        state = State.IDLE;
        logged = true;
    }

    private void setRegAuthError() {
        GUIForNetworkAdapter.getInstance().setRegAuthError(commandPackage.getInt());
        state = State.IDLE;
    }

    public void signIn(String login, String pass) {
        sendRegAuthData(CommandBytes.AUTH, login, pass);
    }

    public void signUp(String login, String pass) {
        sendRegAuthData(CommandBytes.REG, login, pass);
    }

    private void sendRegAuthData(CommandBytes command, String login, String pass) {
        if (!CommandBytes.REG.equals(command) && !CommandBytes.AUTH.equals(command)) return;
        byte[] loginBytes = login.getBytes();
        byte[] passBytes = pass.getBytes();
        DataSocketWriter.sendCommand(ctx, command, (byte) (loginBytes.length), (byte) (passBytes.length));
        DataSocketWriter.sendData(ctx, loginBytes, passBytes);
    }

    // TODO: 16.02.2020 перенести в FileUploader после настройки логгера для модуля common
    public void uploadFiles() {
        try {
            List<Path> files = Files.list(repoPath)
                    .sorted(Comparator.naturalOrder())
                    .collect(Collectors.toList());
            for (Path file : files) {
                uploadFile(file);
            }
        } catch (IOException e) {
            LogService.CLIENT.error("Ошибка отправки файлов", e.toString());
        }
    }

    // TODO: 22.02.2020 сохранить path в FileRepresentation и удалить этот метод
    public void uploadFile(String file) {
        Path path = repoPath.resolve(file);
        uploadFile(path);
    }

    public void uploadFile(Path file) {
        System.out.println("Uploading: " + file.getFileName());
        if (FileUploader.upload(ctx, file)) {
            LogService.CLIENT.info("File upload success", file.getFileName().toString());
            System.out.println("File upload success: " + file.getFileName());
        } else {
            LogService.CLIENT.info("File upload failed", file.getFileName().toString());
            System.out.println("File upload failed: " + file.getFileName());
        }
    }

    private void fileDownload() throws NoEnoughDataException {
        int result = downloader.download();
        if (result == 1) {
            state = State.IDLE;
            GUIForNetworkAdapter.getInstance().setDownloadComplete();
        }
        // TODO: 14.02.2020 обработать ошибку
        else if (result == -1) {
            downloader.reset();
            state = State.IDLE;
        }
    }

    public List<FileRepresentation> getClientFilesList() {
        try {
            return Files.list(repoPath)
                    .sorted(Comparator.naturalOrder())
                    .map(FileRepresentation::new)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LogService.CLIENT.error("Error while getting files list", e.toString());
            return null;
        }
    }

    public void sendFilesListRequest() {
        DataSocketWriter.sendCommand(ctx, CommandBytes.FILES_LIST);
    }

    public void sendAllFilesRequest() {
        DataSocketWriter.sendCommand(ctx, CommandBytes.FILES);
    }

    public void sendFileRequest(String filename) {
        DataSocketWriter.sendCommand(ctx, CommandBytes.FILE);
        FileUploader.sendFileInfo(ctx, filename);
    }

    private void getFilesList() throws NoEnoughDataException {
        while (filesListCount > 0) {
            FileRepresentation file = downloader.downloadFileInfo();
            if (file == null) {
                GUIForNetworkAdapter.getInstance().log("Files list update from server failed");
                LogService.SERVER.error("Files list update from server failed", filesList.toString(), "filesListCount - " + filesListCount);
                state = State.IDLE;
                break;
            }
            filesList.add(file);
            filesListCount--;
        }
        GUIForNetworkAdapter.getInstance().updateServerFilesList(filesList);
        state = State.IDLE;
    }

    private void checkAvailableData(int length) throws NoEnoughDataException {
        if (byteBuf.readableBytes() < length) throw new NoEnoughDataException();
    }

    private enum State {
        IDLE, COMMAND_SELECT, DOWNLOAD, FILES_LIST
    }
}
