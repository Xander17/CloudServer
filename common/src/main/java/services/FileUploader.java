package services;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;

import java.nio.file.Path;

public class FileUploader {

    public static void writeFileInfo(ChannelHandlerContext ctx, Path file) {
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer();
        byte[] bytes = file.getFileName().toString().getBytes();
        buf.writeShort((short) bytes.length);
        buf.writeBytes(bytes);
        ctx.write(buf);
        buf.release();
    }
}
