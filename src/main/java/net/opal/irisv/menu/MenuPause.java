package net.opal.irisv.menu;

import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.Optional;

public class MenuPause {

    @SubscribeEvent
    public static void onInitScreen(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();

        if (screen instanceof PauseScreen) {
            String feedbackText = Component.translatable("menu.sendFeedback").getString();
            String reportBugText = Component.translatable("menu.reportBugs").getString();

            Optional<AbstractWidget> feedbackButtonOpt = screen.renderables.stream()
                    .filter(w -> w instanceof AbstractWidget)
                    .map(w -> (AbstractWidget) w)
                    .filter(w -> w.getMessage().getString().equals(feedbackText))
                    .findFirst();

            feedbackButtonOpt.ifPresent(oldButton -> {
                event.removeListener(oldButton);
                Button irisvOptionsButton = Button.builder(
                                Component.translatable("menu.irisv.options"),
                                b -> screen.getMinecraft().setScreen(new MenuOptionIrisv(screen)))
                        .pos(oldButton.getX(), oldButton.getY())
                        .size(oldButton.getWidth(), oldButton.getHeight())
                        .build();
                event.addListener(irisvOptionsButton);
            });

            Optional<AbstractWidget> reportBugButtonOpt = screen.renderables.stream()
                    .filter(w -> w instanceof AbstractWidget)
                    .map(w -> (AbstractWidget) w)
                    .filter(w -> w.getMessage().getString().equals(reportBugText))
                    .findFirst();

            reportBugButtonOpt.ifPresent(oldButton -> {
                event.removeListener(oldButton);
                Button emptyButton = Button.builder(
                                Component.literal(""),
                                b -> {
                                })
                        .pos(oldButton.getX(), oldButton.getY())
                        .size(oldButton.getWidth(), oldButton.getHeight())
                        .build();
                event.addListener(emptyButton);
            });
        }
    }
}
