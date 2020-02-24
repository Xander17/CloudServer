package ru.kornev.cloudcommon.services.transfer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import ru.kornev.cloudcommon.resources.CommandBytes;
import ru.kornev.cloudcommon.settings.GlobalSettings;

import java.util.Arrays;

public class DataSocketWriter {

    public static void sendData(ChannelHandlerContext ctx, byte[]... bytes) {
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer();
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
        // TODO: 16.02.2020 сделано для логирования, возможно нужно будет удалить
        System.out.println("out bytes: " + Arrays.toString(data));
        ctx.writeAndFlush(buf);
    }

    public static void sendCommand(ChannelHandlerContext ctx, CommandBytes b, int data) {
        ByteBuf buf = getCommandByteBuf(b);
        buf.writeInt(data);
        // TODO: 16.02.2020 сделано для логирования, возможно нужно будет удалить
        System.out.println("out int: " + data);
        ctx.writeAndFlush(buf);
    }

    private static ByteBuf getCommandByteBuf(CommandBytes b) {
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(GlobalSettings.COMMAND_DATA_LENGTH + 2);
        buf.writeByte(CommandBytes.COMMAND_START.getByte());
        buf.writeByte(b.getByte());
        // TODO: 16.02.2020 сделано для логирования, возможно нужно будет удалить
        System.out.println("out: " + CommandBytes.COMMAND_START.name() + " " + CommandBytes.getCommand(b.getByte()));
        return buf;
    }

}
