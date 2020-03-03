package ru.kornev.cloudnetwork.network;

import io.netty.channel.ChannelHandlerContext;
import ru.kornev.cloudcommon.exceptions.NoEnoughDataException;
import ru.kornev.cloudcommon.resources.CommandBytes;
import ru.kornev.cloudcommon.resources.FileRepresentation;
import ru.kornev.cloudcommon.services.transfer.*;
import ru.kornev.cloudnetwork.resources.NetworkSettings;
import ru.kornev.cloudnetwork.services.LogService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

class FilesHandler {
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
        this.repoPath = Paths.get(Network.getSettings().get(NetworkSettings.ROOT_DIRECTORY));
        checkUserRepository();
        Progress progress = new Progress();
        int bufferSize = Network.getSettings().getInt(NetworkSettings.DOWNLOAD_BUFFER_SIZE);
        downloader = new FileDownloader(repoPath, dataHandler.getByteBuf(), bufferSize, progress);
        downloader.setMessageCallback(CallbackHandler.getOnServerMessage());
        bufferSize = Network.getSettings().getInt(NetworkSettings.UPLOAD_BUFFER_SIZE);
        uploader = new FileUploader(bufferSize, progress);
        CallbackHandler.onProgressPropertyRegister(progress);
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
        dataHandler.setWaitingThreadState(true);
        new Thread(() -> {
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
            dataHandler.setWaitingThreadState(false);
            CallbackHandler.onUploadFinish();
        }).start();
    }

    void uploadFile(FileRepresentation file) {
        if (!Files.exists(file.getPath())) {
            CallbackHandler.onUploadFileNotExists();
            CallbackHandler.onServerMessage("Local file " + file.getName() + " is not exist");
            return;
        }
        dataHandler.setWaitingThreadState(true);
        new Thread(() -> {
            uploadFile(file.getPath());
            dataHandler.setWaitingThreadState(false);
            CallbackHandler.onUploadFinish();
        }).start();
    }

    private void uploadFile(Path file) {
        String filename = file.getFileName().toString();
        CallbackHandler.onServerMessage("File upload starts: " + filename);
        if (uploader.upload(ctx, file)) {
            LogService.CLIENT.info("File upload success", filename);
            CallbackHandler.onServerMessage("File upload success: " + filename);
        } else {
            LogService.CLIENT.info("File upload failed", filename);
            CallbackHandler.onServerMessage("File upload failed: " + filename);
        }
    }

    void fileDownload() throws NoEnoughDataException {
        int result = downloader.download();
        if (result == 1 || result == -1) {
            downloader.reset();
            dataHandler.downloadFinish();
        }
    }

    List<FileRepresentation> getLocalFilesList() {
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
        CallbackHandler.onServerMessage("Request files list from server");
        CommandSender.sendCommand(ctx, CommandBytes.FILES_LIST);
    }

    void sendAllFilesRequest() {
        CallbackHandler.onServerMessage("Request all files from server");
        CommandSender.sendCommand(ctx, CommandBytes.FILES);
    }

    void sendFileRequest(String filename) {
        CallbackHandler.onServerMessage("Downloading request - " + filename);
        CommandSender.sendCommand(ctx, CommandBytes.FILE);
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
                CallbackHandler.onServerMessage("Files list update from server failed");
                LogService.SERVER.error("Files list update from server failed", filesList.toString(), "filesListCount - " + filesListCount);
                downloader.reset();
                break;
            }
            filesList.add(file);
            filesListCount--;
            downloader.reset();
        }
        downloader.reset();
        CallbackHandler.onFileListDownloadFinish(filesList);
    }

    void deleteLocalFile(FileRepresentation file) {
        CallbackHandler.onServerMessage("Deleting local file: " + file.getName());
        try {
            Files.deleteIfExists(repoPath.resolve(file.getName()));
        } catch (IOException e) {
            LogService.CLIENT.error("Error when deleting file", file.getName());
        }
    }

    void deleteFileFromServer(FileRepresentation file) {
        CallbackHandler.onServerMessage("Deleting file from server: " + file.getName());
        CommandSender.sendCommand(ctx, CommandBytes.DELETE);
        uploader.sendFileInfo(ctx, file.getName());
    }
}