package net.opal.irisv.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.Font;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.opal.irisv.Irisv;
import net.opal.irisv.theme.UiTheme;

public class MainMenuTitleOverlay {

    private static final Component TAGLINE_TEXT = Component.translatable("menu.irisv.main.tagline");
    private static final ResourceLocation WORDMARK = ResourceLocation.fromNamespaceAndPath(Irisv.MODID, "textures/gui/irisv_wordmark.png");
    private static final int WORDMARK_WIDTH = 86;
    private static final int WORDMARK_HEIGHT = 29;

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof TitleScreen titleScreen) {
            GuiGraphics guiGraphics = event.getGuiGraphics();
            Font font = Minecraft.getInstance().font;
            UiTheme theme = UiTheme.getCurrent();

            int x = 4;
            int y = 6;
            int contentWidth = Math.max(WORDMARK_WIDTH, font.width(TAGLINE_TEXT));
            int panelRight = x + contentWidth + 12;
            int panelBottom = y + WORDMARK_HEIGHT + 17;
            int taglineX = x + 4 + (WORDMARK_WIDTH - font.width(TAGLINE_TEXT)) / 2;

            guiGraphics.fill(x - 4, y - 3, panelRight, panelBottom, theme.gui_bgOverlay());
            guiGraphics.fill(x - 1, y - 2, x, panelBottom - 1, theme.gui_lineColor());
            guiGraphics.blit(WORDMARK, x + 4, y, 0, 0.0F, 0.0F, WORDMARK_WIDTH, WORDMARK_HEIGHT, WORDMARK_WIDTH, WORDMARK_HEIGHT);
            guiGraphics.drawString(font, TAGLINE_TEXT, Math.max(x + 4, taglineX), y + WORDMARK_HEIGHT + 3, theme.mod_name_color(), true);
        }
    }
}
