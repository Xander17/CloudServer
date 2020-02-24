package app;

import exceptions.NoEnoughDataException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import resources.ClientSettings;
import resources.CommandBytes;
import resources.FileRepresentation;
import services.GUIForNetworkAdapter;
import services.LogService;
import services.NetworkForGUIAdapter;
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
        try {
            while (byteBuf.readableBytes() > 0) {
                stateExecute();
            }
        } catch (NoEnoughDataException ignored) {
        }
    }

    private void stateExecute() throws NoEnoughDataException {
        if (state == State.IDLE) listenPackageStart();
        if (state == State.COMMAND_SELECT) selectCommandState();
        if (logged) stateLoggedExecute();
    }

    private void stateLoggedExecute() throws NoEnoughDataException {
        if (state == State.DOWNLOAD) fileDownload();
        else if (state == State.FILES_LIST) getFilesList();
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
        FileDownloader.checkAvailableData(byteBuf, GlobalSettings.COMMAND_DATA_LENGTH + 1);
        commandPackage.load();
        if (CommandBytes.AUTH_OK.check(commandPackage.getCommand())) {
            setAuthSuccess();
        } else if (CommandBytes.REG_OK.check(commandPackage.getCommand())) {
            setRegSuccess();
        } else if (CommandBytes.ERROR.check(commandPackage.getCommand())) {
            setRegAuthError();
        } else if (logged) {
            selectLoggedCommandState();
        } else state = State.IDLE;
    }

    private void selectLoggedCommandState() {
        if (CommandBytes.FILES_LIST.check(commandPackage.getCommand()) && logged) {
            filesListCount = commandPackage.getInt();
            filesList = new ArrayList<>();
            state = State.FILES_LIST;
        }
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
            LogService.CLIENT.error("Sending file error", e.toString());
        }
    }

    // TODO: 22.02.2020 сохранить path в FileRepresentation и удалить этот метод
    public void uploadFile(String filename) {
        Path path = repoPath.resolve(filename);

        uploadFile(path);
    }

    public void uploadFile(Path file) {
        String filename = file.getFileName().toString();
        GUIForNetworkAdapter.getInstance().log("File upload starts: " + filename);
        if (FileUploader.upload(ctx, file)) {
            LogService.CLIENT.info("File upload success", filename);
            GUIForNetworkAdapter.getInstance().log("File upload success: " + filename);
        } else {
            LogService.CLIENT.info("File upload failed", filename);
            GUIForNetworkAdapter.getInstance().log("File upload failed: " + filename);
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
        GUIForNetworkAdapter.getInstance().log("Request files list from server");
        DataSocketWriter.sendCommand(ctx, CommandBytes.FILES_LIST);
    }

    public void sendAllFilesRequest() {
        GUIForNetworkAdapter.getInstance().log("Request all files from server");
        DataSocketWriter.sendCommand(ctx, CommandBytes.FILES);
    }

    public void sendFileRequest(String filename) {
        GUIForNetworkAdapter.getInstance().log("Downloading request - " + filename);
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

    public void deleteLocalFile(FileRepresentation file) {
        GUIForNetworkAdapter.getInstance().log("Deleting local file: " + file.getName());
        try {
            Files.deleteIfExists(repoPath.resolve(file.getName()));
        } catch (IOException e) {
            LogService.CLIENT.error("Error when deleting file", file.getName());
        }
    }

    public void deleteFileFromServer(FileRepresentation file) {
        GUIForNetworkAdapter.getInstance().log("Deleting file from server: " + file.getName());
        DataSocketWriter.sendCommand(ctx, CommandBytes.DELETE);
        FileUploader.sendFileInfo(ctx, file.getName());
    }

    private enum State {
        IDLE, COMMAND_SELECT, DOWNLOAD, FILES_LIST
    }
}
