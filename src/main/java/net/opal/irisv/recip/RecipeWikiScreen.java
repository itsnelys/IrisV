package net.opal.irisv.recip;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.Component;
import net.opal.irisv.theme.UiTheme;

import java.util.List;

public class RecipeWikiScreen extends Screen {
    private static final int BUTTON_HEIGHT = 20;
    private static final int BASE_WIDTH = 360;
    private static final int TABS_TOP = 42;
    private static final int CONTENT_TOP = 72;
    private static final int CONTENT_BOTTOM_PADDING = 52;
    private static final int LINE_GAP = 3;
    private static final int TAB_GAP = 2;

    private final Screen parent;
    private Tab activeTab = Tab.CATALOG;
    private int scrollOffset;

    public RecipeWikiScreen(Screen parent) {
        super(Component.translatable("recip.irisv.wiki.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        TabLayout tabs = createTabLayout();
        for (Tab tab : Tab.values()) {
            int x = tabs.x + tab.ordinal() * (tabs.tabWidth + TAB_GAP);
            this.addRenderableWidget(Button.builder(Component.translatable(tab.titleKey), button -> {
                activeTab = tab;
                scrollOffset = 0;
            }).pos(x, TABS_TOP).size(tabs.tabWidth, BUTTON_HEIGHT).build());
        }

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
        renderActiveTab(gui, theme);
        renderContent(gui, theme);
        renderScrollbar(gui, theme);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int previous = scrollOffset;
        scrollOffset = Mth.clamp(scrollOffset - (int) (scrollY * 18), 0, maxScroll());
        return previous != scrollOffset || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void renderContent(GuiGraphics gui, UiTheme theme) {
        int panelWidth = contentWidth();
        int panelX = (this.width - panelWidth) / 2;
        int y = CONTENT_TOP - scrollOffset;
        int listBottom = this.height - CONTENT_BOTTOM_PADDING;

        y = drawSection(gui, theme, activeTab.titleKey, panelX, y, panelWidth, listBottom);
        drawLines(gui, panelX, y, panelWidth, listBottom, activeTab.lines);
    }

    private int drawSection(GuiGraphics gui, UiTheme theme, String titleKey, int x, int y, int width, int bottom) {
        if (y + 14 >= CONTENT_TOP && y <= bottom) {
            gui.drawString(this.font, section(titleKey), x, y, 0xFFFFFF, true);
            gui.fill(x, y + 13, x + width, y + 14, theme.gui_separatorLine());
        }
        return y + 20;
    }

    private int drawLines(GuiGraphics gui, int x, int y, int width, int bottom, List<String> keys) {
        int textWidth = Math.max(40, width - 10);
        for (String key : keys) {
            Component line = Component.literal("§8> §7").append(Component.translatable(key));
            for (FormattedCharSequence part : this.font.split(line, textWidth)) {
                if (y + this.font.lineHeight >= CONTENT_TOP && y <= bottom) {
                    gui.drawString(this.font, part, x + 6, y, 0xFFFFFFFF, false);
                }
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

    private void renderScrollbar(GuiGraphics gui, UiTheme theme) {
        if (maxScroll() <= 0) return;

        int top = CONTENT_TOP;
        int bottom = this.height - CONTENT_BOTTOM_PADDING;
        int trackHeight = bottom - top;
        int barX = (this.width + contentWidth()) / 2 + 8;
        int contentHeight = contentHeight();
        int thumbHeight = Math.max(18, (trackHeight * trackHeight) / Math.max(trackHeight, contentHeight));
        int thumbY = top + (int) ((trackHeight - thumbHeight) * (scrollOffset / (float) maxScroll()));

        gui.fill(barX, top, barX + 2, bottom, theme.gui_separatorLine());
        gui.fill(barX - 1, thumbY, barX + 3, thumbY + thumbHeight, theme.gui_lineColor());
    }

    private void renderActiveTab(GuiGraphics gui, UiTheme theme) {
        TabLayout tabs = createTabLayout();
        int x = tabs.x + activeTab.ordinal() * (tabs.tabWidth + TAB_GAP);
        int y = TABS_TOP;

        gui.fill(x, y, x + tabs.tabWidth, y + 1, theme.gui_lineColor());
        gui.fill(x, y + BUTTON_HEIGHT - 2, x + tabs.tabWidth, y + BUTTON_HEIGHT, theme.gui_lineColor());
        gui.fill(x, y, x + 1, y + BUTTON_HEIGHT, theme.gui_lineColor());
        gui.fill(x + tabs.tabWidth - 1, y, x + tabs.tabWidth, y + BUTTON_HEIGHT, theme.gui_lineColor());
    }

    private int maxScroll() {
        return Math.max(0, contentHeight() - (this.height - CONTENT_TOP - CONTENT_BOTTOM_PADDING));
    }

    private int contentHeight() {
        int width = contentWidth();
        int y = CONTENT_TOP;
        y = measureSection(y);
        y = measureLines(y, width, activeTab.lines);
        return y - CONTENT_TOP;
    }

    private int measureSection(int y) {
        return y + 20;
    }

    private int measureLines(int y, int width, List<String> keys) {
        int textWidth = Math.max(40, width - 10);
        for (String key : keys) {
            Component line = Component.literal("§8> §7").append(Component.translatable(key));
            y += this.font.split(line, textWidth).size() * (this.font.lineHeight + LINE_GAP);
        }
        return y;
    }

    private static Component section(String key) {
        return Component.literal("§8> §7").append(Component.translatable(key));
    }

    private TabLayout createTabLayout() {
        int panelWidth = contentWidth();
        int tabWidth = Math.max(46, (panelWidth - TAB_GAP * (Tab.values().length - 1)) / Tab.values().length);
        int totalWidth = tabWidth * Tab.values().length + TAB_GAP * (Tab.values().length - 1);
        return new TabLayout((this.width - totalWidth) / 2, tabWidth);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private enum Tab {
        CATALOG("recip.irisv.wiki.catalog", List.of(
                "recip.irisv.wiki.catalog.pages",
                "recip.irisv.wiki.catalog.categories",
                "recip.irisv.wiki.catalog.favorites",
                "recip.irisv.wiki.catalog.eye"
        )),
        SEARCH("recip.irisv.wiki.search", List.of(
                "recip.irisv.wiki.search_text",
                "recip.irisv.wiki.search_highlight",
                "recip.irisv.wiki.search_clear",
                "recip.irisv.wiki.search_prefixes"
        )),
        TOOLTIPS("recip.irisv.wiki.tooltips", List.of(
                "recip.irisv.wiki.tooltips.overlay",
                "recip.irisv.wiki.tooltips.providers",
                "recip.irisv.wiki.tooltips.advanced",
                "recip.irisv.wiki.tooltips.position"
        )),
        HUD("recip.irisv.wiki.hud", List.of(
                "recip.irisv.wiki.hud.indicators",
                "recip.irisv.wiki.hud.durability",
                "recip.irisv.wiki.hud.side"
        )),
        RECIPE("recip.irisv.wiki.recipe", List.of(
                "recip.irisv.wiki.recipe.survival",
                "recip.irisv.wiki.recipe.creative",
                "recip.irisv.wiki.recipe.hidden"
        ));

        private final String titleKey;
        private final List<String> lines;

        Tab(String titleKey, List<String> lines) {
            this.titleKey = titleKey;
            this.lines = lines;
        }
    }

    private record TabLayout(int x, int tabWidth) {}
}
