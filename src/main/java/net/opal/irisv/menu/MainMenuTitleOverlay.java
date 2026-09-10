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

    private static final Component TAGLINE_TEXT = Component.translatable("menu.irisv.main.tagline");
    private static final Component BRAND_TEXT = Component.literal("IrisV").withStyle(net.minecraft.ChatFormatting.BOLD);

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof TitleScreen titleScreen) {
            GuiGraphics guiGraphics = event.getGuiGraphics();
            Font font = Minecraft.getInstance().font;
            UiTheme theme = UiTheme.getCurrent();

            int x = 4;
            int y = 6;
            int contentWidth = Math.max(font.width(BRAND_TEXT), font.width(TAGLINE_TEXT));
            int panelRight = x + contentWidth + 12;
            int panelBottom = y + font.lineHeight * 2 + 9;

            guiGraphics.fill(x - 4, y - 3, panelRight, panelBottom, theme.gui_bgOverlay());
            guiGraphics.fill(x - 1, y - 2, x, panelBottom - 1, theme.gui_lineColor());
            guiGraphics.drawString(font, BRAND_TEXT, x + 4, y, 0xFFFFFF, true);
            guiGraphics.drawString(font, TAGLINE_TEXT, x + 4, y + font.lineHeight + 3, theme.mod_name_color(), true);
        }
    }
}
