package services.transfer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import resources.CommandBytes;
import services.LogServiceCommon;
import settings.GlobalSettings;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileUploader {
    
    // TODO: 24.02.2020 добавить в настройки 
    private static int BUFFER_SIZE = 8192;

    public static boolean upload(ChannelHandlerContext ctx, Path file) {
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
        }
        return true;
    }

    private static void writeFileStartByte(ChannelHandlerContext ctx){
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer();
        buf.writeByte(CommandBytes.PACKAGE_START.getByte());
        ctx.writeAndFlush(buf);
    }

    public static void sendFileInfo(ChannelHandlerContext ctx, String filename){
        writeFileInfo(ctx, filename, 0L, 0L);
    }

    public static void sendFileInfo(ChannelHandlerContext ctx, Path file) throws IOException {
        writeFileInfo(ctx, file);
    }

    private static void writeFileInfo(ChannelHandlerContext ctx, Path file) throws IOException {
        writeFileInfo(ctx, file.getFileName().toString(), Files.size(file), Files.getLastModifiedTime(file).toMillis());
    }

    private static void writeFileInfo(ChannelHandlerContext ctx, String filename, long fileLength, long fileDate){
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer();
        byte[] bytes = filename.getBytes();
        buf.writeShort((short) bytes.length);
        buf.writeBytes(bytes);
        buf.writeLong(fileLength);
        buf.writeLong(fileDate);
        ctx.writeAndFlush(buf);
    }

    private static void writeFileData(ChannelHandlerContext ctx, Path file) throws NoSuchAlgorithmException, IOException, InterruptedException {
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
        }
        buf.writeBytes(md.digest(), 0, GlobalSettings.CHECKSUM_LENGTH);
        ctx.writeAndFlush(buf);
        in.close();
    }
}

