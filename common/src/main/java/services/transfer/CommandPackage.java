package services.transfer;

import io.netty.buffer.ByteBuf;
import resources.CommandBytes;
import services.LogServiceCommon;
import settings.GlobalSettings;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class CommandPackage {
    byte[] bytes;
    private byte command;
    private ByteBuf byteBuf;

    public CommandPackage(ByteBuf byteBuf) {
        this.byteBuf = byteBuf;
    }

    public void load() {
        command = byteBuf.readByte();
        bytes = new byte[GlobalSettings.COMMAND_DATA_LENGTH];
        byteBuf.readBytes(bytes);
        // TODO: 16.02.2020 сделано для логирования, возможно нужно будет удалить
        System.out.println("in: " + CommandBytes.getCommand(command) + ": " + Arrays.toString(bytes));
    }

    public byte getByte(int i) {
        if (i < 0 || i > GlobalSettings.COMMAND_DATA_LENGTH) {
            LogServiceCommon.APP.error("Illegal argument in getByte of CommandPackage - " + i);
            throw new IllegalArgumentException();
        }
        return bytes[i];
    }

    public int getInt() {
        return ByteBuffer.wrap(bytes).getInt();
    }

    public byte getCommand() {
        return command;
    }
}
