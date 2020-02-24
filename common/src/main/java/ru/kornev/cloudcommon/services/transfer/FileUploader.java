package ru.kornev.cloudcommon.services.transfer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import ru.kornev.cloudcommon.resources.CommandBytes;
import ru.kornev.cloudcommon.services.LogServiceCommon;
import ru.kornev.cloudcommon.services.transfer.resources.Progress;
import ru.kornev.cloudcommon.settings.GlobalSettings;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileUploader {

    // TODO: 24.02.2020 добавить в настройки 
    private final int BUFFER_SIZE = 8192;

    private Progress progress;
    private boolean progressNeed;

    public FileUploader(boolean progressNeed) {
        this.progressNeed = progressNeed;
        if (progressNeed) this.progress = new Progress();
    }

    public boolean upload(ChannelHandlerContext ctx, Path file) {
        try {
            writeFileStartByte(ctx);
            writeFileInfo(ctx, file);
            writeFileData(ctx, file);
        } catch (InterruptedException e) {
            LogServiceCommon.TRANSFER.error("Upload error - " + file.getFileName().toString());
            LogServiceCommon.TRANSFER.error(e);
            return false;
        } catch (NoSuchAlgorithmException e) {
            LogServiceCommon.TRANSFER.error("Checksum calculation error - " + file.getFileName().toString());
            LogServiceCommon.TRANSFER.error(e);
            return false;
        } catch (IOException e) {
            LogServiceCommon.TRANSFER.error("File read error - " + file.getFileName().toString());
            LogServiceCommon.TRANSFER.error(e);
            return false;
        } finally {
            if (progressNeed) progress.resetProgress();
        }
        return true;
    }

    private void writeFileStartByte(ChannelHandlerContext ctx) {
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer();
        buf.writeByte(CommandBytes.PACKAGE_START.getByte());
        ctx.writeAndFlush(buf);
    }

    public void sendFileInfo(ChannelHandlerContext ctx, String filename) {
        writeFileInfo(ctx, filename, 0L, 0L);
    }

    public void sendFileInfo(ChannelHandlerContext ctx, Path file) throws IOException {
        writeFileInfo(ctx, file);
    }

    private void writeFileInfo(ChannelHandlerContext ctx, Path file) throws IOException {
        writeFileInfo(ctx, file.getFileName().toString(), Files.size(file), Files.getLastModifiedTime(file).toMillis());
    }

    private void writeFileInfo(ChannelHandlerContext ctx, String filename, long fileLength, long fileDate) {
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer();
        byte[] bytes = filename.getBytes(StandardCharsets.UTF_8);
        buf.writeShort((short) bytes.length);
        buf.writeBytes(bytes);
        buf.writeLong(fileLength);
        buf.writeLong(fileDate);
        ctx.writeAndFlush(buf);
        if (progressNeed) progress.setMaxValue(fileLength);
    }

    private void writeFileData(ChannelHandlerContext ctx, Path file) throws NoSuchAlgorithmException, IOException, InterruptedException {
        MessageDigest md = MessageDigest.getInstance(GlobalSettings.CHECKSUM_PROTOCOL);
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer();
        byte[] bytes = new byte[BUFFER_SIZE];
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(file.toFile()));
        int blockSize;
        while ((blockSize = in.read(bytes)) != -1) {
            buf.retain();
            buf.writeBytes(bytes, 0, blockSize);
            md.update(bytes, 0, blockSize);
            ctx.writeAndFlush(buf).sync();
            buf.clear();
            if (progressNeed) progress.addProgress(blockSize);
        }
        buf.writeBytes(md.digest(), 0, GlobalSettings.CHECKSUM_LENGTH);
        ctx.writeAndFlush(buf);
        in.close();
    }

    public Progress getProgress() {
        return progress;
    }
}