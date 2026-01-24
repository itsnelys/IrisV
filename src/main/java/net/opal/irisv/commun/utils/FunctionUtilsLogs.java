package net.opal.irisv.commun.utils;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class FunctionUtilsLogs {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static void debugLog(String context, String message) {
        System.out.println("[" + LocalTime.now().format(formatter) + "] [IrisV /DEBUG] [IrisV/" + context + "]: " + message);
    }
    public static void infoLog(String context, String message) {
        System.out.println("[" + LocalTime.now().format(formatter) + "] [IrisV /INFO] [IrisV/" + context + "]: " + message);
    }
    public static void actionLog(String context, String message) {
        System.out.println("[" + LocalTime.now().format(formatter) + "] [IrisV /ACTION] [IrisV/" + context + "]: " + message);
    }
    public static void errorLog(String context, String message) {
        System.out.println("[" + LocalTime.now().format(formatter) + "] [IrisV /ERROR] [IrisV/" + context + "]: " + message);
    }
}
