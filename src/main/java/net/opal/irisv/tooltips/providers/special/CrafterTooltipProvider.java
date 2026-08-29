package net.opal.irisv.tooltips.providers.special;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.opal.irisv.api.IBlockPreviewRenderer;
import net.opal.irisv.network.ClientDataCache;
import net.opal.irisv.option.ConfigOptions;
import net.opal.irisv.tooltips.TooltipData;
import net.opal.irisv.tooltips.overlay.renderers.TooltipOverlayRendererStack;

import javax.annotation.Nullable;
import java.util.List;

public class CrafterTooltipProvider implements IBlockPreviewRenderer {

    @Override
    public boolean isApplicable(BlockState state, @Nullable BlockEntity be) {
        return state != null && state.getBlock() instanceof CrafterBlock;
    }

    @Override
    public void render(GuiGraphics gui, Font font, List<ItemStack> items, int x, int y, BlockState state, @Nullable BlockEntity be, TooltipData.BlockInfo info) {
        if (!ConfigOptions.getInstance().advancedTooltips || be == null || be.getLevel() == null) return;

        CompoundTag data = ClientDataCache.get(be.getBlockPos());
        if (data.isEmpty() || !data.contains("Items", Tag.TAG_LIST)) return;

        ItemStack[] slots = new ItemStack[9];
        for (int i = 0; i < slots.length; i++) slots[i] = ItemStack.EMPTY;

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

        int startX = x + 26;
        int startY = y + 2;

        for (int slot = 0; slot < slots.length; slot++) {
            ItemStack stack = slots[slot];
            if (stack.isEmpty()) continue;

            int slotX = startX + (slot % 3) * 18;
            int slotY = startY + (slot / 3) * 18;
            gui.renderFakeItem(stack, slotX, slotY);
            TooltipOverlayRendererStack.renderStack(gui, font, stack, slotX, slotY);
        }
    }
}
