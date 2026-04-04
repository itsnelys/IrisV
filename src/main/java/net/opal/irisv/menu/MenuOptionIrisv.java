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
        super(Component.literal("IrisV Options"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ConfigOptions config = ConfigOptions.getInstance();
        int centerX = this.width / 2;
        int bWidth = 200;
        int bHeight = 20;
        int halfW = (bWidth - 4) / 2;

        // --- ZONE GÉNÉRALE (Y: 65) ---
        this.addRenderableWidget(Button.builder(getDebugButtonText(config.enableDebugChat), b -> {
            config.enableDebugChat = !config.enableDebugChat;
            b.setMessage(getDebugButtonText(config.enableDebugChat));
            config.save();
        }).pos(centerX - 100, 65).size(halfW, bHeight).build());

// 1. On définit le nombre total de thèmes disponibles
// (C'est plus propre d'avoir une méthode statique ou un tableau dans UiTheme)
        int totalThemes = 7; // Darkness, Frost, Elder, Abyss, Forest, Crimson, Valhalla

        this.addRenderableWidget(Button.builder(getThemeButtonText(config), b -> {
            // --- LE FIX : On boucle sur le nombre TOTAL de thèmes ---
            config.themeIndex = (config.themeIndex + 1) % totalThemes;

            b.setMessage(getThemeButtonText(config));
            config.save();
        }).pos(centerX + 2, 65).size(halfW, bHeight).build());

        // --- ZONE TOOLTIPS (Y: 115+) ---
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

        // --- ZONE INDICATORS (Y: 235+) ---
        this.addRenderableWidget(Button.builder(getIndicatorGlobalButtonText(config.enableIndicators), b -> {
            config.enableIndicators = !config.enableIndicators;
            b.setMessage(getIndicatorGlobalButtonText(config.enableIndicators));
            config.save();
        }).pos(centerX - 100, 235).size(halfW, bHeight).build());

        this.addRenderableWidget(Button.builder(getIndicatorSideButtonText(config.indicatorPosition), b -> {
            config.indicatorPosition = (config.indicatorPosition == 1) ? 2 : 1;
            b.setMessage(getIndicatorSideButtonText(config.indicatorPosition));
            config.save();
        }).pos(centerX + 2, 235).size(halfW, bHeight).build());

        // --- BOUTON RETOUR ---
        this.addRenderableWidget(Button.builder(Component.translatable("menu.irisv.return"), b -> {
            if (this.minecraft != null) this.minecraft.setScreen(parent);
        }).pos(centerX - 100, this.height - 30).size(bWidth, bHeight).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        UiTheme theme = UiTheme.getCurrent();
        guiGraphics.fill(0, 0, this.width, this.height, theme.gui_bgOverlay());

        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        // Header
        guiGraphics.fill(0, 0, this.width, 35, theme.gui_barColor());
        guiGraphics.fill(0, 34, this.width, 35, theme.gui_lineColor());
        guiGraphics.drawCenteredString(this.font, "§l" + this.title.getString(), this.width / 2, 12, 0xFFFFFF);

        renderFooter(guiGraphics, theme);
        renderSectionsLayout(guiGraphics, theme);
    }

    private void renderSectionsLayout(GuiGraphics guiGraphics, UiTheme theme) {
        int centerX = this.width / 2;

        // Section Général
        guiGraphics.drawString(this.font, "§8> §7GÉNÉRAL", centerX - 100, 50, 0xFFFFFF, true);

        // Section Tooltips
        int sep1Y = 98;
        guiGraphics.fill(centerX - 100, sep1Y, centerX + 100, sep1Y + 1, theme.gui_separatorLine());
        guiGraphics.drawString(this.font, "§8> §7TOOLTIPS", centerX - 100, 105, 0xFFFFFF, true);

        // Section Indicators
        int sep2Y = 218;
        guiGraphics.fill(centerX - 100, sep2Y, centerX + 100, sep2Y + 1, theme.gui_separatorLine());
        guiGraphics.drawString(this.font, "§8> §7INDICATORS (HUD)", centerX - 100, 225, 0xFFFFFF, true);
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

    // --- HELPERS DE TEXTE ---
    private Component getThemeButtonText(ConfigOptions config) {
        UiTheme theme = UiTheme.getCurrent();
        return Component.literal("Thème: " + theme.gui_valColor() + theme.name());
    }

    private Component getDebugButtonText(boolean on) {
        UiTheme theme = UiTheme.getCurrent();
        return Component.literal("Debug Chat: " + (on ? theme.gui_onColor() + "Activé" : theme.gui_offColor() + "Désactivé"));
    }

    private Component getOverlayButtonText(boolean on) {
        UiTheme theme = UiTheme.getCurrent();
        return Component.literal("Overlay: " + (on ? theme.gui_onColor() + "ON" : theme.gui_offColor() + "OFF"));
    }

    private Component getCompactButtonText(boolean on) {
        UiTheme theme = UiTheme.getCurrent();
        return Component.literal("Mode Compact: " + (on ? theme.gui_onColor() + "OUI" : theme.gui_offColor() + "NON"));
    }

    private Component getAdvancedButtonText(boolean on) {
        UiTheme theme = UiTheme.getCurrent();
        return Component.literal("T-Adv: " + (on ? theme.gui_onColor() + "ON" : theme.gui_offColor() + "OFF"));
    }

    private Component getLiquidAdvancedButtonText(boolean on) {
        UiTheme theme = UiTheme.getCurrent();
        return Component.literal("L-Adv: " + (on ? theme.gui_onColor() + "ON" : theme.gui_offColor() + "OFF"));
    }

    private Component getEntityButtonText(boolean on) {
        UiTheme theme = UiTheme.getCurrent();
        return Component.literal("Entités: " + (on ? theme.gui_onColor() + "Activées" : theme.gui_offColor() + "Désactivées"));
    }

    private Component getIndicatorGlobalButtonText(boolean on) {
        UiTheme theme = UiTheme.getCurrent();
        return Component.literal("HUD: " + (on ? theme.gui_onColor() + "ON" : theme.gui_offColor() + "OFF"));
    }

    private Component getIndicatorSideButtonText(int pos) {
        UiTheme theme = UiTheme.getCurrent();
        String side = (pos == 1) ? "Gauche" : "Droite";
        return Component.literal("Côté: " + theme.gui_valColor() + side);
    }

    // --- SLIDER DE POSITION ---
    private class PositionSlider extends AbstractSliderButton {
        private final ConfigOptions config;

        public PositionSlider(int x, int y, int width, int height, ConfigOptions config) {
            super(x, y, width, height, Component.empty(), config.tooltipPosition / 4.0);
            this.config = config;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            UiTheme theme = UiTheme.getCurrent();
            String s = switch(config.tooltipPosition) {
                case 1 -> "Haut G.";
                case 2 -> "Haut D.";
                case 3 -> "Bas G.";
                case 4 -> "Bas D.";
                default -> "Haut C.";
            };
            setMessage(Component.literal("Pos: " + theme.gui_valColor() + s));
        }

        @Override
        protected void applyValue() {
            int newValue = Mth.clamp((int)(this.value * 4.99), 0, 4);
            if (config.tooltipPosition != newValue) {
                config.tooltipPosition = newValue;
                config.save();
            }
        }
    }
}