package net.opal.irisv.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.opal.irisv.option.ConfigOptions;
import org.lwjgl.glfw.GLFW;

public final class ClientKeyBindings {
    private static final String CATEGORY = "key.categories.irisv";
    private static final KeyMapping TOGGLE_OVERLAY = new KeyMapping(
            "key.irisv.toggle_overlay",
            GLFW.GLFW_KEY_V,
            CATEGORY
    );

    private ClientKeyBindings() {}

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_OVERLAY);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        while (TOGGLE_OVERLAY.consumeClick()) {
            ConfigOptions config = ConfigOptions.getInstance();
            config.enableBlockTooltipOverlay = !config.enableBlockTooltipOverlay;
            config.save();

            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.displayClientMessage(
                        Component.translatable(
                                "key.irisv.toggle_overlay.message",
                                Component.translatable(config.enableBlockTooltipOverlay ? "menu.irisv.on" : "menu.irisv.off")
                        ),
                        true
                );
            }
        }
    }
}
