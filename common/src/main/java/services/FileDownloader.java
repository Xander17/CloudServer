package services;

import exceptions.NoEnoughDataException;
import io.netty.buffer.ByteBuf;
import settings.GlobalSettings;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

// TODO: 14.02.2020 прикрутить логсервис вместо sout
public class FileDownloader {
    private enum State {
        IDLE, FILENAME_LENGTH, FILENAME, FILE_LENGTH, FILE_DATA, CHECKSUM, FAIL, SUCCESS
    }

    private final int BUFFER_SIZE = 8192;

    private Path rootDir;
    private ByteBuf byteBuf;

    private int filenameLen;
    private String filename;
    private long fileLen;
    private byte[] checksum;
    private State state;

    public FileDownloader(Path rootDir, ByteBuf byteBuf) {
        this.rootDir = rootDir;
        this.byteBuf = byteBuf;
        this.state = State.IDLE;
    }

    public int download() throws NoEnoughDataException {
        if (state == State.FILENAME_LENGTH) readFilenameLen();
        else if (state == State.FILENAME) readFilename();
        else if (state == State.FILE_LENGTH) readFileLen();
        else if (state == State.FILE_DATA) downloadFileData();
        else if (state == State.CHECKSUM) readChecksum();
        if (state == State.SUCCESS) return 1;
        else if (state == State.FAIL) return -1;
        else return 0;
    }

    private void readFilenameLen() throws NoEnoughDataException {
        checkAvailableData(Short.BYTES);
        filenameLen = byteBuf.readShort();
        if (filenameLen <= 0) state = State.FAIL;
        else state = State.FILENAME;
        System.out.println("Checked filename len - " + filenameLen);
    }

    private void readFilename() throws NoEnoughDataException {
        checkAvailableData(filenameLen);
        filename = byteBuf.readCharSequence(filenameLen, StandardCharsets.UTF_8).toString();
        state = State.FILE_LENGTH;
        System.out.println("Checked filename - " + filename);
    }

    private void readFileLen() throws NoEnoughDataException {
        checkAvailableData(Long.BYTES);
        fileLen = byteBuf.readLong();
        if (filenameLen <= 0) state = State.FAIL;
        else state = State.FILE_DATA;
        System.out.println("Checked file len - " + fileLen);
    }

    private void downloadFileData() throws NoEnoughDataException {
        System.out.println("Downloading");
        try (FileOutputStream out = new FileOutputStream(rootDir.resolve(filename).toFile(), true)) {
            byte[] bytes = new byte[BUFFER_SIZE];
            MessageDigest md = MessageDigest.getInstance(GlobalSettings.CHECKSUM_PROTOCOL);
            while (fileLen > 0) {
                int blockSize = fileLen >= BUFFER_SIZE ? BUFFER_SIZE : (int) fileLen;
                checkAvailableData(1);
                if (byteBuf.readableBytes() < blockSize) blockSize = byteBuf.readableBytes();
                byteBuf.readBytes(bytes);
                out.write(bytes, 0, blockSize);
                md.update(bytes, 0, blockSize);
                fileLen -= blockSize;
            }
            checksum = md.digest();
            state = State.CHECKSUM;
        } catch (NoSuchAlgorithmException e) {
            System.out.println(e.toString());
        } catch (FileNotFoundException e) {
            System.out.println(e.toString());
        } catch (IOException e) {
            System.out.println(e.toString());
        }
        System.out.println("Download complete");
        System.out.println("Checksum - " + new String(checksum));
    }

    private void readChecksum() throws NoEnoughDataException {
        checkAvailableData(GlobalSettings.CHECKSUM_LENGTH);
        for (int i = 0; i < GlobalSettings.CHECKSUM_LENGTH; i++) {
            if (byteBuf.readByte() != checksum[i]) {
                System.out.println("Checksum error");
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
        state = State.FILENAME_LENGTH;
    }

    private void checkAvailableData(int length) throws NoEnoughDataException {
        if (byteBuf.readableBytes() < length) throw new NoEnoughDataException();
    }
}
