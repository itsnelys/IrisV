package net.opal.irisv.menu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.opal.irisv.option.ConfigOptions;
import net.opal.irisv.theme.UiTheme;

public class MenuOptionIrisv extends Screen {

    private final Screen parent;

    public MenuOptionIrisv(Screen parent) {
        super(Component.translatable("menu.irisv.options"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ConfigOptions config = ConfigOptions.getInstance();
        int centerX = this.width / 2;
        int bWidth = 200;
        int bHeight = 20;
        int halfW = (bWidth - 4) / 2;

        this.addRenderableWidget(Button.builder(getDebugButtonText(config.enableDebugChat), b -> {
            config.enableDebugChat = !config.enableDebugChat;
            b.setMessage(getDebugButtonText(config.enableDebugChat));
            config.save();
        }).pos(centerX - 100, 65).size(halfW, bHeight).build());

        this.addRenderableWidget(Button.builder(getThemeButtonText(config), b -> {
            config.theme = config.theme.next();
            b.setMessage(getThemeButtonText(config));
            config.save();
        }).pos(centerX + 2, 65).size(halfW, bHeight).build());

        this.addRenderableWidget(Button.builder(getOverlayButtonText(config.enableBlockTooltipOverlay), b -> {
            config.enableBlockTooltipOverlay = !config.enableBlockTooltipOverlay;
            b.setMessage(getOverlayButtonText(config.enableBlockTooltipOverlay));
            config.save();
        }).pos(centerX - 100, 115).size(halfW, bHeight).build());

        this.addRenderableWidget(new PositionSlider(centerX + 2, 115, halfW, bHeight, config));

        this.addRenderableWidget(Button.builder(getAdvancedButtonText(config.advancedTooltips), b -> {
            config.advancedTooltips = !config.advancedTooltips;
            b.setMessage(getAdvancedButtonText(config.advancedTooltips));
            config.save();
        }).pos(centerX - 100, 140).size(halfW, bHeight).build());

        this.addRenderableWidget(Button.builder(getLiquidAdvancedButtonText(config.advancedLiquidStats), b -> {
            config.advancedLiquidStats = !config.advancedLiquidStats;
            b.setMessage(getLiquidAdvancedButtonText(config.advancedLiquidStats));
            config.save();
        }).pos(centerX + 2, 140).size(halfW, bHeight).build());

        this.addRenderableWidget(Button.builder(getEntityButtonText(config.enableEntityTooltip), b -> {
            config.enableEntityTooltip = !config.enableEntityTooltip;
            b.setMessage(getEntityButtonText(config.enableEntityTooltip));
            config.save();
        }).pos(centerX - 100, 165).size(bWidth, bHeight).build());

        this.addRenderableWidget(Button.builder(getCompactButtonText(config.compactMode), b -> {
            config.compactMode = !config.compactMode;
            b.setMessage(getCompactButtonText(config.compactMode));
            config.save();
        }).pos(centerX - 100, 190).size(bWidth, bHeight).build());

        this.addRenderableWidget(Button.builder(getIndicatorGlobalButtonText(config.enableIndicators), b -> {
            config.enableIndicators = !config.enableIndicators;
            b.setMessage(getIndicatorGlobalButtonText(config.enableIndicators));
            config.save();
        }).pos(centerX - 100, 235).size(halfW, bHeight).build());

        this.addRenderableWidget(Button.builder(getIndicatorSideButtonText(config.indicatorPosition), b -> {
            config.indicatorPosition = config.indicatorPosition.next();
            b.setMessage(getIndicatorSideButtonText(config.indicatorPosition));
            config.save();
        }).pos(centerX + 2, 235).size(halfW, bHeight).build());

        this.addRenderableWidget(Button.builder(Component.translatable("menu.irisv.return"), b -> {
            if (this.minecraft != null) this.minecraft.setScreen(parent);
        }).pos(centerX - 100, this.height - 30).size(bWidth, bHeight).build());
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
    }

    private void renderSectionsLayout(GuiGraphics guiGraphics, UiTheme theme) {
        int centerX = this.width / 2;

        guiGraphics.drawString(this.font, section("menu.irisv.section.general"), centerX - 100, 50, 0xFFFFFF, true);

        int sep1Y = 98;
        guiGraphics.fill(centerX - 100, sep1Y, centerX + 100, sep1Y + 1, theme.gui_separatorLine());
        guiGraphics.drawString(this.font, section("menu.irisv.section.tooltips"), centerX - 100, 105, 0xFFFFFF, true);

        int sep2Y = 218;
        guiGraphics.fill(centerX - 100, sep2Y, centerX + 100, sep2Y + 1, theme.gui_separatorLine());
        guiGraphics.drawString(this.font, section("menu.irisv.section.indicators"), centerX - 100, 225, 0xFFFFFF, true);
    }

    private void renderFooter(GuiGraphics g, UiTheme theme) {
        int footerY = this.height - 40;
        int centerX = this.width / 2;
        int holeLeft = centerX - 100;
        int holeRight = centerX + 100;

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
}
