package ru.kornev.cloudserver.resources;

import ru.kornev.cloudcommon.services.settings.AppOption;
import ru.kornev.cloudcommon.services.settings.AppSettings;

public enum ServerSettings implements AppSettings {
    ROOT_DIRECTORY("root_directory", "server-repo"),
    INBOUND_BUFFER_MIN_SIZE("inbound_buffer_min_size", 100 * 1024),
    INBOUND_BUFFER_MAX_SIZE("inbound_buffer_max_size", 1024 * 1024 * 2),
    DATABASE_HOST("database_host", "localhost"),
    DATABASE_PORT("database_port", 3306),
    DATABASE_NAME("database_name", "cloud_server"),
    DATABASE_SETTINGS_STRING("database_settings_string", "createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&useSSL=false&useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC"),
    DATABASE_USERNAME("database_login", "user"),
    DATABASE_PASS("database_pass", "pass"),
    SERVER_PORT("server_port",8189);
    
    private AppOption option;

    ServerSettings(String name, String defaultValue) {
        this.option = new AppOption(name, defaultValue, false);
    }

    ServerSettings(String name, int defaultValue) {
        this.option = new AppOption(name, String.valueOf(defaultValue), true);
    }

    public static AppOption[] getSettings() {
        return AppSettings.getSettings(ServerSettings.values());
    }

    public AppOption getOption() {
        return option;
    }
}


