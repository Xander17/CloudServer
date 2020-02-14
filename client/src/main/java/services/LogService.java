package services;

import org.apache.log4j.Logger;

public class LogService {

    public static final LogService SERVER = new LogService(Logger.getLogger("server"));
    public static final LogService CLIENT = new LogService(Logger.getLogger("users"));

    private Logger logger;

    private LogService(Logger logger) {
        this.logger = logger;
    }

    public void debug(String... message) {
        String msg = String.join(": ", message);
        logger.debug(msg);
    }

    public void error(String... message) {
        String msg = String.join(": ", message);
        logger.error(msg);
    }

    public void error(Throwable e) {
        StackTraceElement[] s = e.getStackTrace();
        for (StackTraceElement element : s) {
            logger.error(element);
        }
    }
    public void fatal(String... message) {
        String msg = String.join(": ", message);
        logger.fatal(msg);
    }

    public void info(String... message) {
        String msg = String.join(": ", message);
        logger.info(msg);
    }

    public void warn(String... message) {
        String msg = String.join(": ", message);
        logger.warn(msg);
    }

    public Logger getLogger() {
        return logger;
    }
}
