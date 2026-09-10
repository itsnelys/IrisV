package net.opal.irisv.commun.utils;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.opal.irisv.option.ConfigOptions;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class FunctionUtilsChat {
    private static final java.util.Map<String, Long> recentClientMessages = new java.util.LinkedHashMap<>();
    private static java.lang.ref.WeakReference<Object> clientLevel = new java.lang.ref.WeakReference<>(null);

    /** Local UI feedback only. Never sends a packet or broadcasts to other players. */
    public static void clientAction(net.minecraft.world.entity.player.Player player, String key, Object... args) {
        clientMessage(player, PREFIX_ACTION_KEY, key, args);
    }

    public static void clientError(net.minecraft.world.entity.player.Player player, String key, Object... args) {
        clientMessage(player, PREFIX_ERROR_KEY, key, args);
    }

    private static void clientMessage(net.minecraft.world.entity.player.Player player, String prefix, String key, Object... args) {
        if (player == null || !player.level().isClientSide || !ConfigOptions.getInstance().enableDebugChat) return;
        if (clientLevel.get() != player.level()) {
            clientLevel = new java.lang.ref.WeakReference<>(player.level());
            recentClientMessages.clear();
        }
        long now = System.nanoTime();
        String identity = prefix + key + java.util.Arrays.toString(args);
        if (!allowClientMessage(identity, now)) return;
        player.displayClientMessage(Component.literal("[IrisV] ").withStyle(net.minecraft.ChatFormatting.GRAY)
                .append(Component.translatable(prefix)).append(Component.translatable(key, args)), false);
    }

    static boolean allowClientMessage(String identity, long now) {
        Long last = recentClientMessages.get(identity);
        if (last != null && now - last < 1_000_000_000L) return false;
        recentClientMessages.put(identity, now);
        if (recentClientMessages.size() > 64) recentClientMessages.remove(recentClientMessages.keySet().iterator().next());
        return true;
    }
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final String PREFIX_DEBUG_KEY = "debug.chat.irisv.prefix.debug";
    private static final String PREFIX_INFO_KEY = "debug.chat.irisv.prefix.info";
    private static final String PREFIX_ACTION_KEY = "debug.chat.irisv.prefix.action";
    private static final String PREFIX_ERROR_KEY = "debug.chat.irisv.prefix.error";

    public static void sendActionMessage(net.minecraft.commands.CommandSourceStack source, String key, Object... args) {
        if (ConfigOptions.getInstance().enableDebugChat) {
            source.sendSuccess(() -> Component.literal("[IrisV] ").append(Component.translatable(PREFIX_ACTION_KEY))
                    .append(Component.translatable(key, args)), false);
        }
    }

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
