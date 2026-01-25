package net.opal.irisv.menu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.opal.irisv.option.ConfigOptions;

public class MenuOptionIrisv extends Screen {

    private final Screen parent;

    public MenuOptionIrisv(Screen parent) {
        super(Component.literal("IrisV Options"));
        this.parent = parent;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);

        int titleY;
        if (!this.renderables.isEmpty() && this.renderables.get(0) instanceof AbstractWidget firstButton) {
            titleY = firstButton.getY() - 20;
        } else {
            titleY = 20;
        }

        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable("menu.irisv.options").getString(),
                this.width / 2,
                titleY,
                0xFFFFFF
        );

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    protected void init() {
        ConfigOptions config = ConfigOptions.getInstance();

        int buttonWidth = 100;
        int buttonHeight = 20;
        int spacing = 10;

        int x1 = this.width / 2 - buttonWidth - spacing / 2;
        int x2 = this.width / 2 + spacing / 2;
        int y = this.height / 2 - 10;

        this.addRenderableWidget(Button.builder(
                        getDebugButtonText(config.enableDebugChat),
                        b -> {
                            config.enableDebugChat = !config.enableDebugChat;
                            b.setMessage(getDebugButtonText(config.enableDebugChat));
                            config.save();
                        })
                .pos(x1, y)
                .size(buttonWidth, buttonHeight)
                .build());

        this.addRenderableWidget(Button.builder(
                        getBlockTooltipOverlayButtonText(config.enableBlockTooltipOverlay),
                        b -> {
                            config.enableBlockTooltipOverlay = !config.enableBlockTooltipOverlay;
                            b.setMessage(getBlockTooltipOverlayButtonText(config.enableBlockTooltipOverlay));
                            config.save();
                        })
                .pos(x2, y)
                .size(buttonWidth, buttonHeight)
                .build());

        this.addRenderableWidget(Button.builder(
                        Component.translatable("menu.irisv.return"),
                        b -> this.minecraft.setScreen(parent))
                .pos(this.width / 2 - 50, this.height / 2 + 20)
                .size(100, 20)
                .build());
    }

    private Component getDebugButtonText(boolean isEnabled) {
        String statusKey = isEnabled ? "menu.irisv.on" : "menu.irisv.off";
        return Component.translatable("menu.irisv.debug_chat", Component.translatable(statusKey).getString());
    }
    private Component getBlockTooltipOverlayButtonText(boolean isEnabled) {
        String statusKey = isEnabled ? "menu.irisv.on" : "menu.irisv.off";
        return Component.translatable("menu.irisv.block_tooltip_overlay", Component.translatable(statusKey).getString());
    }
}