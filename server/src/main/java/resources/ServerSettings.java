package resources;

import services.settings.AppOption;
import services.settings.AppSettings;

public enum ServerSettings implements AppSettings {
    ROOT_DIRECTORY(new AppOption("root_directory", "server-repo", false)),
    INBOUND_BUFFER_MIN_SIZE(new AppOption("inbound_buffer_min_size", String.valueOf(100 * 1024), true)),
    INBOUND_BUFFER_MAX_SIZE(new AppOption("inbound_buffer_max_size", String.valueOf(1024 * 1024 * 2), true)),
    DATABASE_HOST(new AppOption("database_host", "localhost", false)),
    DATABASE_PORT(new AppOption("database_port", "3306", true)),
    DATABASE_NAME(new AppOption("database_name", "cloud_server", false)),
    DATABASE_SETTINGS_STRING(new AppOption("database_settings_string", "createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&useSSL=false&useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC", false)),
    DATABASE_USERNAME(new AppOption("database_login", "user", false)),
    DATABASE_PASS(new AppOption("database_pass", "pass", false));

    private AppOption option;

    ServerSettings(AppOption option) {
        this.option = option;
    }

    public AppOption getOption() {
        return option;
    }

    public static AppOption[] getSettings() {
        return AppSettings.getSettings(ServerSettings.values());
    }
}


