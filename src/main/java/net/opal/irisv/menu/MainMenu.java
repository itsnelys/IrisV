package net.opal.irisv.menu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.opal.irisv.Irisv;
import net.opal.irisv.recip.RecipeWikiScreen;

import java.util.Optional;

public class MainMenu {
    private static final int BUTTON_HEIGHT = 20;
    private static final int DEFAULT_BUTTON_WIDTH = 200;
    private static final int ROW_GAP = 4;
    private static final int WIKI_BUTTON_WIDTH = 20;
    private static final ResourceLocation WIKI_ICON = ResourceLocation.fromNamespaceAndPath(Irisv.MODID, "textures/gui/wiki_icon.png");

    @SubscribeEvent
    public static void onInitScreen(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();

        if (screen instanceof TitleScreen) {
            Anchor modsAnchor = findModsAnchor(screen)
                    .orElseGet(() -> new Anchor((screen.width - DEFAULT_BUTTON_WIDTH) / 2, screen.height / 4 + 96, DEFAULT_BUTTON_WIDTH));
            moveButtonsBelowMods(screen, modsAnchor);
            int irisvY = modsAnchor.y() + BUTTON_HEIGHT + ROW_GAP;
            int optionsWidth = Math.max(80, modsAnchor.width() - WIKI_BUTTON_WIDTH - ROW_GAP);

            Button irisvOptionsButton = Button.builder(
                            Component.translatable("menu.irisv.options"),
                            b -> screen.getMinecraft().setScreen(new MenuOptionIrisv(screen)))
                    .pos(modsAnchor.x(), irisvY)
                    .size(optionsWidth, BUTTON_HEIGHT)
                    .build();
            event.addListener(irisvOptionsButton);

            event.addListener(new WikiButton(
                    modsAnchor.x() + optionsWidth + ROW_GAP,
                    irisvY,
                    b -> screen.getMinecraft().setScreen(new RecipeWikiScreen(screen))
            ));
        }
    }

    private static Optional<Anchor> findModsAnchor(Screen screen) {
        return screen.children().stream()
                .filter(AbstractWidget.class::isInstance)
                .map(AbstractWidget.class::cast)
                .filter(widget -> widget instanceof Button)
                .filter(widget -> "Mods".equals(widget.getMessage().getString()))
                .findFirst()
                .map(widget -> new Anchor(widget.getX(), widget.getY(), widget.getWidth()));
    }

    private static void moveButtonsBelowMods(Screen screen, Anchor modsAnchor) {
        screen.children().stream()
                .filter(AbstractWidget.class::isInstance)
                .map(AbstractWidget.class::cast)
                .filter(widget -> widget instanceof Button)
                .filter(widget -> widget.getY() > modsAnchor.y())
                .forEach(widget -> widget.setY(widget.getY() + BUTTON_HEIGHT + ROW_GAP));
    }

    private record Anchor(int x, int y, int width) {}

    private static class WikiButton extends Button {
        private WikiButton(int x, int y, OnPress onPress) {
            super(x, y, WIKI_BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable(""), onPress, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
            guiGraphics.blit(WIKI_ICON, getX() + 2, getY() + 2, 0.0F, 0.0F, 16, 16, 16, 16);
        }
    }
}
