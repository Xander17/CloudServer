package ru.kornev.cloudclient.resources;

import ru.kornev.cloudcommon.services.settings.AppOption;
import ru.kornev.cloudcommon.services.settings.AppSettings;

public enum ClientSettings implements AppSettings {
    CONNECTION_HOST("connection_host","localhost"),
    CONNECTION_PORT("connection_port",8189),
    ROOT_DIRECTORY("root_directory", "client-repo"),
    DATA_BUFFER_MIN_SIZE("inbound_buffer_min_size", 100 * 1024),
    DATA_BUFFER_MAX_SIZE("inbound_buffer_max_size", 1024 * 1024 * 2),
    STYLE("style", "css/client.css");

    private AppOption option;

    ClientSettings(String name, String defaultValue) {
        this.option = new AppOption(name, defaultValue, false);
    }

    ClientSettings(String name, int defaultValue) {
        this.option = new AppOption(name, String.valueOf(defaultValue), true);
    }

    public static AppOption[] getSettings() {
        return AppSettings.getSettings(ClientSettings.values());
    }

    public AppOption getOption() {
        return option;
    }
}