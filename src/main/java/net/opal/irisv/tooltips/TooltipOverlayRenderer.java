package net.opal.irisv.tooltips;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;
import net.opal.irisv.tooltips.helpers.TooltipColorManager;

import java.util.List;

public class TooltipOverlayRenderer {

    public static void render(GuiGraphics gui, Font font, TooltipDataCollector.BlockInfo info, BlockState state, float visualProgress, long timeSinceFinish, int screenWidth) {
        int nameWidth = font.width(info.name());
        int modWidth = font.width(info.modName());

        int toolsSpace = info.requiredTools().isEmpty() ? 0 : (info.requiredTools().size() * 14) + 2;
        int contentWidth = Math.max(nameWidth + toolsSpace, modWidth);

        // Calcul pour ajuster la largeur si un texte d'état est plus long
        for(String s : info.stateInfo()) contentWidth = Math.max(contentWidth, font.width(s));

        int width = 5 + 16 + 6 + contentWidth + 6;

        // MODIFICATION : Hauteur dynamique
        int height = 28 + (info.stateInfo().size() * 10);

        int x = (screenWidth - width) / 2;
        int y = 10;

        gui.fill(x, y, x + width, y + height, 0xAA000000);
        renderBorder(gui, x, y, width, height);

        gui.renderItem(info.icon(), x + 4, y + 6);
        gui.drawString(font, info.name(), x + 25, y + 5, 0xFFFFFF, true);

        // AJOUT : Rendu des états
        int currentY = y + 16;
        for (String line : info.stateInfo()) {
            gui.drawString(font, "§7" + line, x + 25, currentY, 0xFFFFFF, true);
            currentY += 10;
        }

        // Le nom du mod se dessine après les états
        int modColor = TooltipColorManager.getModColor(info.modId());
        var modComponent = Component.literal(info.modName())
                .withStyle(style -> style.withColor(modColor).withItalic(true));
        gui.drawString(font, modComponent, x + 25, currentY, 0xFFFFFF, true);

        if (!info.requiredTools().isEmpty()) {
            ItemStack held = Minecraft.getInstance().player.getMainHandItem();
            boolean alreadyHoldingCorrect = isAnyCorrectToolHeld(info.requiredTools(), held);
            for (int i = 0; i < info.requiredTools().size(); i++) {
                renderTool(gui, font, info.requiredTools().get(i), state, held, alreadyHoldingCorrect, x + 25 + nameWidth + 4 + (i * 14), y + 4);
            }
        }

// --- RENDU DE LA PROGRESS BAR ---
        boolean isAnimatingFinish = timeSinceFinish < 400;
        if (visualProgress > 0 || isAnimatingFinish) {
            int barY = y + height - 1;
            int barColor;

            if (isAnimatingFinish) {
                // Effet de Flash Blanc avec fondu (Fade Out)
                float alpha = 1.0f - (timeSinceFinish / 400f);
                int alphaInt = (int)(alpha * 255);
                barColor = (alphaInt << 24) | 0xFFFFFF;
            } else {
                // Couleurs de la barre
                var player = Minecraft.getInstance().player;
                if (player != null && !player.isCreative()) {
                    boolean canDrop = player.hasCorrectToolForDrops(state);
                    boolean isTool = player.getMainHandItem().getItem() instanceof DiggerItem || player.getMainHandItem().getItem() instanceof ShearsItem;

                    if (!canDrop) barColor = 0xFFFF4545;      // Rouge : Pas de loot
                    else if (!isTool) barColor = 0xFFFFD700; // Jaune : Pas opti
                    else barColor = 0xFF50C878;             // Vert : Parfait
                } else {
                    barColor = 0xFF708090; // Créatif ou défaut
                }
            }

            // Fond gris de la barre
            gui.fill(x, barY, x + width, barY + 1, 0xFF1A1A1A);

            var pose = gui.pose();
            pose.pushPose();
            pose.translate(x, barY, 0);
            if (isAnimatingFinish) {
                gui.fill(0, 0, width, 1, barColor);
            } else {
                pose.scale(visualProgress, 1.0f, 1.0f);
                gui.fill(0, 0, width, 1, barColor);
            }
            pose.popPose();
        }
    }

    private static void renderTool(GuiGraphics gui, Font font, ItemStack toolIcon, BlockState state, ItemStack held, boolean alreadyHoldingCorrect, int tx, int ty) {
        String status = "§c✘"; // Par défaut : Rouge
        boolean isCreative = Minecraft.getInstance().player.isCreative();
        boolean isCorrectType = isSameToolType(toolIcon.getItem(), held.getItem());

        // Vérification spécifique pour les cisailles (Shears)
        boolean isShearsRequired = toolIcon.getItem() instanceof ShearsItem;

        if (isCreative) {
            status = "§a✔";
        } else if (isShearsRequired) {
            // Pour les cisailles : Vert si tenu, sinon rouge (pas de "!")
            status = (held.getItem() instanceof ShearsItem) ? "§a✔" : "§c✘";
        } else {
            // Logique standard pour les autres outils (Pioches, Haches, etc.)
            boolean canDropWithHeld = Minecraft.getInstance().player.hasCorrectToolForDrops(state);
            boolean holdingAnyTool = isTieredTool(held.getItem());

            if (isCorrectType) {
                status = canDropWithHeld ? "§a✔" : "§c✘";
            } else if (canDropWithHeld && !holdingAnyTool) {
                status = "§e!";
            }
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
        return (icon instanceof PickaxeItem && held instanceof PickaxeItem) ||
                (icon instanceof ShovelItem && held instanceof ShovelItem) ||
                (icon instanceof AxeItem && held instanceof AxeItem) ||
                (icon instanceof HoeItem && held instanceof HoeItem) ||
                (icon instanceof SwordItem && held instanceof SwordItem) ||
                (icon instanceof ShearsItem && held instanceof ShearsItem);
    }

    private static void renderBorder(GuiGraphics gui, int x, int y, int width, int height) {
        int color = 0xCC333333;
        gui.fill(x - 1, y - 1, x + width + 1, y, color);
        gui.fill(x - 1, y + height, x + width + 1, y + height + 1, color);
        gui.fill(x - 1, y, x, y + height, color);
        gui.fill(x + width, y, x + width + 1, y + height, color);
    }
}