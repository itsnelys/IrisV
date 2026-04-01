package net.opal.irisv.tooltips;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.*;
import net.opal.irisv.api.IBlockAccessor;
import net.opal.irisv.option.ConfigOptions;
import net.opal.irisv.theme.UiTheme;
import net.opal.irisv.tooltips.overlay.*;
import net.opal.irisv.tooltips.overlay.TooltipOverlayRendererTools.*;

import java.util.List;

public class TooltipOverlayRenderer {

    public static void render(GuiGraphics gui, Font font, TooltipData.BlockInfo info, IBlockAccessor accessor, float visualProgress, long timeSinceFinish, int screenWidth, int screenHeight) {
        UiTheme theme = UiTheme.getCurrent();
        ConfigOptions config = ConfigOptions.getInstance();
        boolean hasCtrl = Screen.hasControlDown();

        // 1. CALCUL DU LAYOUT
        BlockTooltipLayout layout = BlockTooltipLayout.calculate(font, info, accessor, hasCtrl);

        // --- LOGIQUE DE POSITIONNEMENT DYNAMIQUE ---
        int margin = config.compactMode ? 0 : 10;
        int x;
        int y;

    /* On imagine que config.tooltipPosition fonctionne comme ceci :
       0 = Centre Haut
       1 = Gauche Haut
       2 = Droite Haut
       3 = Gauche Bas
       4 = Droite Bas
    */

        switch (config.tooltipPosition) {
            case 1 -> { // GAUCHE HAUT
                x = margin;
                y = margin;
            }
            case 2 -> { // DROITE HAUT
                x = screenWidth - layout.width() - margin;
                y = margin;
            }
            case 3 -> { // GAUCHE BAS
                x = margin;
                y = screenHeight - layout.height() - margin;
            }
            case 4 -> { // DROITE BAS
                x = screenWidth - layout.width() - margin;
                y = screenHeight - layout.height() - margin;
            }
            default -> { // CENTRE HAUT (Par défaut)
                x = (screenWidth - layout.width()) / 2;
                y = margin;
            }
        }

        // 2. RENDU DU FOND
        // Utilisation de x et y calculés dynamiquement
        gui.fill(x, y, x + layout.width(), y + layout.height(), theme.tooltip_backgroundColor());
        renderBorder(gui, x, y, layout.width(), layout.height());

        // 3. HEADER
        String finalTitle = (accessor != null && accessor.getTitleOverride() != null) ? accessor.getTitleOverride() : info.name();
        ItemStack finalIcon = (accessor != null && !accessor.getIcon().isEmpty()) ? accessor.getIcon() : info.icon();

        gui.renderFakeItem(finalIcon, x + 5, y + 5);
        gui.drawString(font, finalTitle, x + 26, y + 5, theme.tooltip_titleColor(), true);

        // 4. INFOS & PREVIEW (On passe bien x et y)
        int currentY = renderStateInfo(gui, font, info, x, y);
        TooltipOverlayRendererBlock.renderInventoryPreview(gui, font, accessor.getPreviewItems(), hasCtrl, x, currentY);

        // 5. MOD NAME & TOOLS
        TooltipOverlayRendererUtils.renderModName(gui, font, info, x, y, layout.height());

        if (accessor != null && accessor.state() != null) {
            TooltipOverlayRendererTools.renderRequiredTools(gui, font, info, accessor.state(), x, y, layout.nameWidth());

            // La barre de progression s'affiche toujours au bas du tooltip, peu importe sa position
            int barY = y + layout.height() - 1;
            TooltipOverlayRendererUtils.renderProgressBar(gui, accessor.state(), x, barY, layout.width(), visualProgress, timeSinceFinish);
        }
    }

    private record BlockTooltipLayout(int width, int height, int nameWidth, int previewHeight) {
        public static BlockTooltipLayout calculate(Font font, TooltipData.BlockInfo info, IBlockAccessor accessor, boolean hasCtrl) {
            List<ItemStack> previewItems = accessor.getPreviewItems();
            int itemCount = previewItems.size();

            // --- 1. CALCULS DU HEADER ---
            String finalTitle = (accessor.getTitleOverride() != null) ? accessor.getTitleOverride() : info.name();
            int nameWidth = font.width(finalTitle);
            int modWidth = font.width(info.modName());
            int toolsSpace = (accessor.state() != null && !info.requiredTools().isEmpty())
                    ? (info.requiredTools().size() * 14) + 4
                    : 0;

            int baseContentWidth = Math.max(nameWidth + toolsSpace, modWidth);

            // DÉTECTION : Si l'icône est vide, c'est un ItemEntity (via handleItemEntity)
            boolean isItemEntity = info.icon().isEmpty();

            for (String s : info.stateInfo()) {
                int lineWidth = font.width(s);
                if (isItemEntity) {
                    // On applique le coefficient 0.7 à la largeur pour que le fond noir colle au texte
                    baseContentWidth = Math.max(baseContentWidth, (int)(lineWidth * 0.7f));
                } else {
                    baseContentWidth = Math.max(baseContentWidth, lineWidth);
                }
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
            int stateLinesCount = (info.stateInfo() != null) ? info.stateInfo().size() : 0;

            // Ajustement de la hauteur : 7 pixels par ligne pour les items (police 0.7), 10 pour les blocs
            int stateHeight = isItemEntity ? (stateLinesCount * 7) : (stateLinesCount * 10);
            int height = 28 + stateHeight + previewHeight;

            return new BlockTooltipLayout(width, height, nameWidth, previewHeight);
        }
    }

    private static int renderStateInfo(GuiGraphics gui, Font font, TooltipData.BlockInfo info, int x, int y) {
        UiTheme theme = UiTheme.getCurrent();
        int currentY = y + 16;

        // Sécurité : si pas d'infos à afficher, on s'arrête
        if (info.stateInfo() == null || info.stateInfo().isEmpty()) return currentY;

        // Détection : si l'icône est vide (AIR), c'est un item au sol (handleItemEntity)
        boolean isItemEntity = info.icon().isEmpty();

        for (String line : info.stateInfo()) {
            if (isItemEntity) {
                // --- RENDU PETIT & STYLE ITEM (ex: §e§o défini dans le thème) ---
                float scale = 0.7f;
                gui.pose().pushPose();

                // On se place à x + marge, y actuel
                gui.pose().translate(x + 26, currentY, 0);
                gui.pose().scale(scale, scale, scale);

                // On applique le format spécial Item du thème (Jaune + Italique par défaut)
                // Le code de formatage dans la String est prioritaire sur la couleur Hexa
                gui.drawString(font, theme.block_stateItemFormat() + line, 0, 0, theme.block_stateTextColor(), true);

                gui.pose().popPose();

                // Interligne réduit (7 au lieu de 10) car le texte est à 70%
                currentY += 7;
            } else {
                // --- RENDU NORMAL (BLOCS) ---
                // Utilise le format standard (ex: §7)
                gui.drawString(font, theme.block_stateTextFormat() + line, x + 26, currentY, theme.block_stateTextColor(), true);
                currentY += 10;
            }
        }
        return currentY;
    }

    private static void renderBorder(GuiGraphics gui, int x, int y, int width, int height) {
        UiTheme theme = UiTheme.getCurrent();
        int color = theme.tooltip_borderColor();
        gui.fill(x - 1, y - 1, x + width + 1, y, color);
        gui.fill(x - 1, y + height, x + width + 1, y + height + 1, color);
        gui.fill(x - 1, y, x, y + height, color);
        gui.fill(x + width, y, x + width + 1, y + height, color);
    }


}