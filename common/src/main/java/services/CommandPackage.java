package services;

import io.netty.buffer.ByteBuf;
import resources.CommandBytes;
import settings.GlobalSettings;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class CommandPackage {
    byte[] bytes;
    private byte command;

    public CommandPackage(ByteBuf byteBuf) {
        command = byteBuf.readByte();
        bytes = new byte[GlobalSettings.COMMAND_DATA_LENGTH];
        byteBuf.readBytes(bytes);
        // TODO: 16.02.2020 сделано для логирования, возможно нужно будет удалить
        System.out.println("in: " + CommandBytes.getCommand(command) + ": " + Arrays.toString(bytes));
    }

    public byte getByte(int i) {
        if (i < 0 || i > GlobalSettings.COMMAND_DATA_LENGTH) throw new IllegalArgumentException();
        return bytes[i];
    }

    public int getInt() {
        return ByteBuffer.wrap(bytes).getInt();
    }

    public byte getCommand() {
        return command;
    }
}
