package net.opal.irisv.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;
import javax.annotation.Nullable;

public interface IBlockTooltipProvider {
    boolean isApplicable(BlockState state, @Nullable BlockEntity be);

    // La signature exacte que tout le monde doit suivre :
    void addTooltip(List<String> info, BlockState state, Level level, BlockPos pos, @Nullable BlockEntity be);
}