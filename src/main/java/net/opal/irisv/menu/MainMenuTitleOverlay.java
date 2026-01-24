package net.opal.irisv.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.Font;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

public class MainMenuTitleOverlay {

    private static final Component CUSTOM_TEXT = Component.translatable("menu.irisv.version");

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof TitleScreen titleScreen) {
            GuiGraphics guiGraphics = event.getGuiGraphics();
            Font font = Minecraft.getInstance().font;

            int x = 2;
            int y = titleScreen.height - 30;

            guiGraphics.drawString(font, CUSTOM_TEXT, x, y, 0xFFFFFF, false);
        }
    }
}