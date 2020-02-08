package services;

import org.apache.log4j.Logger;

public class LogService {

    public static final LogService SERVER = new LogService(Logger.getLogger("server"));
    public static final LogService USERS = new LogService(Logger.getLogger("users"));
    public static final LogService AUTH = new LogService(Logger.getLogger("auth"));
    public static final LogService CONSOLE = new LogService(Logger.getLogger("console"));

    private Logger logger;

    private LogService(Logger logger) {
        this.logger = logger;
    }

    public void debug(String... message) {
        String msg = String.join(": ", message);
        logger.debug(msg);
        if (this != CONSOLE) CONSOLE.getLogger().debug(msg);
    }

    public void error(String... message) {
        String msg = String.join(": ", message);
        logger.error(msg);
        if (this != CONSOLE) CONSOLE.getLogger().error(msg);
    }

    public void fatal(String... message) {
        String msg = String.join(": ", message);
        logger.fatal(msg);
        if (this != CONSOLE) CONSOLE.getLogger().fatal(msg);
    }

    public void info(String... message) {
        String msg = String.join(": ", message);
        logger.info(msg);
        if (this != CONSOLE) CONSOLE.getLogger().info(msg);
    }

    public void warn(String... message) {
        String msg = String.join(": ", message);
        logger.warn(msg);
        if (this != CONSOLE) CONSOLE.getLogger().warn(msg);
    }

    public Logger getLogger() {
        return logger;
    }
}
