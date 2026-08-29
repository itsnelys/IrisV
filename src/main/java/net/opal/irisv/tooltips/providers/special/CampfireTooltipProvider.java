package net.opal.irisv.tooltips.providers.special;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.opal.irisv.api.IBlockPreviewRenderer;
import net.opal.irisv.network.ClientDataCache;
import net.opal.irisv.option.ConfigOptions;
import net.opal.irisv.tooltips.TooltipData;
import net.opal.irisv.tooltips.overlay.renderers.TooltipOverlayRendererStack;

import javax.annotation.Nullable;
import java.util.List;

public class CampfireTooltipProvider implements IBlockPreviewRenderer {

    @Override
    public boolean isApplicable(BlockState state, @Nullable BlockEntity be) {
        return state != null && state.getBlock() instanceof CampfireBlock;
    }

    @Override
    public void render(GuiGraphics gui, Font font, List<ItemStack> items, int x, int y, BlockState state, @Nullable BlockEntity be, TooltipData.BlockInfo info) {
        if (!ConfigOptions.getInstance().advancedTooltips || be == null || be.getLevel() == null) return;

        CompoundTag data = ClientDataCache.get(be.getBlockPos());
        if (data.isEmpty()) return;

        ItemStack[] slots = new ItemStack[4];
        for (int i = 0; i < slots.length; i++) slots[i] = ItemStack.EMPTY;

        if (data.contains("Items", Tag.TAG_LIST)) {
            ListTag list = data.getList("Items", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag itemTag = list.getCompound(i);
                int slot = itemTag.getInt("Slot");
                if (slot >= 0 && slot < slots.length) {
                    ItemStack stack = ItemStack.parseOptional(be.getLevel().registryAccess(), itemTag);
                    if (itemTag.contains("count")) stack.setCount(itemTag.getInt("count"));
                    slots[slot] = stack;
                }
            }
        }

        int[] cookingTimes = data.getIntArray("CookingTimes");
        int[] cookingTotalTimes = data.getIntArray("CookingTotalTimes");

        int startX = x + 26;
        int slotY = y + 2;

        int rendered = 0;
        for (int i = 0; i < slots.length; i++) {
            if (slots[i].isEmpty()) continue;

            int slotX = startX + (rendered * 22);
            renderSlot(gui, font, slots[i], slotX, slotY);
            renderProgressBar(gui, slotX, slotY + 18, getProgress(cookingTimes, cookingTotalTimes, i));
            rendered++;
        }
    }

    private static float getProgress(int[] cookingTimes, int[] cookingTotalTimes, int slot) {
        if (slot >= cookingTimes.length || slot >= cookingTotalTimes.length) return 0.0F;
        int total = cookingTotalTimes[slot];
        if (total <= 0) return 0.0F;
        return Math.min(1.0F, Math.max(0.0F, cookingTimes[slot] / (float) total));
    }

    private static void renderSlot(GuiGraphics gui, Font font, ItemStack stack, int x, int y) {
        if (!stack.isEmpty()) {
            gui.renderFakeItem(stack, x, y);
            TooltipOverlayRendererStack.renderStack(gui, font, stack, x, y);
        }
    }

    private static void renderProgressBar(GuiGraphics gui, int x, int y, float progress) {
        gui.fill(x, y, x + 16, y + 3, 0xFF000000);
        if (progress > 0.0F) {
            int width = Math.max(1, (int) (progress * 16.0F));
            gui.fillGradient(x + 1, y + 1, x + width, y + 2, 0xFFFFB347, 0xFFFF6B35);
        }
    }
}
