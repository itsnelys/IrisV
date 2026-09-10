package net.opal.irisv.option;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.opal.irisv.commun.utils.FunctionUtilsChat;
import net.opal.irisv.commun.utils.FunctionUtilsLogs;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class ConfigReloader {
    private static final Map<String, Consumer<Path>> reloadHandlers = new HashMap<>();

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("irisvreload")
                .requires(source -> source.hasPermission(2))
                .executes(context -> reloadAllConfigs(context.getSource())));

        event.getDispatcher().register(Commands.literal("irisv")
                .then(Commands.literal("reload")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> reloadAllConfigs(context.getSource()))));
    }

    public static void registerReloadHandler(String fileName, Consumer<Path> handler) {
        reloadHandlers.put(fileName, handler);
    }

    public static int reloadAllConfigs(CommandSourceStack source) {
        try {
            Path basePath = FMLPaths.CONFIGDIR.get().resolve("irisv");

            if (!Files.exists(basePath)) {
                source.sendFailure(net.minecraft.network.chat.Component.translatable("reloadfile.error.config.folder", basePath.toString()));
                FunctionUtilsLogs.errorLog("Config", "Config folder not found : " + basePath.toString());
                return 0;
            }

            FunctionUtilsChat.sendActionMessage(source, "reloadfile.action.config.start_reload", basePath.toString());
            FunctionUtilsLogs.actionLog("Config", "Reloaded folder : " + basePath.toString());

            java.util.concurrent.atomic.AtomicBoolean failed = new java.util.concurrent.atomic.AtomicBoolean();
            try (Stream<Path> files = Files.list(basePath)) {
                files.filter(Files::isRegularFile)
                        .forEach(file -> {
                            String fileName = file.getFileName().toString();

                            try {
                                if ("options.json".equals(fileName)) {
                                    ConfigOptions.reloadChecked();
                                    FunctionUtilsChat.sendActionMessage(source, "reloadfile.action.config.reload", fileName);
                                    FunctionUtilsLogs.actionLog("Config", "Reloaded file " + fileName);
                                } else if (reloadHandlers.containsKey(fileName)) {
                                    reloadHandlers.get(fileName).accept(file);
                                    FunctionUtilsChat.sendActionMessage(source, "reloadfile.action.config.reload", fileName);
                                    FunctionUtilsLogs.actionLog("Config", "Reloaded file " + fileName);
                                } else {
                                    FunctionUtilsLogs.debugLog("Config", "Skipped file (no handler): " + fileName);
                                }
                            } catch (Exception e) {
                                failed.set(true);
                                source.sendFailure(net.minecraft.network.chat.Component.translatable("reloadfile.error.config.load", fileName, e.getMessage()));
                                FunctionUtilsLogs.errorLog("Config", "Failed to load: " + fileName, e);
                            }
                        });
            }
            if (failed.get()) {
                source.sendFailure(net.minecraft.network.chat.Component.translatable("irisv.chat.reload_failed"));
                return 0;
            }
            source.sendSuccess(() -> net.minecraft.network.chat.Component.translatable("reloadfile.action.config.done"), false);
            return 1;
        } catch (Exception e) {
            source.sendFailure(net.minecraft.network.chat.Component.translatable("reloadfile.error.config.reload", e.getMessage()));
            FunctionUtilsLogs.errorLog("Config", "Failed to reload configs", e);
            return 0;
        }
    }
}
