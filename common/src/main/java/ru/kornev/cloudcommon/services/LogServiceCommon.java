package ru.kornev.cloudcommon.services;

import org.apache.log4j.Logger;

public class LogServiceCommon {

    public static final LogServiceCommon APP = new LogServiceCommon(Logger.getLogger("app"));
    public static final LogServiceCommon TRANSFER = new LogServiceCommon(Logger.getLogger("transfer"));
    private Logger logger;

    private LogServiceCommon(Logger logger) {
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
}
