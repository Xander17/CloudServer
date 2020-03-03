package ru.kornev.cloudclient.app.settings;

import ru.kornev.cloudcommon.services.settings.AppOption;
import ru.kornev.cloudcommon.services.settings.AppSettings;

public enum ClientSettings implements AppSettings {
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