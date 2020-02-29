package ru.kornev.cloudnetwork.resources;

import ru.kornev.cloudcommon.services.settings.AppOption;
import ru.kornev.cloudcommon.services.settings.AppSettings;

public enum NetworkSettings implements AppSettings {
    CONNECTION_HOST("connection_host", "localhost"),
    CONNECTION_PORT("connection_port", 8189),
    ROOT_DIRECTORY("root_directory", "client-repo"),
    TEMP_DIRECTORY("temp_directory", "temp"),
    DATA_BUFFER_MIN_SIZE("inbound_buffer_min_size", 100 * 1024),
    DATA_BUFFER_MAX_SIZE("inbound_buffer_max_size", 1024 * 1024 * 2),
    STYLE("style", "css/client.css"),
    DOWNLOAD_BUFFER_SIZE("download_buffer_size", 8192),
    UPLOAD_BUFFER_SIZE("upload_buffer_size", 8192);

    private AppOption option;

    NetworkSettings(String name, String defaultValue) {
        this.option = new AppOption(name, defaultValue, false);
    }

    NetworkSettings(String name, int defaultValue) {
        this.option = new AppOption(name, String.valueOf(defaultValue), true);
    }

    public static AppOption[] getSettings() {
        return AppSettings.getSettings(NetworkSettings.values());
    }

    public AppOption getOption() {
        return option;
    }
}