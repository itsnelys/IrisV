package net.opal.irisv.recip;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.Component;
import net.opal.irisv.theme.UiTheme;

import java.util.List;

public class RecipeWikiScreen extends Screen {
    private static final int BUTTON_HEIGHT = 20;
    private static final int BASE_WIDTH = 360;
    private static final int TABS_TOP = 68;
    private static final int CONTENT_BOTTOM_PADDING = 76;
    private static final int LINE_GAP = 3;
    private static final int TAB_GAP = 2;

    private final Screen parent;
    private Tab activeTab = Tab.CATALOG;
    private int scrollOffset;
    private String query = "";

    public RecipeWikiScreen(Screen parent) {
        super(Component.translatable("recip.irisv.wiki.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        EditBox search = new EditBox(font, (width - contentWidth()) / 2, 42, contentWidth() - 24, 20,
                Component.translatable("irisv.wiki.search_field"));
        search.setMaxLength(128);
        search.setHint(Component.translatable("irisv.wiki.search_field"));
        search.setValue(query);
        search.setResponder(value -> { query = value; scrollOffset = 0; });
        addRenderableWidget(search);
        addRenderableWidget(Button.builder(Component.literal("x"), button -> search.setValue(""))
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("irisv.wiki.clear")))
                .bounds((width + contentWidth()) / 2 - 20, 42, 20, 20).build());
        TabLayout tabs = createTabLayout();
        for (Tab tab : Tab.values()) {
            int x = tabs.x + (tab.ordinal() % tabs.columns) * (tabs.tabWidth + TAB_GAP);
            this.addRenderableWidget(Button.builder(Component.translatable(tab.titleKey), button -> {
                activeTab = tab;
                search.setValue("");
                scrollOffset = 0;
            }).pos(x, TABS_TOP + tab.ordinal() / tabs.columns * 24).size(tabs.tabWidth, BUTTON_HEIGHT).build());
        }

        addRenderableWidget(Button.builder(Component.translatable("irisv.support.title"), button ->
                minecraft.setScreen(new net.opal.irisv.menu.IrisVSupportScreen(this)))
                .bounds((width - returnButtonWidth()) / 2, height - 54, returnButtonWidth(), 20).build());

        int buttonWidth = returnButtonWidth();
        this.addRenderableWidget(Button.builder(Component.translatable("menu.irisv.return"), button -> {
            if (this.minecraft != null) this.minecraft.setScreen(parent);
        }).pos((this.width - buttonWidth) / 2, this.height - 30).size(buttonWidth, BUTTON_HEIGHT).build());
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll());
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
        int y = contentTop() - scrollOffset;
        int listBottom = this.height - CONTENT_BOTTOM_PADDING;

        if (listBottom <= contentTop()) return;
        gui.enableScissor(panelX, contentTop(), panelX + panelWidth, listBottom);
        boolean found = false;
        for (Tab tab : Tab.values()) {
            List<String> lines = matchingLines(tab);
            if (lines.isEmpty()) continue;
            found = true;
            y = drawSection(gui, theme, tab.titleKey, panelX, y, panelWidth, listBottom);
            y = drawLines(gui, panelX, y, panelWidth, listBottom, lines) + 10;
        }
        if (!found) gui.drawString(font, Component.translatable("irisv.wiki.no_results"), panelX, y, 0xAAAAAA);
        gui.disableScissor();
    }

    private int drawSection(GuiGraphics gui, UiTheme theme, String titleKey, int x, int y, int width, int bottom) {
        if (y + 14 >= contentTop() && y <= bottom) {
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
                if (y + this.font.lineHeight >= contentTop() && y <= bottom) {
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

        int top = contentTop();
        int bottom = this.height - CONTENT_BOTTOM_PADDING;
        int trackHeight = bottom - top;
        if (trackHeight <= 0) return;
        int barX = (this.width + contentWidth()) / 2 + 8;
        int contentHeight = contentHeight();
        int thumbHeight = Math.min(trackHeight, Math.max(8, (trackHeight * trackHeight) / Math.max(trackHeight, contentHeight)));
        int thumbY = top + (int) ((trackHeight - thumbHeight) * (scrollOffset / (float) maxScroll()));

        gui.fill(barX, top, barX + 2, bottom, theme.gui_separatorLine());
        gui.fill(barX - 1, thumbY, barX + 3, thumbY + thumbHeight, theme.gui_lineColor());
    }

    private void renderActiveTab(GuiGraphics gui, UiTheme theme) {
        TabLayout tabs = createTabLayout();
        if (!query.isBlank()) return;
        int x = tabs.x + activeTab.ordinal() % tabs.columns * (tabs.tabWidth + TAB_GAP);
        int y = TABS_TOP + activeTab.ordinal() / tabs.columns * 24;

        gui.fill(x, y, x + tabs.tabWidth, y + 1, theme.gui_lineColor());
        gui.fill(x, y + BUTTON_HEIGHT - 2, x + tabs.tabWidth, y + BUTTON_HEIGHT, theme.gui_lineColor());
        gui.fill(x, y, x + 1, y + BUTTON_HEIGHT, theme.gui_lineColor());
        gui.fill(x + tabs.tabWidth - 1, y, x + tabs.tabWidth, y + BUTTON_HEIGHT, theme.gui_lineColor());
    }

    private int maxScroll() {
        return Math.max(0, contentHeight() - Math.max(1, this.height - contentTop() - CONTENT_BOTTOM_PADDING));
    }

    private int contentHeight() {
        int width = contentWidth();
        int y = 0;
        for (Tab tab : Tab.values()) {
            List<String> lines = matchingLines(tab);
            if (!lines.isEmpty()) y = measureLines(measureSection(y), width, lines) + 10;
        }
        return y;
    }

    private List<String> matchingLines(Tab tab) {
        if (query.isBlank()) return activeTab == tab ? tab.lines : List.of();
        String title = Component.translatable(tab.titleKey).getString();
        return tab.lines.stream().filter(key -> WikiSearch.matches(query,
                title + " " + Component.translatable(key).getString())).toList();
    }

    private int contentTop() {
        int columns = createTabLayout().columns;
        return TABS_TOP + ((Tab.values().length + columns - 1) / columns) * 24 + 6;
    }

    @Override public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }

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
        int minimum = 64;
        for (Tab tab : Tab.values()) minimum = Math.max(minimum, font.width(Component.translatable(tab.titleKey)) + 12);
        int columns = Math.max(1, Math.min(Tab.values().length, (panelWidth + TAB_GAP) / (minimum + TAB_GAP)));
        int tabWidth = (panelWidth - TAB_GAP * (columns - 1)) / columns;
        return new TabLayout((this.width - panelWidth) / 2, tabWidth, columns);
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
                "recip.irisv.wiki.search_prefixes",
                "recip.irisv.wiki.properties"
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
                "recip.irisv.wiki.recipe.transfer",
                "recip.irisv.wiki.pinned",
                "recip.irisv.wiki.keys",
                "recip.irisv.wiki.recipe.hidden"
        ));

        private final String titleKey;
        private final List<String> lines;

        Tab(String titleKey, List<String> lines) {
            this.titleKey = titleKey;
            this.lines = lines;
        }
    }

    private record TabLayout(int x, int tabWidth, int columns) {}
}
