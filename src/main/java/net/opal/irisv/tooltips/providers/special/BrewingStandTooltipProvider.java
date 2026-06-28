package net.opal.irisv.tooltips.providers.special;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.opal.irisv.Irisv;
import net.opal.irisv.api.IBlockPreviewRenderer;
import net.opal.irisv.network.ClientDataCache;
import net.opal.irisv.option.ConfigOptions;
import net.opal.irisv.tooltips.TooltipData;
import net.opal.irisv.tooltips.overlay.renderers.TooltipOverlayRendererStack;

import javax.annotation.Nullable;
import java.util.List;

public class BrewingStandTooltipProvider implements IBlockPreviewRenderer {

    private static final ResourceLocation ARROW_EMPTY = ResourceLocation.fromNamespaceAndPath(Irisv.MODID, "textures/gui/arrow_empty.png");
    private static final ResourceLocation ARROW_FULL = ResourceLocation.fromNamespaceAndPath(Irisv.MODID, "textures/gui/arrow_full.png");

    private static final int ARROW_WIDTH = 24;
    private static final int ARROW_HEIGHT = 17;

    @Override
    public boolean isApplicable(@Nullable BlockState state, @Nullable BlockEntity be) {
        return state != null && state.getBlock() instanceof BrewingStandBlock;
    }

    @Override
    public void render(GuiGraphics gui, Font font, List<ItemStack> items, int x, int y, BlockState state, @Nullable BlockEntity be, TooltipData.BlockInfo info) {
        if (ConfigOptions.getInstance().advancedTooltips) {
            if (be == null || be.getLevel() == null) return;

            CompoundTag data = ClientDataCache.get(be.getBlockPos());
            if (data.isEmpty()) return;

            // --- 1. RÉCUPÉRATION DES DONNÉES ---
            int fuelLevel = data.getInt("Fuel");
            ItemStack[] slots = new ItemStack[5];
            for (int i = 0; i < 5; i++) slots[i] = ItemStack.EMPTY;

            if (data.contains("Items", Tag.TAG_LIST)) {
                ListTag list = data.getList("Items", Tag.TAG_COMPOUND);
                for (int i = 0; i < list.size(); i++) {
                    CompoundTag itemTag = list.getCompound(i);
                    int slot = itemTag.getInt("Slot");
                    if (slot >= 0 && slot < 5) {
                        slots[slot] = ItemStack.parseOptional(be.getLevel().registryAccess(), itemTag);
                    }
                }
            }

            // --- 2. POSITIONNEMENT ---
            int startX = x + 26;
            int currentY = y + 2;
            int spacing = 18;

            // GAUCHE : Ingrédient et Fuel
            renderSlot(gui, font, slots[3], startX, currentY);
            renderSlot(gui, font, slots[4], startX + spacing, currentY);

            // MILIEU : La Flèche
            int arrowX = startX + (spacing * 2) + 2;
            int arrowY = currentY;

            gui.blit(ARROW_EMPTY, arrowX, arrowY, 0.0F, 0.0F, ARROW_WIDTH, ARROW_HEIGHT, ARROW_WIDTH, ARROW_HEIGHT);

            int brewTime = data.getInt("BrewTime");
            if (brewTime > 0) {
                float progress = 1.0F - ((float) brewTime / 400.0F);
                int scaledWidth = (int) (progress * ARROW_WIDTH);
                if (scaledWidth > 0) {
                    gui.pose().pushPose();
                    gui.pose().translate(0, 0, 0.1F);
                    gui.blit(ARROW_FULL, arrowX, arrowY + 1, 0.0F, 0.0F, scaledWidth, ARROW_HEIGHT, ARROW_WIDTH, ARROW_HEIGHT);
                    gui.pose().popPose();
                }
            }

            // DROITE : Les 3 Potions
            int potionStartX = arrowX + 28;
            for (int i = 0; i < 3; i++) {
                renderSlot(gui, font, slots[i], potionStartX + (i * spacing), currentY);
            }

            // --- 3. RENDU DE LA BARRE DE FUEL HORIZONTALE (EN DESSOUS) ---
            // On calcule la largeur totale : du début de l'ingrédient à la fin de la dernière potion
            int totalWidth = (potionStartX + (2 * spacing) + 16) - startX;
            int barY = currentY + 18 + 2; // 18px de l'item + 2px d'espacement

            // Fond sombre de la barre
            gui.fill(startX - 1, barY - 1, startX + totalWidth + 1, barY + 3, 0xFF000000);

            if (fuelLevel > 0) {
                // Calcul de la largeur selon le fuel (0-20)
                int fuelWidth = (int) (fuelLevel * totalWidth / 20.0f);

                // Dessin de la barre avec dégradé (Orange -> Jaune "Blaze")
                gui.fillGradient(
                        startX, barY,
                        startX + fuelWidth, barY + 2,
                        0xFFFF8C00, // Orange foncé
                        0xFFFED031  // Jaune poudre de blaze
                );

                // Petit reflet de lumière sur le dessus
                gui.fill(startX, barY, startX + fuelWidth, barY + 1, 0x40FFFFFF);
            }
        }
    }

    private void renderSlot(GuiGraphics gui, Font font, ItemStack stack, int x, int y) {
        if (!stack.isEmpty()) {
            gui.renderFakeItem(stack, x, y);
            TooltipOverlayRendererStack.renderStack(gui, font, stack, x, y);
        }
    }
}
