package resources;

public enum CommandBytes {
    PACKAGE_START((byte) 15),
    COMMAND_START((byte) 16),
    AUTH((byte) 1),
    AUTH_OK((byte) 2),
    ERROR((byte) 3),
    REG((byte) 4),
    REG_OK((byte) 5),
    FILELIST((byte) 6);

    private byte byteNum;
    private String byteString;

    CommandBytes(byte byteNum) {
        this.byteNum = byteNum;
        this.byteString = new String(new byte[]{byteNum});
    }

    // TODO: 16.02.2020 сделано для логирования, возможно нужно будет удалить
    public static CommandBytes getCommand(byte b) {
        CommandBytes[] values = CommandBytes.values();
        for (int i = 0; i < values.length; i++) {
            if (values[i].getByte() == b) return values[i];
        }
        return null;
    }

    public boolean check(byte b) {
        return b == byteNum;
    }

    public byte getByte() {
        return byteNum;
    }

    @Override
    public String toString() {
        return name();
    }
}
