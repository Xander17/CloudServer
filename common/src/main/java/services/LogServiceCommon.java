package services;

import org.apache.log4j.Logger;

public class LogServiceCommon {

    public static final LogServiceCommon APP = new LogServiceCommon(Logger.getLogger("app"));
    public static final LogServiceCommon TRANSFER = new LogServiceCommon(Logger.getLogger("transfer"));
    private static final LogServiceCommon CONSOLE = new LogServiceCommon(Logger.getLogger("console"));

    private Logger logger;
    
    private static boolean appendConsole;

    private LogServiceCommon(Logger logger) {
        this.logger = logger;
    }

    public static void setAppendConsole(boolean appendConsole) {
        LogServiceCommon.appendConsole = appendConsole;
    }

    public void debug(String... message) {
        String msg = String.join(": ", message);
        logger.debug(msg);
        if (appendConsole) CONSOLE.getLogger().debug(msg);
    }

    public void error(String... message) {
        String msg = String.join(": ", message);
        logger.error(msg);
        if (appendConsole) CONSOLE.getLogger().error(msg);
    }

    public void error(Throwable e) {
        StackTraceElement[] s = e.getStackTrace();
        for (StackTraceElement element : s) {
            logger.error(element);
            if (appendConsole) CONSOLE.getLogger().error(element);
        }
    }

    public void fatal(String... message) {
        String msg = String.join(": ", message);
        logger.fatal(msg);
        if (appendConsole) CONSOLE.getLogger().fatal(msg);
    }

    public void info(String... message) {
        String msg = String.join(": ", message);
        logger.info(msg);
        if (appendConsole) CONSOLE.getLogger().info(msg);
    }

    public void warn(String... message) {
        String msg = String.join(": ", message);
        logger.warn(msg);
        if (appendConsole) CONSOLE.getLogger().warn(msg);
    }

    public Logger getLogger() {
        return logger;
    }
}
