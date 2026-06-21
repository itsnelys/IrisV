package net.opal.irisv.indicators;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.opal.irisv.api.IIndicator;
import net.opal.irisv.indicators.providers.ArmorIndicator;
import net.opal.irisv.indicators.providers.ArrowIndicator;
import net.opal.irisv.option.ConfigOptions;

import java.util.ArrayList;
import java.util.List;

public class IndicatorOverlayRenderer {
    private static final List<IIndicator> INDICATORS = new ArrayList<>();

    static {
        INDICATORS.add(new ArmorIndicator());
        INDICATORS.add(new ArrowIndicator());
    }

    // Remplace 'float partialTick' par 'DeltaTracker deltaTracker'
    public static void render(GuiGraphics gui, net.minecraft.client.DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        ConfigOptions config = ConfigOptions.getInstance();

        if (mc.player == null || mc.options.hideGui || !config.enableIndicators) return;

        // Si tu as besoin du partialTick (le float), tu l'extrais comme ça :
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int totalHeight = INDICATORS.stream()
                .filter(IIndicator::isVisible)
                .mapToInt(IIndicator::getHeight)
                .sum();

        int x = config.indicatorPosition == ConfigOptions.IndicatorPosition.LEFT ? 10 : (screenWidth - 26);
        int currentY = (screenHeight / 2) - (totalHeight / 2);

        gui.pose().pushPose();
        gui.pose().translate(0, 0, 200);

        for (IIndicator indicator : INDICATORS) {
            if (indicator.isVisible()) {
                indicator.render(gui, x, currentY, partialTick);
                currentY += indicator.getHeight() + 6;
            }
        }
        gui.pose().popPose();
    }
}
