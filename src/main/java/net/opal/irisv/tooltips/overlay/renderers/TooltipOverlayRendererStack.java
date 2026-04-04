package net.opal.irisv.tooltips.overlay.renderers;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.opal.irisv.theme.UiTheme;
import net.opal.irisv.tooltips.overlay.TooltipOverlayRendererUtils;

public class TooltipOverlayRendererStack {

    public static void renderStack(GuiGraphics gui, Font font, ItemStack stack, int x, int y) {
        UiTheme theme = UiTheme.getCurrent();
        if (stack.getCount() <= 1 && !stack.isBarVisible()) return;
        String text = TooltipOverlayRendererUtils.formatCount(stack.getCount());
        int color = stack.getCount() > stack.getMaxStackSize() ? theme.block_countOverstackHex() : theme.block_countNormalHex();
        float baseScale = 0.7f;
        if (text.length() > 3) {
            baseScale = Math.max(0.45f, 0.7f - ((text.length() - 3) * 0.08f));
        }
        gui.pose().pushPose();
        float yOffset = 11f + (0.7f - baseScale) * 5f;
        gui.pose().translate(x + 18, y + yOffset, 200);
        gui.pose().scale(baseScale, baseScale, 1.0f);
        int textWidth = font.width(text);
        gui.drawString(font, text, -textWidth, 0, color, true);
        gui.pose().popPose();
    }
}
