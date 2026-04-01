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

        // --- ZONE GÉNÉRAL ---
        // Ligne 1 : Debug Chat & Thème
        this.addRenderableWidget(Button.builder(getDebugButtonText(config.enableDebugChat), b -> {
                    config.enableDebugChat = !config.enableDebugChat;
                    b.setMessage(getDebugButtonText(config.enableDebugChat));
                    config.save();
                })
                .pos(centerX - 100, 65)
                .size(halfW, bHeight)
                .build());

        this.addRenderableWidget(Button.builder(getThemeButtonText(config), b -> {
                    config.themeIndex = (config.themeIndex + 1) % 2; // Alterne entre 0 et 1
                    b.setMessage(getThemeButtonText(config));
                    config.save();
                })
                .pos(centerX + 2, 65)
                .size(halfW, bHeight)
                .build());

        // --- ZONE TOOLTIPS ---
        // Ligne 2 : Overlay & Position Slider
        this.addRenderableWidget(Button.builder(getOverlayButtonText(config.enableBlockTooltipOverlay), b -> {
                    config.enableBlockTooltipOverlay = !config.enableBlockTooltipOverlay;
                    b.setMessage(getOverlayButtonText(config.enableBlockTooltipOverlay));
                    config.save();
                })
                .pos(centerX - 100, 125)
                .size(halfW, bHeight)
                .build());

        this.addRenderableWidget(new PositionSlider(centerX + 2, 125, halfW, bHeight, config));

        // Ligne 3 : Mode Compact
        this.addRenderableWidget(Button.builder(getCompactButtonText(config.compactMode), b -> {
                    config.compactMode = !config.compactMode;
                    b.setMessage(getCompactButtonText(config.compactMode));
                    config.save();
                })
                .pos(centerX - 100, 150)
                .size(bWidth, bHeight)
                .build());

        // --- BOUTON RETOUR ---
        this.addRenderableWidget(Button.builder(Component.translatable("menu.irisv.return"), b -> {
                    if (this.minecraft != null) this.minecraft.setScreen(parent);
                })
                .pos(centerX - 100, this.height - 30)
                .size(bWidth, bHeight)
                .build());
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

    private void renderFooter(GuiGraphics g, UiTheme theme) {
        int footerY = this.height - 40;
        int centerX = this.width / 2;
        int holeLeft = centerX - 100;
        int holeRight = centerX + 100;

        // Barres latérales et séparateurs
        g.fill(0, footerY, holeLeft, this.height, theme.gui_barColor());
        g.fill(0, footerY, holeLeft, footerY + 1, theme.gui_lineColor());
        g.fill(holeRight, footerY, this.width, this.height, theme.gui_barColor());
        g.fill(holeRight, footerY, this.width, footerY + 1, theme.gui_lineColor());

        // Encadrement bouton retour
        g.fill(holeLeft, footerY, holeRight, this.height - 30, theme.gui_barColor());
        g.fill(holeLeft, footerY, holeRight, footerY + 1, theme.gui_lineColor());
        g.fill(holeLeft, this.height - 10, holeRight, this.height, theme.gui_barColor());
    }

    private void renderSectionsLayout(GuiGraphics guiGraphics, UiTheme theme) {
        int centerX = this.width / 2;
        guiGraphics.drawString(this.font, "§8> §7GÉNÉRAL", centerX - 100, 50, 0xFFFFFF, true);

        int sepY = 98;
        guiGraphics.fill(centerX - 100, sepY, centerX + 100, sepY + 1, theme.gui_separatorLine());

        guiGraphics.drawString(this.font, "§8> §7TOOLTIPS", centerX - 100, 110, 0xFFFFFF, true);
    }

    // --- HELPERS ---
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

    // --- CLASSE INTERNE POUR LE SLIDER ---
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