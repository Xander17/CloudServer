package resources;

public enum CommandMessage {
    CLOSE_SERVER("close", "Закрыть соединение"),
    USER_LIST("users","Список пользователей"),
    COMMANDS_LIST("help", "Помощь"),
    AUTH("auth", ""),
    AUTH_OK("authok", ""),
    ERROR("error", ""),
    REG("reg", ""),
    REG_OK("regok", ""),
    FILELIST("flist", "");

    private String message;
    private String description;

    CommandMessage(String message, String description) {
        this.message = message;
        this.description = description;
    }

    public boolean check(String s) {
        return s.equalsIgnoreCase(message);
    }

    public boolean hasDescription() {
        return !description.isEmpty();
    }

    public String getFullDescription() {
        String tabSpaces = "";
        if (message.length() < 5) tabSpaces = "\t\t\t";
        else if (message.length() < 9) tabSpaces = "\t\t";
        else tabSpaces = "\t";
        return message + tabSpaces + description;
    }

    public static boolean isControlMessage(String s) {
        return s.startsWith("/");
    }

    @Override
    public String toString() {
        return message;
    }
}