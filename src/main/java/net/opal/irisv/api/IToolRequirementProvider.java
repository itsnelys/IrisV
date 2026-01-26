package net.opal.irisv.api;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;

@FunctionalInterface
public interface IToolRequirementProvider {
    void addRequirements(BlockState state, List<ItemStack> tools);
}