package net.opal.irisv.tooltips.providers.special;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.opal.irisv.Irisv;
import net.opal.irisv.api.IBlockPreviewRenderer;
import net.opal.irisv.api.IFurnaceAccessor;
import net.opal.irisv.network.ClientDataCache;
import net.opal.irisv.option.ConfigOptions;
import net.opal.irisv.tooltips.TooltipData;
import net.opal.irisv.tooltips.overlay.renderers.TooltipOverlayRendererStack;

import javax.annotation.Nullable;
import java.util.List;

public class FurnaceTooltipProvider implements IBlockPreviewRenderer {

    private static final ResourceLocation ARROW_EMPTY = ResourceLocation.fromNamespaceAndPath(Irisv.MODID, "textures/gui/arrow_empty.png");
    private static final ResourceLocation ARROW_FULL = ResourceLocation.fromNamespaceAndPath(Irisv.MODID, "textures/gui/arrow_full.png");

    private static final int ARROW_WIDTH = 24;
    private static final int ARROW_HEIGHT = 17;

    @Override
    public boolean isApplicable(@Nullable BlockState state, @Nullable BlockEntity be) {
        return state != null && state.getBlock() instanceof AbstractFurnaceBlock;
    }

    @Override
    public void render(GuiGraphics gui, Font font, List<ItemStack> items, int x, int y, BlockState state, @Nullable BlockEntity be, TooltipData.BlockInfo info) {
        if (ConfigOptions.getInstance().advancedTooltips) {
            if (be == null || be.getLevel() == null) return;

            CompoundTag data = ClientDataCache.get(be.getBlockPos());
            if (data.isEmpty()) return;

            IFurnaceAccessor furnace = new IFurnaceAccessor(data);

            // --- 1. GESTION DES ITEMS (SLOTS 0, 1, 2) ---
            ItemStack inputStack = ItemStack.EMPTY;
            ItemStack fuelStack = ItemStack.EMPTY;
            ItemStack outputStack = ItemStack.EMPTY;

            if (data.contains("Items", Tag.TAG_LIST)) {
                ListTag list = data.getList("Items", Tag.TAG_COMPOUND);
                for (int i = 0; i < list.size(); i++) {
                    CompoundTag itemTag = list.getCompound(i);
                    int slot = itemTag.getInt("Slot");
                    ItemStack stack = ItemStack.parseOptional(be.getLevel().registryAccess(), itemTag);
                    if (slot == 0) inputStack = stack;
                    else if (slot == 1) fuelStack = stack;
                    else if (slot == 2) outputStack = stack;
                }
            }

            int startX = x + 26;
            int currentY = y + 2;

            renderSlot(gui, font, fuelStack, startX, currentY);
            int inputX = startX + 22;
            renderSlot(gui, font, inputStack, inputX, currentY);

            // --- 2. RENDU DE LA FLÈCHE (CUISSON) ---
            int arrowX = inputX + 22;
            int arrowY = currentY + 1;

            // Fond de la flèche
            gui.blit(ARROW_EMPTY, arrowX, arrowY, 0.0F, 0.0F, ARROW_WIDTH, ARROW_HEIGHT, ARROW_WIDTH, ARROW_HEIGHT);

            float cookProgress = furnace.getCookProgress();
            if (cookProgress > 0) {
                int scaledWidth = (int) (cookProgress * ARROW_WIDTH);
                gui.pose().pushPose();
                gui.pose().translate(0, 0, 0.1F);
                // On applique le décalage Y+1 pour l'alignement visuel
                gui.blit(ARROW_FULL, arrowX, arrowY + 1, 0.0F, 0.0F, Math.min(scaledWidth, ARROW_WIDTH), ARROW_HEIGHT, ARROW_WIDTH, ARROW_HEIGHT);
                gui.pose().popPose();
            }

            int outputX = arrowX + ARROW_WIDTH + 4;
            renderSlot(gui, font, outputStack, outputX, currentY);

            // --- 3. BARRE DE COMBUSTIBLE (FUEL) ---
            int barWidthTotal = (outputX + 16) - startX;
            int barY = currentY + 18 + 2;

            // Bordure / Fond noir
            gui.fill(startX - 1, barY - 1, startX + barWidthTotal + 1, barY + 3, 0xFF000000);

            if (furnace.isBurning()) {
                float fuelRatio = furnace.getBurnProgress();
                int fuelWidth = (int) (fuelRatio * barWidthTotal);

                // Dégradé orange/rouge pour l'effet de feu
                gui.fillGradient(startX, barY, startX + fuelWidth, barY + 2, 0xFFE25822, 0xFFB22222);
                // Petit éclat de lumière sur le dessus
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
