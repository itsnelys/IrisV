package net.opal.irisv.menu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.opal.irisv.option.ConfigOptions;
import net.opal.irisv.theme.UiTheme;

import java.util.ArrayList;
import java.util.List;

public class MenuOptionIrisv extends Screen {
    private static final int BUTTON_HEIGHT = 20;
    private static final int BASE_WIDTH = 200;
    private static final int BUTTON_GAP = 4;
    private static final int ROW_GAP = 5;
    private static final int SECTION_GAP = 12;
    private static final int CONTENT_TOP = 50;
    private static final int CONTENT_BOTTOM_PADDING = 52;

    private final Screen parent;
    private final List<PositionedWidget> contentWidgets = new ArrayList<>();
    private int scrollOffset;

    public MenuOptionIrisv(Screen parent) {
        super(Component.translatable("menu.irisv.options"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        contentWidgets.clear();
        ConfigOptions config = ConfigOptions.getInstance();
        Layout layout = createLayout();
        int halfW = (layout.width - BUTTON_GAP) / 2;

        addContentWidget(Button.builder(getDebugButtonText(config.enableDebugChat), b -> {
            config.enableDebugChat = !config.enableDebugChat;
            b.setMessage(getDebugButtonText(config.enableDebugChat));
            config.save();
        }).tooltip(tooltip("menu.irisv.tooltip.debug_chat")).pos(layout.left, layout.generalButtonY).size(halfW, BUTTON_HEIGHT).build(), layout.left, layout.generalButtonY, halfW);

        addContentWidget(Button.builder(getThemeButtonText(config), b -> {
            config.theme = config.theme.next();
            b.setMessage(getThemeButtonText(config));
            config.save();
        }).tooltip(tooltip("menu.irisv.tooltip.theme")).pos(layout.left + halfW + BUTTON_GAP, layout.generalButtonY).size(halfW, BUTTON_HEIGHT).build(), layout.left + halfW + BUTTON_GAP, layout.generalButtonY, halfW);

        addContentWidget(Button.builder(Component.translatable("menu.irisv.providers"), b -> {
            if (this.minecraft != null) this.minecraft.setScreen(new ProviderOptionsMenu(this));
        }).tooltip(tooltip("menu.irisv.tooltip.providers")).pos(layout.left, layout.generalProviderButtonY).size(layout.width, BUTTON_HEIGHT).build(), layout.left, layout.generalProviderButtonY, layout.width);

        int supportY = layout.generalProviderButtonY + layout.rowStep;
        addContentWidget(Button.builder(Component.translatable("irisv.support.title"), b ->
                this.minecraft.setScreen(new IrisVSupportScreen(this)))
                .bounds(layout.left, supportY, layout.width, BUTTON_HEIGHT).build(), layout.left, supportY, layout.width);

        addContentWidget(Button.builder(getOverlayButtonText(config.enableBlockTooltipOverlay), b -> {
            config.enableBlockTooltipOverlay = !config.enableBlockTooltipOverlay;
            b.setMessage(getOverlayButtonText(config.enableBlockTooltipOverlay));
            config.save();
        }).tooltip(tooltip("menu.irisv.tooltip.overlay")).pos(layout.left, layout.tooltipsButtonY).size(halfW, BUTTON_HEIGHT).build(), layout.left, layout.tooltipsButtonY, halfW);

        addContentWidget(new PositionSlider(layout.left + halfW + BUTTON_GAP, layout.tooltipsButtonY, halfW, BUTTON_HEIGHT, config), layout.left + halfW + BUTTON_GAP, layout.tooltipsButtonY, halfW);

        addContentWidget(Button.builder(getAdvancedButtonText(config.advancedTooltips), b -> {
            config.advancedTooltips = !config.advancedTooltips;
            b.setMessage(getAdvancedButtonText(config.advancedTooltips));
            config.save();
        }).tooltip(tooltip("menu.irisv.tooltip.advanced_tooltips")).pos(layout.left, layout.tooltipsButtonY + layout.rowStep).size(halfW, BUTTON_HEIGHT).build(), layout.left, layout.tooltipsButtonY + layout.rowStep, halfW);

        addContentWidget(Button.builder(getLiquidAdvancedButtonText(config.advancedLiquidStats), b -> {
            config.advancedLiquidStats = !config.advancedLiquidStats;
            b.setMessage(getLiquidAdvancedButtonText(config.advancedLiquidStats));
            config.save();
        }).tooltip(tooltip("menu.irisv.tooltip.advanced_liquids")).pos(layout.left + halfW + BUTTON_GAP, layout.tooltipsButtonY + layout.rowStep).size(halfW, BUTTON_HEIGHT).build(), layout.left + halfW + BUTTON_GAP, layout.tooltipsButtonY + layout.rowStep, halfW);

        addContentWidget(Button.builder(getEntityButtonText(config.enableEntityTooltip), b -> {
            config.enableEntityTooltip = !config.enableEntityTooltip;
            b.setMessage(getEntityButtonText(config.enableEntityTooltip));
            config.save();
        }).tooltip(tooltip("menu.irisv.tooltip.entities")).pos(layout.left, layout.tooltipsButtonY + layout.rowStep * 2).size(halfW, BUTTON_HEIGHT).build(), layout.left, layout.tooltipsButtonY + layout.rowStep * 2, halfW);

        addContentWidget(Button.builder(getCompactButtonText(config.compactMode), b -> {
            config.compactMode = !config.compactMode;
            b.setMessage(getCompactButtonText(config.compactMode));
            config.save();
        }).tooltip(tooltip("menu.irisv.tooltip.compact")).pos(layout.left + halfW + BUTTON_GAP, layout.tooltipsButtonY + layout.rowStep * 2).size(halfW, BUTTON_HEIGHT).build(), layout.left + halfW + BUTTON_GAP, layout.tooltipsButtonY + layout.rowStep * 2, halfW);

        addContentWidget(Button.builder(getIndicatorGlobalButtonText(config.enableIndicators), b -> {
            config.enableIndicators = !config.enableIndicators;
            b.setMessage(getIndicatorGlobalButtonText(config.enableIndicators));
            config.save();
        }).tooltip(tooltip("menu.irisv.tooltip.hud")).pos(layout.left, layout.indicatorsButtonY).size(halfW, BUTTON_HEIGHT).build(), layout.left, layout.indicatorsButtonY, halfW);

        addContentWidget(Button.builder(getIndicatorSideButtonText(config.indicatorPosition), b -> {
            config.indicatorPosition = config.indicatorPosition.next();
            b.setMessage(getIndicatorSideButtonText(config.indicatorPosition));
            config.save();
        }).tooltip(tooltip("menu.irisv.tooltip.side")).pos(layout.left + halfW + BUTTON_GAP, layout.indicatorsButtonY).size(halfW, BUTTON_HEIGHT).build(), layout.left + halfW + BUTTON_GAP, layout.indicatorsButtonY, halfW);

        this.addRenderableWidget(Button.builder(Component.translatable("menu.irisv.return"), b -> {
            if (this.minecraft != null) this.minecraft.setScreen(parent);
        }).tooltip(tooltip("menu.irisv.tooltip.return")).pos(layout.left, this.height - 30).size(layout.width, BUTTON_HEIGHT).build());

        clampScroll();
        updateContentPositions();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        UiTheme theme = UiTheme.getCurrent();
        guiGraphics.fill(0, 0, this.width, this.height, theme.gui_bgOverlay());

        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        guiGraphics.fill(0, 0, this.width, 35, theme.gui_barColor());
        guiGraphics.fill(0, 34, this.width, 35, theme.gui_lineColor());
        guiGraphics.drawCenteredString(this.font, Component.literal("§l").append(this.title), this.width / 2, 12, 0xFFFFFF);

        renderFooter(guiGraphics, theme);
        renderSectionsLayout(guiGraphics, theme);
        renderScrollbar(guiGraphics, theme);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int previous = scrollOffset;
        scrollOffset = Mth.clamp(scrollOffset - (int) (scrollY * (BUTTON_HEIGHT + ROW_GAP)), 0, maxScroll());
        if (previous != scrollOffset) {
            updateContentPositions();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void renderSectionsLayout(GuiGraphics guiGraphics, UiTheme theme) {
        Layout layout = createLayout();

        drawSection(guiGraphics, theme, "menu.irisv.section.general", layout.left, layout.generalTitleY - scrollOffset, layout.width);
        drawSection(guiGraphics, theme, "menu.irisv.section.tooltips", layout.left, layout.tooltipsTitleY - scrollOffset, layout.width);
        drawSection(guiGraphics, theme, "menu.irisv.section.indicators", layout.left, layout.indicatorsTitleY - scrollOffset, layout.width);
    }

    private void drawSection(GuiGraphics guiGraphics, UiTheme theme, String key, int x, int y, int width) {
        if (y + 14 < CONTENT_TOP || y > listBottom()) return;
        guiGraphics.drawString(this.font, section(key), x, y, 0xFFFFFF, true);
        int separatorY = y + 13;
        guiGraphics.fill(x, separatorY, x + width, separatorY + 1, theme.gui_separatorLine());
    }

    private void renderFooter(GuiGraphics g, UiTheme theme) {
        int footerY = this.height - 40;
        Layout layout = createLayout();
        int holeLeft = layout.left;
        int holeRight = layout.left + layout.width;

        g.fill(0, footerY, holeLeft, this.height, theme.gui_barColor());
        g.fill(0, footerY, holeLeft, footerY + 1, theme.gui_lineColor());
        g.fill(holeRight, footerY, this.width, this.height, theme.gui_barColor());
        g.fill(holeRight, footerY, this.width, footerY + 1, theme.gui_lineColor());

        g.fill(holeLeft, footerY, holeRight, this.height - 30, theme.gui_barColor());
        g.fill(holeLeft, footerY, holeRight, footerY + 1, theme.gui_lineColor());
        g.fill(holeLeft, this.height - 10, holeRight, this.height, theme.gui_barColor());
    }

    private Component getThemeButtonText(ConfigOptions config) {
        UiTheme theme = UiTheme.getCurrent();
        return Component.translatable("menu.irisv.theme", colored(theme.gui_valColor(), Component.translatable(config.theme.translationKey())));
    }

    private Component getDebugButtonText(boolean on) {
        UiTheme theme = UiTheme.getCurrent();
        return Component.translatable("menu.irisv.debug_chat", state(on, theme));
    }

    private Component getOverlayButtonText(boolean on) {
        UiTheme theme = UiTheme.getCurrent();
        return Component.translatable("menu.irisv.overlay", state(on, theme));
    }

    private Component getCompactButtonText(boolean on) {
        UiTheme theme = UiTheme.getCurrent();
        return Component.translatable("menu.irisv.compact", state(on, theme));
    }

    private Component getAdvancedButtonText(boolean on) {
        UiTheme theme = UiTheme.getCurrent();
        return Component.translatable("menu.irisv.advanced_tooltips", state(on, theme));
    }

    private Component getLiquidAdvancedButtonText(boolean on) {
        UiTheme theme = UiTheme.getCurrent();
        return Component.translatable("menu.irisv.advanced_liquids", state(on, theme));
    }

    private Component getEntityButtonText(boolean on) {
        UiTheme theme = UiTheme.getCurrent();
        return Component.translatable("menu.irisv.entities", state(on, theme));
    }

    private Component getIndicatorGlobalButtonText(boolean on) {
        UiTheme theme = UiTheme.getCurrent();
        return Component.translatable("menu.irisv.hud", state(on, theme));
    }

    private Component getIndicatorSideButtonText(ConfigOptions.IndicatorPosition position) {
        UiTheme theme = UiTheme.getCurrent();
        String key = position == ConfigOptions.IndicatorPosition.LEFT ? "menu.irisv.left" : "menu.irisv.right";
        return Component.translatable("menu.irisv.side", colored(theme.gui_valColor(), Component.translatable(key)));
    }

    private static Component section(String key) {
        return Component.literal("§8> §7").append(Component.translatable(key));
    }

    private static Tooltip tooltip(String key) {
        return Tooltip.create(Component.translatable(key));
    }

    private Layout createLayout() {
        int width = Math.min(BASE_WIDTH, Math.max(120, this.width - 36));
        int left = (this.width - width) / 2;
        int availableHeight = Math.max(1, this.height - CONTENT_TOP - CONTENT_BOTTOM_PADDING);
        int defaultRowStep = BUTTON_HEIGHT + ROW_GAP;
        int defaultContentHeight = 198;
        int rowStep = availableHeight < defaultContentHeight ? Math.max(BUTTON_HEIGHT + 1, (availableHeight - 48) / 6) : defaultRowStep;
        int sectionGap = availableHeight < defaultContentHeight ? Math.max(6, SECTION_GAP - 4) : SECTION_GAP;

        int generalTitleY = CONTENT_TOP;
        int generalButtonY = generalTitleY + 15;
        int generalProviderButtonY = generalButtonY + rowStep;
        int tooltipsTitleY = generalProviderButtonY + rowStep + BUTTON_HEIGHT + sectionGap;
        int tooltipsButtonY = tooltipsTitleY + 15;
        int indicatorsTitleY = tooltipsButtonY + rowStep * 3 + sectionGap - 5;
        int indicatorsButtonY = indicatorsTitleY + 15;

        return new Layout(left, width, rowStep, generalTitleY, generalButtonY, generalProviderButtonY, tooltipsTitleY, tooltipsButtonY, indicatorsTitleY, indicatorsButtonY);
    }

    private static Component state(boolean on, UiTheme theme) {
        return colored(on ? theme.gui_onColor() : theme.gui_offColor(),
                Component.translatable(on ? "menu.irisv.on" : "menu.irisv.off"));
    }

    private static Component colored(String color, Component text) {
        return Component.literal(color).append(text);
    }

    private class PositionSlider extends AbstractSliderButton {
        private final ConfigOptions config;

        public PositionSlider(int x, int y, int width, int height, ConfigOptions config) {
            super(x, y, width, height, Component.empty(), config.tooltipPosition.legacyValue() / 4.0);
            this.config = config;
            setTooltip(tooltip("menu.irisv.tooltip.position"));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            UiTheme theme = UiTheme.getCurrent();
            String key = switch(config.tooltipPosition) {
                case TOP_LEFT -> "menu.irisv.position.top_left";
                case TOP_RIGHT -> "menu.irisv.position.top_right";
                case BOTTOM_LEFT -> "menu.irisv.position.bottom_left";
                case BOTTOM_RIGHT -> "menu.irisv.position.bottom_right";
                case TOP_CENTER -> "menu.irisv.position.top_center";
            };
            setMessage(Component.translatable("menu.irisv.position", colored(theme.gui_valColor(), Component.translatable(key))));
        }

        @Override
        protected void applyValue() {
            int newValue = Mth.clamp((int)(this.value * 4.99), 0, 4);
            ConfigOptions.TooltipPosition newPosition = ConfigOptions.TooltipPosition.fromSlider(newValue);
            if (config.tooltipPosition != newPosition) {
                config.tooltipPosition = newPosition;
                config.save();
            }
        }
    }

    private <T extends AbstractWidget> T addContentWidget(T widget, int x, int y, int width) {
        contentWidgets.add(new PositionedWidget(widget, x, y, width));
        return this.addRenderableWidget(widget);
    }

    private void updateContentPositions() {
        int listBottom = listBottom();
        for (PositionedWidget positioned : contentWidgets) {
            AbstractWidget widget = positioned.widget;
            widget.setX(positioned.x);
            widget.setY(positioned.y - scrollOffset);
            widget.setWidth(positioned.width);

            boolean visible = widget.getY() >= CONTENT_TOP && widget.getY() + BUTTON_HEIGHT <= listBottom;
            widget.visible = visible;
            widget.active = visible;
        }
    }

    private void renderScrollbar(GuiGraphics guiGraphics, UiTheme theme) {
        if (maxScroll() <= 0) return;

        Layout layout = createLayout();
        int top = CONTENT_TOP;
        int bottom = listBottom();
        int barX = layout.left + layout.width + 8;
        int trackHeight = bottom - top;
        int thumbHeight = Math.max(18, (trackHeight * trackHeight) / Math.max(trackHeight, contentHeight()));
        int thumbY = top + (int) ((trackHeight - thumbHeight) * (scrollOffset / (float) maxScroll()));

        guiGraphics.fill(barX, top, barX + 2, bottom, theme.gui_separatorLine());
        guiGraphics.fill(barX - 1, thumbY, barX + 3, thumbY + thumbHeight, theme.gui_lineColor());
    }

    private void clampScroll() {
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll());
    }

    private int maxScroll() {
        return Math.max(0, contentHeight() - (listBottom() - CONTENT_TOP));
    }

    private int contentHeight() {
        Layout layout = createLayout();
        return layout.indicatorsButtonY + BUTTON_HEIGHT - CONTENT_TOP;
    }

    private int listBottom() {
        return this.height - CONTENT_BOTTOM_PADDING;
    }

    private record PositionedWidget(AbstractWidget widget, int x, int y, int width) {}

    private record Layout(
            int left,
            int width,
            int rowStep,
            int generalTitleY,
            int generalButtonY,
            int generalProviderButtonY,
            int tooltipsTitleY,
            int tooltipsButtonY,
            int indicatorsTitleY,
            int indicatorsButtonY
    ) {}
}
