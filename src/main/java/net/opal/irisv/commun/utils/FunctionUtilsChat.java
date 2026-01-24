package net.opal.irisv.commun.utils;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.opal.irisv.option.ConfigOptions;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class FunctionUtilsChat {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final String PREFIX_DEBUG_KEY = "debug.chat.irisv.prefix.debug";
    private static final String PREFIX_INFO_KEY = "debug.chat.irisv.prefix.info";
    private static final String PREFIX_ACTION_KEY = "debug.chat.irisv.prefix.action";
    private static final String PREFIX_ERROR_KEY = "debug.chat.irisv.prefix.error";

    private static void sendMessage(ServerLevel level, String prefixKey, String messageKey, Object... args) {
        if (!level.isClientSide && ConfigOptions.getInstance().enableDebugChat) {
            String time = LocalTime.now().format(formatter);
            Component timeComponent = Component.literal("[" + time + "] ");
            Component prefixComponent = Component.translatable(prefixKey);
            Component messageComponent;
            if (args == null || args.length == 0) {
                messageComponent = Component.translatable(messageKey);
            } else {
                messageComponent = Component.translatable(messageKey, args);
            }

            Component fullMessage = Component.empty()
                    .append(timeComponent)
                    .append(prefixComponent)
                    .append(messageComponent);

            for (ServerPlayer player : level.players()) {
                player.sendSystemMessage(fullMessage);
            }
        }
    }

    public static void sendDebugMessage(ServerLevel level, String messageKey, Object... args) {
        sendMessage(level, PREFIX_DEBUG_KEY, messageKey, args);
    }

    public static void sendInfoMessage(ServerLevel level, String messageKey, Object... args) {
        sendMessage(level, PREFIX_INFO_KEY, messageKey, args);
    }

    public static void sendActionMessage(ServerLevel level, String messageKey, Object... args) {
        sendMessage(level, PREFIX_ACTION_KEY, messageKey, args);
    }

    public static void sendErrorMessage(ServerLevel level, String messageKey, Object... args) {
        sendMessage(level, PREFIX_ERROR_KEY, messageKey, args);
    }
}
