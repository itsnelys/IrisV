package net.opal.irisv.mixin;

import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractFurnaceBlockEntity.class)
public interface FurnaceBlockEntityAccessor {
    @Accessor("litTime")
    int irisv$getLitTimeRemaining();

    @Accessor("litDuration")
    int irisv$getLitTotalTime();

    @Accessor("cookingProgress")
    int irisv$getCookingTimer();

    @Accessor("cookingTotalTime")
    int irisv$getCookingTotalTime();
}
