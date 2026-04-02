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

            // DÉTECTION : Si l'icône est vide, c'est un ItemEntity
            boolean isItemEntity = info.icon().isEmpty();

            // --- 2. CALCUL DYNAMIQUE DU STATE INFO (Texte ou Cœurs) ---
            int stateHeight = 0;
            if (info.stateInfo() != null) {
                for (String s : info.stateInfo()) {
                    // CAS A : La ligne est un rendu de cœurs
                    if (s.startsWith("hp_render:")) {
                        try {
                            String[] values = s.replace("hp_render:", "").split("/");
                            float maxHealth = Float.parseFloat(values[1]);

                            // Largeur fixe pour 10 cœurs (9px * 10 = 90px)
                            baseContentWidth = Math.max(baseContentWidth, 90);

                            // Hauteur : 12px pour une ligne, 20px pour deux lignes (si > 20 HP)
                            stateHeight += (maxHealth > 20) ? 20 : 12;
                        } catch (Exception e) {
                            stateHeight += 10;
                        }
                    }
                    // CAS B : La ligne est du texte classique
                    else {
                        int lineWidth = font.width(s);
                        if (isItemEntity) {
                            baseContentWidth = Math.max(baseContentWidth, (int)(lineWidth * 0.7f));
                            stateHeight += 7;
                        } else {
                            baseContentWidth = Math.max(baseContentWidth, lineWidth);
                            stateHeight += 10;
                        }
                    }
                }
            }

            // --- 3. CALCUL DYNAMIQUE DE LA PREVIEW D'INVENTAIRE ---
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

            // --- 4. DIMENSIONS FINALES ---
            // 26px (icône + marge) + largeur contenu + 6px marge droite
            int width = Math.max(26 + baseContentWidth + 6, 26 + previewMaxWidth + 6);
            // 28px = marge header (16) + marge modName/marge basse (12)
            int height = 28 + stateHeight + previewHeight;

            return new BlockTooltipLayout(width, height, nameWidth, previewHeight);
        }
    }

    private static int renderStateInfo(GuiGraphics gui, Font font, TooltipData.BlockInfo info, int x, int y) {
        UiTheme theme = UiTheme.getCurrent();
        int currentY = y + 16;

        // Sécurité : si pas d'infos à afficher, on s'arrête
        if (info.stateInfo() == null || info.stateInfo().isEmpty()) return currentY;

        // Détection : si l'icône est vide (AIR), c'est un item au sol
        boolean isItemEntity = info.icon().isEmpty();

        for (String line : info.stateInfo()) {

            // --- 1. DÉTECTION ET RENDU DES CŒURS (JOUEURS & MOBS) ---
            if (line.startsWith("hp_render:")) {
                try {
                    // On extrait les valeurs (ex: hp_render:15.5/20.0)
                    String[] values = line.replace("hp_render:", "").split("/");
                    float health = Float.parseFloat(values[0]);
                    float maxHealth = Float.parseFloat(values[1]);

                    // Appel de l'utilitaire (x + 26 pour aligner avec le texte)
                    TooltipOverlayRendererUtils.renderHearts(gui, x + 26, currentY, health, maxHealth);

                    // On calcule le décalage vertical : 9px par ligne de cœurs + petite marge
                    // Si maxHealth > 20, il y a 2 lignes de cœurs (10 par ligne)
                    currentY += (maxHealth > 20) ? 20 : 12;

                    continue; // On passe à la ligne suivante, on ne dessine pas le texte "hp_render"
                } catch (Exception e) {
                    // En cas d'erreur de parsing, on laisse tomber et on continue
                }
            }

            // --- 2. RENDU DU TEXTE CLASSIQUE ---
            if (isItemEntity) {
                // --- STYLE ITEM (Petit & Italique) ---
                float scale = 0.7f;
                gui.pose().pushPose();
                gui.pose().translate(x + 26, currentY, 0);
                gui.pose().scale(scale, scale, scale);

                gui.drawString(font, theme.block_stateItemFormat() + line, 0, 0, theme.block_stateTextColor(), true);

                gui.pose().popPose();
                currentY += 7;
            } else {
                // --- STYLE NORMAL (Blocs & Mobs) ---
                gui.drawString(font, theme.block_stateTextFormat() + line, x + 26, currentY, theme.block_stateTextColor(), true);
                currentY += 10;
            }
        }
        return currentY;
    }

    // Dans TooltipOverlayRenderer.java
    private static int renderStateInfo(GuiGraphics gui, Font font, TooltipData.BlockInfo info, UiTheme theme, int x, int y) {
        int currentY = y;
        for (String line : info.stateInfo()) {

            // --- NOUVEAU : DÉTECTION DES CŒURS ---
            if (line.startsWith("hp_render:")) {
                try {
                    String[] values = line.replace("hp_render:", "").split("/");
                    float health = Float.parseFloat(values[0]);
                    float maxHealth = Float.parseFloat(values[1]);

                    // On dessine les cœurs
                    TooltipOverlayRendererUtils.renderHearts(gui, x + 26, currentY, health, maxHealth);

                    // On ajuste l'espacement vertical (9px par ligne de cœurs)
                    currentY += (maxHealth > 20) ? 20 : 12;
                    continue; // On passe à la ligne suivante, on ne dessine pas le texte "hp_render"
                } catch (Exception e) {
                    // En cas d'erreur de parsing, on ignore
                }
            }

            // --- RENDU DU TEXTE NORMAL (ce que tu avais déjà) ---
            if (line.startsWith("item:")) {
                // ... ton code pour les items
            } else {
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