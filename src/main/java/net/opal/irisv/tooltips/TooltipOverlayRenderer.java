package net.opal.irisv.tooltips;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.*;
import net.opal.irisv.api.IBlockAccessor;
import net.opal.irisv.tooltips.overlay.*;
import net.opal.irisv.tooltips.overlay.TooltipOverlayRendererTools.*;

import java.util.List;

public class TooltipOverlayRenderer {

    public static void render(GuiGraphics gui, Font font, TooltipData.BlockInfo info, IBlockAccessor accessor, float visualProgress, long timeSinceFinish, int screenWidth) {
        boolean hasCtrl = Screen.hasControlDown();

        // 1. APPEL AU CALCULATEUR
        BlockTooltipLayout layout = BlockTooltipLayout.calculate(font, info, accessor, hasCtrl);

        int x = (screenWidth - layout.width()) / 2;
        int y = 10;

        // 2. DESSIN DU FOND ET BORDURE
        gui.fill(x, y, x + layout.width(), y + layout.height(), 0xAA000000);
        renderBorder(gui, x, y, layout.width(), layout.height());

        // 3. HEADER (Icône + Titre)
        String finalTitle = (accessor.getTitleOverride() != null) ? accessor.getTitleOverride() : info.name();
        ItemStack finalIcon = accessor.getIcon().isEmpty() ? info.icon() : accessor.getIcon();

        gui.renderFakeItem(finalIcon, x + 5, y + 5);
        gui.drawString(font, finalTitle, x + 26, y + 5, 0xFFFFFF, true);

        // 4. LIGNES D'INFORMATIONS
        int currentY = renderStateInfo(gui, font, info, x, y);

        // 5. RENDU DE LA PREVIEW (Appel à ta classe Block)
        TooltipOverlayRendererBlock.renderInventoryPreview(gui, font, accessor.getPreviewItems(), hasCtrl, x, currentY);

        // 6. PIED DE PAGE (Mod, Outils, Barre)
        TooltipOverlayRendererUtils.renderModName(gui, font, info, x, y, layout.height());
        TooltipOverlayRendererTools.renderRequiredTools(gui, font, info, accessor.state(), x, y, layout.nameWidth());

        int barY = y + layout.height() - 1;
        TooltipOverlayRendererUtils.renderProgressBar(gui, accessor.state(), x, barY, layout.width(), visualProgress, timeSinceFinish);
    }

    private record BlockTooltipLayout(int width, int height, int nameWidth, int previewHeight) {
        public static BlockTooltipLayout calculate(Font font, TooltipData.BlockInfo info, IBlockAccessor accessor, boolean hasCtrl) {
            List<ItemStack> previewItems = accessor.getPreviewItems();
            int itemCount = previewItems.size();

            // --- 1. CALCULS DU HEADER ---
            String finalTitle = (accessor.getTitleOverride() != null) ? accessor.getTitleOverride() : info.name();
            int nameWidth = font.width(finalTitle);
            int modWidth = font.width(info.modName());
            int toolsSpace = info.requiredTools().isEmpty() ? 0 : (info.requiredTools().size() * 14) + 4;

            int baseContentWidth = Math.max(nameWidth + toolsSpace, modWidth);
            for (String s : info.stateInfo()) {
                baseContentWidth = Math.max(baseContentWidth, font.width(s));
            }

            // --- 2. CALCUL DYNAMIQUE DE LA PREVIEW ---
            int previewHeight = 0;
            int previewMaxWidth = 0;

            if (!previewItems.isEmpty()) {
                if (itemCount <= 4 && !hasCtrl) {
                    previewHeight = (itemCount * 10) + 2;
                    int maxQteWidth = 0;
                    for (ItemStack stack : previewItems) {
                        String qteText = TooltipOverlayRendererUtils.formatCount(stack.getCount()) + "x";
                        maxQteWidth = Math.max(maxQteWidth, (int) (font.width(qteText) * 0.7f));
                    }
                    for (ItemStack stack : previewItems) {
                        int itemNameWidth = (int) (font.width(stack.getHoverName().getString()) * 0.7f);
                        previewMaxWidth = Math.max(previewMaxWidth, 16 + maxQteWidth + itemNameWidth);
                    }
                } else if (hasCtrl || itemCount <= 9) {
                    int maxIcons = hasCtrl ? Math.min(itemCount, 54) : itemCount;
                    int cols = Math.min(maxIcons, 9);
                    int rows = (maxIcons + 8) / 9;
                    previewMaxWidth = cols * 18;
                    previewHeight = rows * 18 + 4;
                    if (hasCtrl && itemCount > 54) previewHeight += 12;
                } else {
                    String surplusText = "[+ " + (itemCount - 9) + " items... CTRL]";
                    previewMaxWidth = Math.max(9 * 18, (int) (font.width(surplusText) * 0.8f));
                    previewHeight = 18 + 12 + 4;
                }
            }

            // --- 3. DIMENSIONS FINALES ---
            int width = Math.max(26 + baseContentWidth + 6, 26 + previewMaxWidth + 6);
            int height = 28 + (info.stateInfo().size() * 10) + previewHeight;

            return new BlockTooltipLayout(width, height, nameWidth, previewHeight);
        }
    }

    private static int renderStateInfo(GuiGraphics gui, Font font, TooltipData.BlockInfo info, int x, int y) {
        int currentY = y + 16;
        for (String line : info.stateInfo()) {
            gui.drawString(font, "§7" + line, x + 26, currentY, 0xFFFFFF, true);
            currentY += 10;
        }
        return currentY;
    }

    private static void renderBorder(GuiGraphics gui, int x, int y, int width, int height) {
        int color = 0xCC333333;
        gui.fill(x - 1, y - 1, x + width + 1, y, color);
        gui.fill(x - 1, y + height, x + width + 1, y + height + 1, color);
        gui.fill(x - 1, y, x, y + height, color);
        gui.fill(x + width, y, x + width + 1, y + height, color);
    }
}