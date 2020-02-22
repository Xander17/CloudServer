package resources;

import services.settings.AppOption;
import services.settings.AppSettings;

public enum ClientSettings implements AppSettings {
    ROOT_DIRECTORY(new AppOption("root_directory", "client-repo", false)),
    DATA_BUFFER_MIN_SIZE(new AppOption("inbound_buffer_min_size", String.valueOf(100 * 1024), true)),
    DATA_BUFFER_MAX_SIZE(new AppOption("inbound_buffer_max_size", String.valueOf(1024 * 1024 * 2), true)),
    STYLE(new AppOption("style", "css/client.css", false));

    private AppOption option;

    ClientSettings(AppOption option) {
        this.option = option;
    }

    public AppOption getOption() {
        return option;
    }

    public static AppOption[] getSettings() {
        return AppSettings.getSettings(ClientSettings.values());
    }
}


