package services;

import exceptions.NoEnoughDataException;
import io.netty.buffer.ByteBuf;
import resources.FileRepresentation;
import settings.GlobalSettings;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

// TODO: 14.02.2020 прикрутить логсервис вместо sout

//            LogService.SERVER.error(login, "Checksum algorithm error", e.toString());
//            LogService.USERS.error(login, "Checksum algorithm error", e.toString());
//        LogService.USERS.info(login, "Package start checked");
//        LogService.USERS.info(login, "Downloading", name);
//            LogService.USERS.info(login, "Download failed", name);
//        LogService.USERS.info(login, "Download complete", name);

public class FileDownloader {
    private final int BUFFER_SIZE = 8192;
    private Path rootDir;
    private ByteBuf byteBuf;
    private int filenameLen;
    private String filename;
    private long fileDate;
    private long fileLen;
    private Path file;
    private byte[] checksum;
    private State state;
    private FileOutputStream out;
    private MessageDigest md;
    private boolean fileInfoOnly;
    private byte[] readBytes;

    public FileDownloader(Path rootDir, ByteBuf byteBuf) {
        this.rootDir = rootDir;
        this.byteBuf = byteBuf;
        this.fileInfoOnly = false;
        this.state = State.IDLE;
        this.readBytes = new byte[BUFFER_SIZE];
        startChecksumCounter();
    }

    public int download() throws NoEnoughDataException {
        try {
            if (state == State.FILENAME_LENGTH) readFilenameLen();
            if (state == State.FILE_INFO) readFilename();
            if (state == State.FILE_DATA) downloadFileData();
            if (state == State.CHECKSUM) readChecksum();
        } catch (IOException e) {
            e.printStackTrace();
            closeFileForWrite();
            state = State.FAIL;
        }
        return getErrorCode();
    }

    private int getErrorCode() {
        if (state == State.SUCCESS) return 1;
        else if (state == State.FAIL) return -1;
        else return 0;
    }

    public FileRepresentation downloadFileInfo() throws NoEnoughDataException {
        fileInfoOnly = true;
        state = State.FILENAME_LENGTH;
        if (download() == 1) {
            fileInfoOnly = false;
            return new FileRepresentation(filename, fileLen, fileDate);
        } else return null;
    }

    private void readFilenameLen() throws NoEnoughDataException {
        checkAvailableData(Short.BYTES);
        filenameLen = byteBuf.readShort();
        if (filenameLen <= 0) state = State.FAIL;
        else state = State.FILE_INFO;
        System.out.println("Checked filename len - " + filenameLen);
    }

    private void readFilename() throws NoEnoughDataException, IOException {
        checkAvailableData(filenameLen + 2 * Long.BYTES);
        filename = byteBuf.readCharSequence(filenameLen, StandardCharsets.UTF_8).toString();
        file = rootDir.resolve(filename);
        fileLen = byteBuf.readLong();
        if (filenameLen < 0) {
            state = State.FAIL;
            return;
        }
        fileDate = byteBuf.readLong();
        if (fileInfoOnly) state = State.SUCCESS;
        else {
            state = State.FILE_DATA;
            System.out.println("Downloading");
            openFileForWrite();
        }
        System.out.println("Checked filename - " + filename);
        System.out.println("Checked file len - " + fileLen);
    }

    private void downloadFileData() throws NoEnoughDataException, IOException {
        while (fileLen > 0) {
            int blockSize = fileLen >= BUFFER_SIZE ? BUFFER_SIZE : (int) fileLen;
            checkAvailableData(1);
            if (byteBuf.readableBytes() < blockSize) blockSize = byteBuf.readableBytes();
            byteBuf.readBytes(readBytes, 0, blockSize);
            out.write(readBytes, 0, blockSize);
            md.update(readBytes, 0, blockSize);
            fileLen -= blockSize;
        }
        checksum = md.digest();
        state = State.CHECKSUM;
        closeFileForWrite();
        System.out.println("Download complete");
    }

    private void readChecksum() throws NoEnoughDataException, IOException {
        checkAvailableData(GlobalSettings.CHECKSUM_LENGTH);
        for (int i = 0; i < GlobalSettings.CHECKSUM_LENGTH; i++) {
            if (byteBuf.readByte() != checksum[i]) {
                System.out.println("Checksum error");
                Files.deleteIfExists(file);
                state = State.FAIL;
                return;
            }
        }
        state = State.SUCCESS;
        System.out.println("Checksum OK");
    }

    public void reset() {
        filenameLen = 0;
        fileLen = 0;
        filename = null;
        file = null;
        fileInfoOnly = false;
        state = State.FILENAME_LENGTH;
        readBytes = new byte[BUFFER_SIZE];
        md.reset();
        closeFileForWrite();
    }

    private void checkAvailableData(int length) throws NoEnoughDataException {
        if (byteBuf.readableBytes() < length) throw new NoEnoughDataException();
    }

    // TODO: 15.02.2020 нужна проверка контрольной суммы на сервере, чтобы не закачивать файл повторно
    private void openFileForWrite() throws IOException {
        out = new FileOutputStream(file.toFile());
    }

    private void startChecksumCounter() {
        try {
            this.md = MessageDigest.getInstance(GlobalSettings.CHECKSUM_PROTOCOL);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private void closeFileForWrite() {
        try {
            if (out != null) out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private enum State {
        IDLE, FILENAME_LENGTH, FILE_INFO, FILE_DATA, CHECKSUM, FAIL, SUCCESS
    }
}
