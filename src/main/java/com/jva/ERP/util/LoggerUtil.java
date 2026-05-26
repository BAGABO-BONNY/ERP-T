package com.jva.ERP.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logging Utility
 * Provides common logging methods
 */
public class LoggerUtil {

    private static final Logger logger = LoggerFactory.getLogger(LoggerUtil.class);

    private LoggerUtil() {
        // Private constructor to prevent instantiation
    }

    public static void logInfo(String message) {
        logger.info(message);
    }

    public static void logError(String message, Exception ex) {
        logger.error(message, ex);
    }

    public static void logDebug(String message) {
        logger.debug(message);
    }

    public static void logWarn(String message) {
        logger.warn(message);
    }
}

