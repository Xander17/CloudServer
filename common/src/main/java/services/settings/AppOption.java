package services.settings;

import services.LogServiceCommon;

public class AppOption {
    private String name;
    private String defaultOption;
    private boolean isInteger;

    public AppOption(String name, String defaultOption, boolean isInteger) {
        checkFormat(defaultOption, isInteger);
        this.name = name;
        this.defaultOption = defaultOption;
        this.isInteger = isInteger;
    }

    public String getDefaultOption() {
        return defaultOption;
    }

    public boolean isInteger() {
        return isInteger;
    }

    public String getName() {
        return name;
    }

    private void checkFormat(String defaultValue, boolean isInteger) {
        if (isInteger && !checkIntegerFormat(defaultValue)) {
            String s = "Option" + toString() + "default value is broken";
            LogServiceCommon.APP.fatal(s);
            throw new RuntimeException(s);
        }
    }

    private static boolean checkIntegerFormat(String value) {
        return value.matches("\\d+");
    }

    @Override
    public String toString() {
        return name;
    }
}
