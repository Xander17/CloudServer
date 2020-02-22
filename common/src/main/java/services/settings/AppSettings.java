package services.settings;

public interface AppSettings {
    AppOption getOption();

    static AppOption[] getSettings(AppSettings[] settings) {
        AppOption[] appOptions = new AppOption[settings.length];
        for (int i = 0; i < settings.length; i++) {
            appOptions[i] = settings[i].getOption();
        }
        return appOptions;
    }
}
