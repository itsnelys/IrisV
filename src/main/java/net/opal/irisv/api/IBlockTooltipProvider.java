package net.opal.irisv.api;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;
import javax.annotation.Nullable;

public interface IBlockTooltipProvider {
    default boolean isApplicable(BlockState state, @Nullable BlockEntity be) {
        return true;
    }

    // On passe maintenant l'accessor qui contient TOUT
    void addTooltip(List<String> info, IBlockAccessor accessor);
}