package net.opal.irisv.recip;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

final class RecipeNavigationButton extends Button {
    private final boolean next;

    RecipeNavigationButton(int x, int y, int size, boolean next, OnPress action) {
        super(x, y, size, size, Component.translatable(next ? "recip.irisv.next" : "recip.irisv.previous"), action, DEFAULT_NARRATION);
        this.next = next;
        setTooltip(Tooltip.create(getMessage()));
    }

    @Override protected void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        draw(gui, getX(), getY(), getWidth(), next, isHoveredOrFocused(), active);
    }

    static void draw(GuiGraphics gui, int x, int y, int size, boolean next, boolean hovered, boolean active) {
        background(gui, x, y, size, size, hovered, active);
        gui.pose().pushPose();
        gui.pose().translate(x + 3, y + 3, 0);
        float scale = (size - 6) / 16F;
        gui.pose().scale(scale, scale, 1);
        gui.blit(ResourceLocation.fromNamespaceAndPath("irisv", "textures/gui/page_" + (next ? "right" : "left") + ".png"),
                0, 0, 0F, 0F, 16, 16, 16, 16);
        gui.pose().popPose();
        if (!active) gui.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0x99181818);
    }

    static void background(GuiGraphics gui, int x, int y, int width, int height, boolean hovered, boolean active) {
        String sprite = !active ? "widget/button_disabled" : hovered ? "widget/button_highlighted" : "widget/button";
        gui.blitSprite(ResourceLocation.withDefaultNamespace(sprite), x, y, width, height);
    }
}
