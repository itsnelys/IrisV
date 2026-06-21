package net.opal.irisv.mixin;

import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractFurnaceBlockEntity.class)
public interface FurnaceBlockEntityAccessor {
    @Accessor("litTimeRemaining")
    int irisv$getLitTimeRemaining();

    @Accessor("litTotalTime")
    int irisv$getLitTotalTime();

    @Accessor("cookingTimer")
    int irisv$getCookingTimer();

    @Accessor("cookingTotalTime")
    int irisv$getCookingTotalTime();
}
