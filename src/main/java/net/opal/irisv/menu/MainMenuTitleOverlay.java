package net.opal.irisv.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.Font;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.opal.irisv.theme.UiTheme;

public class MainMenuTitleOverlay {

    private static final Component CUSTOM_TEXT = Component.translatable("menu.irisv.version");
    private static final Component TAGLINE_TEXT = Component.translatable("menu.irisv.main.tagline");

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof TitleScreen titleScreen) {
            GuiGraphics guiGraphics = event.getGuiGraphics();
            Font font = Minecraft.getInstance().font;
            UiTheme theme = UiTheme.getCurrent();

            int x = 4;
            int y = 8;
            int textWidth = Math.max(font.width(CUSTOM_TEXT), font.width(TAGLINE_TEXT));

            guiGraphics.fill(x - 4, y - 5, x + textWidth + 12, y + 20, theme.gui_bgOverlay());
            guiGraphics.fill(x - 1, y - 4, x, y + 19, theme.gui_lineColor());
            guiGraphics.drawString(font, CUSTOM_TEXT, x + 4, y, 0xFFFFFF, true);
            guiGraphics.drawString(font, TAGLINE_TEXT, x + 4, y + 10, theme.mod_name_color(), true);
        }
    }
}
