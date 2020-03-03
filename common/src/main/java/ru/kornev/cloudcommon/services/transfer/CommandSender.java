package ru.kornev.cloudcommon.services.transfer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import ru.kornev.cloudcommon.resources.CommandBytes;
import ru.kornev.cloudcommon.settings.GlobalSettings;

import java.util.Arrays;

public class CommandSender {

    private static PooledByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;

    public static void sendData(ChannelHandlerContext ctx, byte[]... bytes) {
        ByteBuf buf = allocator.directBuffer();
        for (int i = 0; i < bytes.length; i++) {
            buf.writeBytes(bytes[i]);
        }
        ctx.writeAndFlush(buf);
    }

    public static void sendCommand(ChannelHandlerContext ctx, CommandBytes b) {
        sendCommand(ctx, b, 0);
    }

    public static void sendCommand(ChannelHandlerContext ctx, CommandBytes b, byte... data) {
        ByteBuf buf = getCommandByteBuf(b);
        if (data.length != GlobalSettings.COMMAND_DATA_LENGTH)
            data = Arrays.copyOf(data, GlobalSettings.COMMAND_DATA_LENGTH);
        buf.writeBytes(data);
//        System.out.println("out bytes: " + Arrays.toString(data));
        ctx.writeAndFlush(buf);
    }

    public static void sendCommand(ChannelHandlerContext ctx, CommandBytes b, int data) {
        ByteBuf buf = getCommandByteBuf(b);
        buf.writeInt(data);
//        System.out.println("out int: " + data);
        ctx.writeAndFlush(buf);
    }

    private static ByteBuf getCommandByteBuf(CommandBytes b) {
        ByteBuf buf = allocator.directBuffer(GlobalSettings.COMMAND_DATA_LENGTH + 2);
        buf.writeByte(CommandBytes.COMMAND_START.getByte());
        buf.writeByte(b.getByte());
//        System.out.println("out: " + CommandBytes.COMMAND_START.name() + " " + CommandBytes.getCommand(b.getByte()));
        return buf;
    }

}
