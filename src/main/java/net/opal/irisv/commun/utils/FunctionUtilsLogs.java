package net.opal.irisv.commun.utils;

import net.opal.irisv.Irisv;

public class FunctionUtilsLogs {

    public static void debugLog(String context, String message) {
        Irisv.LOGGER.debug("[IrisV/{}] {}", context, message);
    }
    public static void infoLog(String context, String message) {
        Irisv.LOGGER.info("[IrisV/{}] {}", context, message);
    }
    public static void actionLog(String context, String message) {
        Irisv.LOGGER.info("[IrisV/{}] {}", context, message);
    }
    public static void errorLog(String context, String message) {
        Irisv.LOGGER.error("[IrisV/{}] {}", context, message);
    }
    public static void errorLog(String context, String message, Throwable cause) {
        Irisv.LOGGER.error("[IrisV/" + context + "] " + message, cause);
    }
}
