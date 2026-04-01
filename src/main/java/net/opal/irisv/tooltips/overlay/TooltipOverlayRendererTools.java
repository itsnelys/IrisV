package net.opal.irisv.tooltips.overlay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;
import net.opal.irisv.theme.UiTheme;
import net.opal.irisv.tooltips.TooltipData;

import java.util.List;

public class TooltipOverlayRendererTools {
    private static void renderTool(GuiGraphics gui, Font font, ItemStack toolIcon, BlockState state, ItemStack held, UiTheme theme, boolean alreadyHoldingCorrect, int tx, int ty) {        String status = "";
        boolean isCreative = Minecraft.getInstance().player.isCreative();

        // La correction est ici : on vérifie si l'icône est un outil de minage (DiggerItem)
        // ou des cisailles. Si c'est un Livre ou un Disque, ça ne rentrera pas dedans.
        boolean isMiningTool = toolIcon.getItem() instanceof DiggerItem || toolIcon.getItem() instanceof ShearsItem;

        if (isCreative) {
            // En créatif, on affiche le ✔ seulement si c'est un outil de minage
            if (isMiningTool) status = theme.status_ok();
        } else if (isMiningTool) {
            // Logique de minage standard (Survie)
            boolean isCorrectType = isSameToolType(toolIcon.getItem(), held.getItem());
            boolean isShearsRequired = toolIcon.getItem() instanceof ShearsItem;

            if (isShearsRequired) {
                status = (held.getItem() instanceof ShearsItem) ? theme.status_ok() : theme.status_error();
            } else {
                boolean canDropWithHeld = Minecraft.getInstance().player.hasCorrectToolForDrops(state);
                boolean holdingAnyTool = isTieredTool(held.getItem());

                if (isCorrectType) {
                    status = canDropWithHeld ? theme.status_ok() : theme.status_error();
                } else if (canDropWithHeld && !holdingAnyTool) {
                    status = theme.status_warning();
                } else {
                    status = theme.status_error();
                }
            }
        }

        // --- RENDU ---
        gui.pose().pushPose();
        gui.pose().translate(tx, ty, 0);
        gui.pose().scale(0.75f, 0.75f, 0.75f);
        gui.renderItem(toolIcon, 0, 0);
        gui.pose().popPose();

        if (!status.isEmpty() && toolIcon.getItem() != Items.BARRIER) {
            gui.pose().pushPose();
            if (status.equals(theme.status_warning())) {
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

    public static boolean isAnyCorrectToolHeld(List<ItemStack> requiredTools, ItemStack held) {
        if (held.isEmpty()) return false;
        for (ItemStack tool : requiredTools) if (isSameToolType(tool.getItem(), held.getItem())) return true;
        return false;
    }
    public static boolean isTieredTool(Item item) { return item instanceof DiggerItem; }

    public static boolean isSameToolType(Item icon, Item held) {
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

    public static void renderRequiredTools(GuiGraphics gui, Font font, TooltipData.BlockInfo info, BlockState state, int x, int y, int nameWidth) {
        if (!info.requiredTools().isEmpty()) {
            UiTheme theme = UiTheme.getCurrent();

            ItemStack held = Minecraft.getInstance().player.getMainHandItem();
            boolean isCorrectToolHeld = isAnyCorrectToolHeld(info.requiredTools(), held);

            int toolX = x + 25 + nameWidth + 4;
            for (ItemStack tool : info.requiredTools()) {
                renderTool(gui, font, tool, state, held, theme, isCorrectToolHeld, toolX, y + 4);
                toolX += 14;
            }
        }
    }
}
