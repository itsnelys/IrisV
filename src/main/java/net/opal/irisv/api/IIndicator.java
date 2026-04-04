package net.opal.irisv.api;

import net.minecraft.client.gui.GuiGraphics;

public interface IIndicator {
    boolean isVisible();
    void render(GuiGraphics gui, int x, int y, float partialTick);
    int getHeight(); // Pour empiler les indicateurs proprement
    int getWidth();
}