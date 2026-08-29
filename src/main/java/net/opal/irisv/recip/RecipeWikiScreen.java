package net.opal.irisv.recip;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Component;
import net.opal.irisv.theme.UiTheme;

import java.util.List;

public class RecipeWikiScreen extends Screen {
    private static final int BUTTON_HEIGHT = 20;
    private static final int BASE_WIDTH = 310;
    private static final int CONTENT_TOP = 50;
    private static final int CONTENT_BOTTOM_PADDING = 52;
    private static final int SECTION_GAP = 10;
    private static final int LINE_GAP = 3;

    private final Screen parent;

    public RecipeWikiScreen(Screen parent) {
        super(Component.translatable("recip.irisv.wiki.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int buttonWidth = returnButtonWidth();
        this.addRenderableWidget(Button.builder(Component.translatable("menu.irisv.return"), button -> {
            if (this.minecraft != null) this.minecraft.setScreen(parent);
        }).pos((this.width - buttonWidth) / 2, this.height - 30).size(buttonWidth, BUTTON_HEIGHT).build());
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        UiTheme theme = UiTheme.getCurrent();
        gui.fill(0, 0, this.width, this.height, theme.gui_bgOverlay());
        super.render(gui, mouseX, mouseY, partialTick);

        renderHeader(gui, theme);
        renderFooter(gui, theme);
        renderContent(gui, theme);
    }

    private void renderContent(GuiGraphics gui, UiTheme theme) {
        int panelWidth = contentWidth();
        int panelX = (this.width - panelWidth) / 2;
        int y = CONTENT_TOP;
        int listBottom = this.height - CONTENT_BOTTOM_PADDING;

        y = drawSection(gui, theme, "recip.irisv.wiki.catalog", panelX, y, panelWidth, listBottom);
        y = drawLines(gui, panelX, y, panelWidth, listBottom, List.of(
                "recip.irisv.wiki.left_click",
                "recip.irisv.wiki.shift_click",
                "recip.irisv.wiki.middle_click",
                "recip.irisv.wiki.scroll"
        ));

        y += SECTION_GAP;
        y = drawSection(gui, theme, "recip.irisv.wiki.search", panelX, y, panelWidth, listBottom);
        drawLines(gui, panelX, y, panelWidth, listBottom, List.of(
                "recip.irisv.wiki.search_text",
                "recip.irisv.wiki.search_highlight",
                "recip.irisv.wiki.search_clear",
                "recip.irisv.wiki.search_prefixes"
        ));
    }

    private int drawSection(GuiGraphics gui, UiTheme theme, String titleKey, int x, int y, int width, int bottom) {
        if (y + 14 > bottom) return y;
        gui.drawString(this.font, section(titleKey), x, y, 0xFFFFFF, true);
        gui.fill(x, y + 13, x + width, y + 14, theme.gui_separatorLine());
        return y + 20;
    }

    private int drawLines(GuiGraphics gui, int x, int y, int width, int bottom, List<String> keys) {
        int textWidth = Math.max(40, width - 10);
        for (String key : keys) {
            Component line = Component.literal("§8> §7").append(Component.translatable(key));
            for (FormattedCharSequence part : this.font.split(line, textWidth)) {
                if (y + this.font.lineHeight > bottom) return y;
                gui.drawString(this.font, part, x + 6, y, 0xFFFFFFFF, false);
                y += this.font.lineHeight + LINE_GAP;
            }
        }
        return y;
    }

    private void renderHeader(GuiGraphics gui, UiTheme theme) {
        gui.fill(0, 0, this.width, 35, theme.gui_barColor());
        gui.fill(0, 34, this.width, 35, theme.gui_lineColor());
        gui.drawCenteredString(this.font, Component.literal("§l").append(this.title), this.width / 2, 12, 0xFFFFFF);
    }

    private void renderFooter(GuiGraphics gui, UiTheme theme) {
        int footerY = this.height - 40;
        int buttonWidth = returnButtonWidth();
        int holeLeft = (this.width - buttonWidth) / 2;
        int holeRight = holeLeft + buttonWidth;

        gui.fill(0, footerY, holeLeft, this.height, theme.gui_barColor());
        gui.fill(0, footerY, holeLeft, footerY + 1, theme.gui_lineColor());
        gui.fill(holeRight, footerY, this.width, this.height, theme.gui_barColor());
        gui.fill(holeRight, footerY, this.width, footerY + 1, theme.gui_lineColor());

        gui.fill(holeLeft, footerY, holeRight, this.height - 30, theme.gui_barColor());
        gui.fill(holeLeft, footerY, holeRight, footerY + 1, theme.gui_lineColor());
        gui.fill(holeLeft, this.height - 10, holeRight, this.height, theme.gui_barColor());
    }

    private int contentWidth() {
        return Math.min(BASE_WIDTH, Math.max(140, this.width - 36));
    }

    private int returnButtonWidth() {
        return Math.min(200, Math.max(120, this.width - 36));
    }

    private static Component section(String key) {
        return Component.literal("§8> §7").append(Component.translatable(key));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
