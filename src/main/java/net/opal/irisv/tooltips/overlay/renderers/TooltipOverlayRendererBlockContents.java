package net.opal.irisv.tooltips.overlay.renderers;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.opal.irisv.theme.UiTheme;
import net.opal.irisv.tooltips.overlay.TooltipOverlayRendererUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TooltipOverlayRendererBlockContents {
    public static void RenderBlockContents(GuiGraphics gui, Font font, @NotNull List<ItemStack> items, boolean hasCtrl, int x, int y) {
        UiTheme theme = UiTheme.getCurrent();
        int count = items.size();
        var pose = gui.pose();
        int renderY = y + 2;

        // --- 1. MODE LISTE (1 à 4 items) ---
        if (count <= 4 && !hasCtrl) {
            int maxQteWidth = 0;
            for (ItemStack s : items) {
                String t = TooltipOverlayRendererUtils.formatCount(s.getCount()) + "x";
                maxQteWidth = Math.max(maxQteWidth, (int) (font.width(t) * 0.7f));
            }

            for (ItemStack stack : items) {
                // A. Icône réduite (0.5x)
                pose.pushPose();
                // Ajustement Y : +0.5f pour centrer l'icône dans les 10px de hauteur
                pose.translate(x + 26, renderY + 0.5f, 0);
                pose.scale(0.5f, 0.5f, 1.0f);
                gui.renderFakeItem(stack, 0, 0);
                pose.popPose();

                // B. Zone Quantité (Alignement dynamique à DROITE)
                String countText = TooltipOverlayRendererUtils.formatCount(stack.getCount()) + "x";
                String countColor = stack.getCount() > stack.getMaxStackSize() ? theme.block_countOverstackFormat() : theme.block_countNormalFormat();
                int currentQteWidth = (int) (font.width(countText) * 0.7f);

                pose.pushPose();
                int qteX = x + 38 + (maxQteWidth - currentQteWidth);
                // Ajustement Y : +1.5f pour aligner le texte avec l'icône réduite
                pose.translate(qteX, renderY + 1.5f, 200);
                pose.scale(0.7f, 0.7f, 1.0f);
                gui.drawString(font, countColor + countText, 0, 0, theme.block_listTextColor(), true);
                pose.popPose();

                // C. Nom de l'objet
                pose.pushPose();
                pose.translate(x + 38 + maxQteWidth + 4, renderY + 1.5f, 200);
                pose.scale(0.7f, 0.7f, 1.0f);
                gui.drawString(font, stack.getHoverName(), 0, 0, theme.block_listTextColor(), true);
                pose.popPose();

                renderY += 10; // RÉDUCTION : Passage de 12px à 10px
            }
        }
        // --- 2. MODE GRILLE (5+ ou CTRL) ---
        else {
            // Limitation visuelle : on ne dessine jamais plus de 54 icônes (6 lignes de 9)
            int maxToRender = hasCtrl ? Math.min(count, 54) : Math.min(count, 9);

            int slotX = 0;
            for (int i = 0; i < maxToRender; i++) {
                ItemStack stack = items.get(i);
                int dx = x + 26 + (slotX * 18);
                gui.renderFakeItem(stack, dx, renderY);
                TooltipOverlayRendererStack.renderStack(gui, font, stack, dx, renderY);

                if (++slotX >= 9) {
                    slotX = 0;
                    renderY += 18;
                }
            }

            // Calcul dynamique de la position du texte sous la dernière ligne d'items
            int textY = (slotX == 0) ? renderY : renderY + 18;

            // CAS 1 : Pas de CTRL -> On affiche le surplus par rapport à 9
            if (!hasCtrl && count > 9) {
                pose.pushPose();
                pose.translate(x + 26, textY + 2, 0);
                pose.scale(0.8f, 0.8f, 1.0f);
                gui.drawString(font, theme.block_extraInfoFormat() + "[+ " + (count - 9) + " items... CTRL]", 0, 0, theme.block_extraInfoColor(), true);
                pose.popPose();
            }
            // CAS 2 : Avec CTRL -> On affiche le surplus par rapport à 54
            else if (hasCtrl && count > 54) {
                pose.pushPose();
                pose.translate(x + 26, textY + 2, 0);
                pose.scale(0.8f, 0.8f, 1.0f);
                // Ici count est le nombre total d'items dans la ListTag
                gui.drawString(font, theme.block_countOverstackFormat() + "[+ " + (count - 54) + " encore]", 0, 0, theme.block_extraInfoColor(), true);
                pose.popPose();
            }
        }
    }

}
