package ru.kornev.cloudclient.app.handlers;

import io.netty.channel.ChannelHandlerContext;
import ru.kornev.cloudclient.resources.ClientSettings;
import ru.kornev.cloudclient.services.GUIForNetworkAdapter;
import ru.kornev.cloudclient.services.LogService;
import ru.kornev.cloudcommon.exceptions.NoEnoughDataException;
import ru.kornev.cloudcommon.resources.CommandBytes;
import ru.kornev.cloudcommon.resources.FileRepresentation;
import ru.kornev.cloudcommon.services.settings.Settings;
import ru.kornev.cloudcommon.services.transfer.CommandPackage;
import ru.kornev.cloudcommon.services.transfer.DataSocketWriter;
import ru.kornev.cloudcommon.services.transfer.FileDownloader;
import ru.kornev.cloudcommon.services.transfer.FileUploader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class FilesHandler {
    private ServerDataHandler dataHandler;
    private ChannelHandlerContext ctx;

    private FileDownloader downloader;
    private FileUploader uploader;
    private Path repoPath;
    private int filesListCount;
    private List<FileRepresentation> filesList;

    FilesHandler(ServerDataHandler dataHandler, ChannelHandlerContext ctx) throws IOException {
        this.dataHandler = dataHandler;
        this.ctx = ctx;
        this.repoPath = Paths.get(Settings.get(ClientSettings.ROOT_DIRECTORY));
        checkUserRepository();
        downloader = new FileDownloader(repoPath, dataHandler.getByteBuf(), true);
        uploader = new FileUploader(true);
    }

    private void checkUserRepository() throws IOException {
        try {
            if (!Files.exists(repoPath)) Files.createDirectory(repoPath);
        } catch (IOException e) {
            LogService.CLIENT.error("Error while creating repository directory", e.toString());
            throw e;
        }
    }

    void uploadFiles() {
        dataHandler.setupProgressProperty(uploader.getProgress());
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
    void uploadFile(String filename) {
        dataHandler.setupProgressProperty(uploader.getProgress());
        Path path = repoPath.resolve(filename);
        if(!Files.exists(path)) {
            GUIForNetworkAdapter.getInstance().localFileNotExist(filename);
            return;
        }
        uploadFile(path);
    }

    private void uploadFile(Path file) {
        String filename = file.getFileName().toString();
        GUIForNetworkAdapter.getInstance().log("File upload starts: " + filename);
        if (uploader.upload(ctx, file)) {
            LogService.CLIENT.info("File upload success", filename);
            GUIForNetworkAdapter.getInstance().log("File upload success: " + filename);
        } else {
            LogService.CLIENT.info("File upload failed", filename);
            GUIForNetworkAdapter.getInstance().log("File upload failed: " + filename);
        }
    }

    void fileDownload() throws NoEnoughDataException {
        int result = downloader.download();
        if (result == 1 || result == -1) {
            downloader.reset();
            dataHandler.downloadFinish();
            GUIForNetworkAdapter.getInstance().setDownloadComplete(result == 1);
        }
    }

    List<FileRepresentation> getClientFilesList() {
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

    void sendFilesListRequest() {
        GUIForNetworkAdapter.getInstance().log("Request files list from server");
        DataSocketWriter.sendCommand(ctx, CommandBytes.FILES_LIST);
    }

    void sendAllFilesRequest() {
        GUIForNetworkAdapter.getInstance().log("Request all files from server");
        DataSocketWriter.sendCommand(ctx, CommandBytes.FILES);
    }

    void sendFileRequest(String filename) {
        GUIForNetworkAdapter.getInstance().log("Downloading request - " + filename);
        DataSocketWriter.sendCommand(ctx, CommandBytes.FILE);
        uploader.sendFileInfo(ctx, filename);
    }

    void filesListGettingPrepare(CommandPackage commandPackage) {
        filesListCount = commandPackage.getIntCommandData();
        filesList = new ArrayList<>();
        downloader.reset();
    }

    void getFilesList() throws NoEnoughDataException {
        while (filesListCount > 0) {
            FileRepresentation file = downloader.downloadFileRepresentation();
            if (file == null) {
                GUIForNetworkAdapter.getInstance().log("Files list update from server failed");
                LogService.SERVER.error("Files list update from server failed", filesList.toString(), "filesListCount - " + filesListCount);
                downloader.reset();
                break;
            }
            filesList.add(file);
            filesListCount--;
            downloader.reset();
        }
        GUIForNetworkAdapter.getInstance().filesListGettingComplete(filesList);
        downloader.reset();
    }

    void deleteLocalFile(FileRepresentation file) {
        GUIForNetworkAdapter.getInstance().log("Deleting local file: " + file.getName());
        try {
            Files.deleteIfExists(repoPath.resolve(file.getName()));
        } catch (IOException e) {
            LogService.CLIENT.error("Error when deleting file", file.getName());
        }
    }

    void deleteFileFromServer(FileRepresentation file) {
        GUIForNetworkAdapter.getInstance().log("Deleting file from server: " + file.getName());
        DataSocketWriter.sendCommand(ctx, CommandBytes.DELETE);
        uploader.sendFileInfo(ctx, file.getName());
    }

    void downloadPrepare() {
        dataHandler.setupProgressProperty(downloader.getProgress());
        GUIForNetworkAdapter.getInstance().setDownloadInProgress();
    }
}