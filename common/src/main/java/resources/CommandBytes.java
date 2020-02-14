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

    public boolean check(byte b) {
        return b == byteNum;
    }

    public byte getByte() {
        return byteNum;
    }

    @Override
    public String toString() {
        return byteString;
    }
}
