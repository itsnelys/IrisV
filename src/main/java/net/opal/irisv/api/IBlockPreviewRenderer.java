package net.opal.irisv.api;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.opal.irisv.tooltips.TooltipData;

import javax.annotation.Nullable;
import java.util.List;

public interface IBlockPreviewRenderer {
    // Est-ce que ce moteur de rendu s'occupe de ce bloc ?
    boolean isApplicable(BlockState state, @Nullable BlockEntity be);

    // Comment on dessine les items et les infos ?
    void render(GuiGraphics gui, Font font, List<ItemStack> items, int x, int y, BlockState state, @Nullable BlockEntity be, TooltipData.BlockInfo info);
}