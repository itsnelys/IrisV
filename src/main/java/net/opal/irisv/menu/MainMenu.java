package net.opal.irisv.menu;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

public class MainMenu {
    @SubscribeEvent
    public static void onInitScreen(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();

        if (screen instanceof TitleScreen) {
            String realmsText = Component.translatable("menu.online").getString();

            screen.renderables.stream()
                    .filter(w -> w instanceof AbstractWidget)
                    .map(w -> (AbstractWidget) w)
                    .filter(w -> w.getMessage().getString().equals(realmsText))
                    .findFirst()
                    .ifPresent(oldButton -> {
                        event.removeListener(oldButton);

                        Button irisvOptionsButton = Button.builder(
                                        Component.translatable("menu.irisv.options"),
                                        b -> screen.getMinecraft().setScreen(new MenuOptionIrisv(screen)))
                                .pos(oldButton.getX(), oldButton.getY())
                                .size(oldButton.getWidth(), oldButton.getHeight())
                                .build();

                        event.addListener(irisvOptionsButton);
                    });
        }
    }
}