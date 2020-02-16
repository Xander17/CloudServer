package services;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import resources.CommandBytes;
import settings.GlobalSettings;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileUploader {

    private static int BUFFER_SIZE = 8192;

    public static boolean upload(ChannelHandlerContext ctx, Path file) {
        try {
            writeFileStartByte(ctx);
            writeFileInfo(ctx, file);
            writeFileData(ctx, file);
        } catch (InterruptedException e) {
            System.out.println("Upload error");
            e.printStackTrace();
            return false;
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Checksum calculation error");
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            System.out.println("File read error");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static void writeFileStartByte(ChannelHandlerContext ctx) throws InterruptedException {
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer();
        buf.writeByte(CommandBytes.PACKAGE_START.getByte());
        ctx.writeAndFlush(buf).sync();
        if (buf.refCnt() > 0) buf.release();
    }

    public static boolean sendFileInfo(ChannelHandlerContext ctx, Path file) {
        return sendFileInfo(ctx, file.getFileName().toString());
    }

    public static boolean sendFileInfo(ChannelHandlerContext ctx, String filename) {
        try {
            writeFileInfo(ctx, filename);
            return true;
        } catch (InterruptedException e) {
            System.out.println("Sending file list error");
            e.printStackTrace();
            return false;
        }
    }

    private static void writeFileInfo(ChannelHandlerContext ctx, Path file) throws InterruptedException {
        writeFileInfo(ctx, file.getFileName().toString());
    }

    private static void writeFileInfo(ChannelHandlerContext ctx, String filename) throws InterruptedException {
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer();
        byte[] bytes = filename.getBytes();
        buf.writeShort((short) bytes.length);
        buf.writeBytes(bytes);
        ctx.writeAndFlush(buf).sync();
        if (buf.refCnt() > 0) buf.release();
    }

    private static void writeFileData(ChannelHandlerContext ctx, Path file) throws NoSuchAlgorithmException, IOException, InterruptedException {
        MessageDigest md = MessageDigest.getInstance(GlobalSettings.CHECKSUM_PROTOCOL);
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer();
        buf.writeLong(Files.size(file));
        byte[] bytes = new byte[BUFFER_SIZE];
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(file.toFile()));
        int n;
        while ((n = in.read(bytes)) != -1) {
            buf.retain();
            buf.writeBytes(bytes, 0, n);
            md.update(bytes, 0, n);
            ctx.writeAndFlush(buf).sync();
            buf.clear();
        }
        buf.retain();
        buf.writeBytes(md.digest(), 0, GlobalSettings.CHECKSUM_LENGTH);
        ctx.writeAndFlush(buf).sync();
        if (buf.refCnt() > 0) buf.release();
        in.close();
    }
}

