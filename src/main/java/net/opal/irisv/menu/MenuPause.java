package net.opal.irisv.menu;

import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

public class MenuPause {

    @SubscribeEvent
    public static void onInitScreen(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();

        if (screen instanceof PauseScreen) {
            Button irisvOptionsButton = Button.builder(
                            Component.translatable("menu.irisv.options"),
                            b -> screen.getMinecraft().setScreen(new MenuOptionIrisv(screen)))
                    .pos(screen.width - 104, screen.height - 24)
                    .size(100, 20)
                    .build();
            event.addListener(irisvOptionsButton);
        }
    }
}
