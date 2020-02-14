package services;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import resources.CommandBytes;
import settings.GlobalSettings;

import java.util.Arrays;

public class DataSocketWriter {

    public static void sendData(ChannelHandlerContext ctx, byte[]... bytes) {
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer();
        for (int i = 0; i < bytes.length; i++) {
            buf.writeBytes(bytes[i]);
        }
        try {
            ctx.writeAndFlush(buf).sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (buf.refCnt() > 0) buf.release();
    }

    public static void sendCommand(ChannelHandlerContext ctx, CommandBytes b) {
        sendCommand(ctx, b, 0);
    }

    public static void sendCommand(ChannelHandlerContext ctx, CommandBytes b, byte... data) {
        ByteBuf buf = getCommandByteBuf(b);
        if (data.length != GlobalSettings.COMMAND_DATA_LENGTH)
            data = Arrays.copyOf(data, GlobalSettings.COMMAND_DATA_LENGTH);
        buf.writeBytes(data);
        try {
            ctx.writeAndFlush(buf).sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (buf.refCnt() > 0) buf.release();
    }

    public static void sendCommand(ChannelHandlerContext ctx, CommandBytes b, int data) {
        ByteBuf buf = getCommandByteBuf(b);
        buf.writeInt(data);
        try {
            ctx.writeAndFlush(buf).sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (buf.refCnt() > 0) buf.release();
    }

    private static ByteBuf getCommandByteBuf(CommandBytes b) {
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(GlobalSettings.COMMAND_DATA_LENGTH + 2);
        buf.writeByte(CommandBytes.COMMAND_START.getByte());
        buf.writeByte(b.getByte());
        return buf;
    }

}
