package net.opal.irisv.tooltips;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;
import net.opal.irisv.api.IBlockAccessor;
import net.opal.irisv.tooltips.helpers.TooltipColorManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TooltipOverlayRenderer {

    public static void render(GuiGraphics gui, Font font, TooltipData.BlockInfo info, IBlockAccessor accessor, float visualProgress, long timeSinceFinish, int screenWidth) {
        List<ItemStack> previewItems = accessor.getPreviewItems();
        boolean hasCtrl = Screen.hasControlDown();
        int itemCount = previewItems.size();

        // 1. CALCULS DES DIMENSIONS DE BASE
        int nameWidth = font.width(accessor.getTitleOverride() != null ? accessor.getTitleOverride() : info.name());
        int modWidth = font.width(info.modName());
        int toolsCount = info.requiredTools().size();
        int toolsSpace = toolsCount == 0 ? 0 : (toolsCount * 14) + 4;

// Largeur minimale basée sur le texte du haut (Nom, Mod, Stats)
        int baseContentWidth = Math.max(nameWidth + toolsSpace, modWidth);
        for (String s : info.stateInfo()) {
            baseContentWidth = Math.max(baseContentWidth, font.width(s));
        }

// --- 2. CALCUL DYNAMIQUE DE LA PREVIEW ---
        int previewHeight = 0;
        int previewMaxWidth = 0;
        int maxQteWidth = 0; // Largeur de la plus grande quantité (ex: "960x")

        if (!previewItems.isEmpty()) {
            // --- MODE LISTE (1 à 4 items) ---
            if (itemCount <= 4 && !hasCtrl) {
                previewHeight = (itemCount * 10) + 2;

                // Premier passage pour trouver la plus grande largeur de quantité (pour l'alignement à droite)
                for (ItemStack stack : previewItems) {
                    String qteText = formatCount(stack.getCount()) + "x";
                    maxQteWidth = Math.max(maxQteWidth, (int)(font.width(qteText) * 0.7f));
                }

                // Deuxième passage pour la largeur totale de la boîte
                for (ItemStack stack : previewItems) {
                    int nameWidth2 = (int)(font.width(stack.getHoverName().getString()) * 0.7f);
                    // Largeur = Icône(10) + Espace(2) + maxQteWidth + Espace(4) + Nom
                    int rowWidth = 16 + maxQteWidth + nameWidth2;
                    previewMaxWidth = Math.max(previewMaxWidth, rowWidth);
                }
            }
            // --- MODE GRILLE COMPLÈTE ---
            else if (hasCtrl || itemCount <= 9) {
                int cols = Math.min(itemCount, 9);
                int rows = (int) Math.ceil(itemCount / 9.0);
                previewMaxWidth = cols * 18;
                previewHeight = (rows * 18) + 4;
            }
            // --- MODE HYBRIDE ---
            else {
                String surplusText = "[+ " + (itemCount - 9) + " items... CTRL]";
                previewMaxWidth = Math.max(9 * 18, font.width(surplusText));
                previewHeight = 18 + 14 + 4;
            }
        }

// --- 3. DIMENSIONS FINALES ---
// Marge de +6 pour l'effet compact Photo 3
        int width = Math.max(26 + baseContentWidth + 6, 26 + previewMaxWidth + 6);
        int height = 28 + (info.stateInfo().size() * 10) + previewHeight;

        int x = (screenWidth - width) / 2;
        int y = 10;

        // 2. DESSIN DU FOND ET BORDURE
        gui.fill(x, y, x + width, y + height, 0xAA000000);
        renderBorder(gui, x, y, width, height);

// 3. HEADER (Icône + Nom du bloc)
// On définit d'abord le titre final pour l'utiliser dans le rendu ET le calcul de largeur
        String finalTitle = (accessor.getTitleOverride() != null) ? accessor.getTitleOverride() : info.name();
        ItemStack finalIcon = accessor.getIcon().isEmpty() ? info.icon() : accessor.getIcon();

// Rendu de l'icône (soit le bloc, soit l'override du provider)
        gui.renderFakeItem(finalIcon, x + 5, y + 5);

// Rendu du titre (soit le nom du bloc, soit l'override "Player")
        gui.drawString(font, finalTitle, x + 26, y + 5, 0xFFFFFF, true);

        // 4. LIGNES D'INFORMATIONS (§7)
        int currentY = y + 16;
        for (String line : info.stateInfo()) {
            gui.drawString(font, "§7" + line, x + 26, currentY, 0xFFFFFF, true);
            currentY += 10;
        }



        // 5. RENDU DE LA PREVIEW (L'appel procédural)
        renderInventoryPreview(gui, font, previewItems, hasCtrl, x, currentY);



        // 6. NOM DU MOD (Toujours calé en bas via 'height')
        renderModName(gui, font, info, x, y, height);
        renderRequiredTools(gui, font, info, accessor.state(), x, y, nameWidth);
        int barY = y + height - 1;
        // 7. BARRE DE PROGRESSION (Toujours à la dernière ligne)
        renderProgressBar(gui, accessor.state(), x, barY, width, visualProgress, timeSinceFinish);

    }

    private static void renderInventoryPreview(GuiGraphics gui, Font font, @NotNull List<ItemStack> items, boolean hasCtrl, int x, int y) {
        if (items.isEmpty()) return;

        int count = items.size();
        var pose = gui.pose();
        int renderY = y + 2;

// --- 1. MODE LISTE (1 à 4 items) ---
        if (count <= 4 && !hasCtrl) {
            int maxQteWidth = 0;
            for (ItemStack s : items) {
                String t = formatCount(s.getCount()) + "x";
                maxQteWidth = Math.max(maxQteWidth, (int)(font.width(t) * 0.7f));
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
                String countText = formatCount(stack.getCount()) + "x";
                String countColor = stack.getCount() > stack.getMaxStackSize() ? "§6" : "§7";
                int currentQteWidth = (int)(font.width(countText) * 0.7f);

                pose.pushPose();
                int qteX = x + 38 + (maxQteWidth - currentQteWidth);
                // Ajustement Y : +1.5f pour aligner le texte avec l'icône réduite
                pose.translate(qteX, renderY + 1.5f, 200);
                pose.scale(0.7f, 0.7f, 1.0f);
                gui.drawString(font, countColor + countText, 0, 0, 0xAAAAAA, true);
                pose.popPose();

                // C. Nom de l'objet
                pose.pushPose();
                pose.translate(x + 38 + maxQteWidth + 4, renderY + 1.5f, 200);
                pose.scale(0.7f, 0.7f, 1.0f);
                gui.drawString(font, stack.getHoverName(), 0, 0, 0xAAAAAA, true);
                pose.popPose();

                renderY += 10; // RÉDUCTION : Passage de 12px à 10px
            }
        }
        // --- 2. MODE GRILLE (5+ ou CTRL) ---
        else {
            int maxToShow = hasCtrl ? count : Math.min(count, 9);
            int slotX = 0;
            for (int i = 0; i < maxToShow; i++) {
                ItemStack stack = items.get(i);
                int dx = x + 26 + (slotX * 18);
                gui.renderFakeItem(stack, dx, renderY);
                renderCustomItemDecorations(gui, font, stack, dx, renderY);
                if (++slotX >= 9) { slotX = 0; renderY += 18; }
            }

            if (!hasCtrl && count > 9) {
                int textY = (slotX == 0) ? renderY : renderY + 18;
                pose.pushPose();
                pose.translate(x + 26, textY + 2, 0);
                pose.scale(0.8f, 0.8f, 1.0f);
                gui.drawString(font, "§8[+ " + (count - 9) + " items... CTRL]", 0, 0, 0xFFFFFF, true);
                pose.popPose();
            }
        }
    }

    private static void renderCustomItemDecorations(GuiGraphics gui, Font font, ItemStack stack, int x, int y) {
        if (stack.getCount() <= 1 && !stack.isBarVisible()) return;

        String text = formatCount(stack.getCount());
        int color = stack.getCount() > stack.getMaxStackSize() ? 0xFFAA00 : 0xFFFFFF;

        gui.pose().pushPose();
        gui.pose().translate(x + 18, y + 11, 200);
        gui.pose().scale(0.7f, 0.7f, 1.0f); // Taille réduite
        int textWidth = font.width(text);
        gui.drawString(font, text, -textWidth, 0, color, true);
        gui.pose().popPose();
    }

    private static String formatCount(int count) {
        if (count < 10000) return String.valueOf(count);
        if (count < 1000000) return String.format("%.1fk", count / 10000.0);
        return String.format("%.1fM", count / 1000000.0);
    }

    private static void renderModName(GuiGraphics gui, Font font, TooltipData.BlockInfo info, int x, int y, int height) {
        int modColor = TooltipColorManager.getModColor(info.modId());
        var modComponent = Component.literal(info.modName()).withStyle(s -> s.withColor(modColor).withItalic(true));
        // Se place exactement à 11 pixels du bas de la boîte
        gui.drawString(font, modComponent, x + 26, y + height - 11, 0xFFFFFF, true);
    }

    private static void renderRequiredTools(GuiGraphics gui, Font font, TooltipData.BlockInfo info, BlockState state, int x, int y, int nameWidth) {
        if (!info.requiredTools().isEmpty()) {
            ItemStack held = Minecraft.getInstance().player.getMainHandItem();
            boolean isCorrectToolHeld = isAnyCorrectToolHeld(info.requiredTools(), held);

            int toolX = x + 25 + nameWidth + 4;
            for (ItemStack tool : info.requiredTools()) {
                renderTool(gui, font, tool, state, held, isCorrectToolHeld, toolX, y + 4);
                toolX += 14;
            }
        }
    }

    private static void renderProgressBar(GuiGraphics gui, BlockState state, int x, int barY, int width, float visualProgress, long timeSinceFinish) {
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

    private static void renderTool(GuiGraphics gui, Font font, ItemStack toolIcon, BlockState state, ItemStack held, boolean alreadyHoldingCorrect, int tx, int ty) {
        String status = "§c✘"; // Par défaut : Rouge
        boolean isCreative = Minecraft.getInstance().player.isCreative();

        // On ne calcule le statut de l'outil QUE si on n'a pas une icône d'override (comme un livre)
        // car on ne "mine" pas un livre avec une pioche.
        boolean isBlockIcon = toolIcon.getItem() == state.getBlock().asItem();

        if (isCreative) {
            status = "§a✔";
        } else if (isBlockIcon) {
            // Logique de minage standard
            boolean isCorrectType = isSameToolType(toolIcon.getItem(), held.getItem());
            boolean isShearsRequired = toolIcon.getItem() instanceof ShearsItem;

            if (isShearsRequired) {
                status = (held.getItem() instanceof ShearsItem) ? "§a✔" : "§c✘";
            } else {
                boolean canDropWithHeld = Minecraft.getInstance().player.hasCorrectToolForDrops(state);
                boolean holdingAnyTool = isTieredTool(held.getItem());

                if (isCorrectType) {
                    status = canDropWithHeld ? "§a✔" : "§c✘";
                } else if (canDropWithHeld && !holdingAnyTool) {
                    status = "§e!";
                }
            }
        } else {
            // Si c'est un item d'override (Livre, Disque, etc.), on n'affiche pas de X ou de V
            status = "";
        }

        // --- RENDU (Inchangé) ---
        gui.pose().pushPose();
        gui.pose().translate(tx, ty, 0);
        gui.pose().scale(0.75f, 0.75f, 0.75f);
        gui.renderItem(toolIcon, 0, 0);
        gui.pose().popPose();

        if (toolIcon.getItem() != Items.BARRIER) {
            gui.pose().pushPose();
            if (status.equals("§e!")) {
                float scale = 0.8f;
                gui.pose().translate(tx + 6 - (font.width(status) * scale / 2), ty + 3, 200);
                gui.pose().scale(scale, scale, scale);
            } else {
                gui.pose().translate(tx + 7, ty + 7, 200);
                gui.pose().scale(0.6f, 0.6f, 0.6f);
            }
            gui.drawString(font, status, 0, 0, 0xFFFFFF, true);
            gui.pose().popPose();
        }
    }

    private static boolean isAnyCorrectToolHeld(List<ItemStack> requiredTools, ItemStack held) {
        if (held.isEmpty()) return false;
        for (ItemStack tool : requiredTools) if (isSameToolType(tool.getItem(), held.getItem())) return true;
        return false;
    }
    private static boolean isTieredTool(Item item) { return item instanceof DiggerItem; }

    private static boolean isSameToolType(Item icon, Item held) {
        if (held instanceof AirItem) return false;

        // On récupère l'instance sous forme de stack pour accéder aux tags d'items
        ItemStack heldStack = held.getDefaultInstance();

        // Vérification par TAGS d'items (Standard Minecraft & Forge/NeoForge)
        // Cela permet de reconnaître une pioche en Ruby ou un Hammer comme une "pioche"
        if (heldStack.is(ItemTags.PICKAXES) && icon instanceof PickaxeItem) return true;
        if (heldStack.is(ItemTags.SHOVELS) && icon instanceof ShovelItem) return true;
        if (heldStack.is(ItemTags.AXES) && icon instanceof AxeItem) return true;
        if (heldStack.is(ItemTags.HOES) && icon instanceof HoeItem) return true;

        // Cas particuliers sans tags globaux évidents
        if (held instanceof ShearsItem && icon instanceof ShearsItem) return true;
        if (held instanceof SwordItem && icon instanceof SwordItem) return true;

        // Si tu as un tag spécifique pour les Hammers dans ton pack :
        // if (heldStack.is(MyTags.HAMMERS) && icon instanceof PickaxeItem) return true;

        return false;
    }

    private static void renderBorder(GuiGraphics gui, int x, int y, int width, int height) {
        int color = 0xCC333333;
        gui.fill(x - 1, y - 1, x + width + 1, y, color);
        gui.fill(x - 1, y + height, x + width + 1, y + height + 1, color);
        gui.fill(x - 1, y, x, y + height, color);
        gui.fill(x + width, y, x + width + 1, y + height, color);
    }
}