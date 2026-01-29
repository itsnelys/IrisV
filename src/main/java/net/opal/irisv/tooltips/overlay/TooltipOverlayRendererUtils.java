package net.opal.irisv.tooltips.overlay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.level.block.state.BlockState;
import net.opal.irisv.tooltips.TooltipData;
import net.opal.irisv.tooltips.helpers.TooltipColorManager;

public class TooltipOverlayRendererUtils {
    public static String formatCount(int count) {
        if (count < 10000) return String.valueOf(count);
        if (count < 1000000) {
            double k = count / 1000.0;
            // Si c'est un nombre pile (ex: 10k au lieu de 10.0k), on enlève la virgule
            return k == (int)k ? (int)k + "k" : String.format("%.1fk", k);
        }
        double m = count / 1000000.0;
        return m == (int)m ? (int)m + "M" : String.format("%.1fM", m);
    }


    public static void renderModName(GuiGraphics gui, Font font, TooltipData.BlockInfo info, int x, int y, int height) {
        int modColor = TooltipColorManager.getModColor(info.modId());
        var modComponent = Component.literal(info.modName()).withStyle(s -> s.withColor(modColor).withItalic(true));
        // Se place exactement à 11 pixels du bas de la boîte
        gui.drawString(font, modComponent, x + 26, y + height - 11, 0xFFFFFF, true);
    }

    public static void renderProgressBar(GuiGraphics gui, BlockState state, int x, int barY, int width, float visualProgress, long timeSinceFinish) {
        boolean isAnimatingFinish = timeSinceFinish < 400;
        if (visualProgress > 0 || isAnimatingFinish) {
            int barColor;

            if (isAnimatingFinish) {
                float alpha = 1.0f - (timeSinceFinish / 400f);
                int alphaInt = (int)(alpha * 255);
                barColor = (alphaInt << 24) | 0xFFFFFF;
            } else {
                var player = Minecraft.getInstance().player;
                if (player != null && !player.isCreative()) {
                    boolean canDrop = player.hasCorrectToolForDrops(state);
                    boolean isTool = player.getMainHandItem().getItem() instanceof DiggerItem || player.getMainHandItem().getItem() instanceof ShearsItem;

                    if (!canDrop) barColor = 0xFFFF4545; // Rouge
                    else if (!isTool) barColor = 0xFFFFD700; // Jaune
                    else barColor = 0xFF50C878; // Vert
                } else {
                    barColor = 0xFF708090; // Gris
                }
            }

            // Fond sombre de la barre
            gui.fill(x, barY, x + width, barY + 1, 0xFF1A1A1A);

            var pose = gui.pose();
            pose.pushPose();
            pose.translate(x, barY, 0);

            if (isAnimatingFinish) {
                gui.fill(0, 0, width, 1, barColor);
            } else {
                // Utilise le scale pour la progression visuelle
                pose.scale(visualProgress, 1.0f, 1.0f);
                gui.fill(0, 0, width, 1, barColor);
            }
            pose.popPose();
        }
    }
}
