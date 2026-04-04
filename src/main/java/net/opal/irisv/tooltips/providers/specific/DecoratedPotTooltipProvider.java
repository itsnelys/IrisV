package net.opal.irisv.tooltips.providers.specific;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.opal.irisv.api.IBlockAccessor;
import net.opal.irisv.api.IBlockTooltipProvider;
import net.opal.irisv.option.ConfigOptions;

import java.util.List;

public class DecoratedPotTooltipProvider implements IBlockTooltipProvider {

    @Override
    public boolean isApplicable(BlockState state, BlockEntity be) {
        return state.is(Blocks.DECORATED_POT);
    }

    @Override
    public void addTooltip(List<String> info, IBlockAccessor accessor) {
        if (ConfigOptions.getInstance().advancedTooltips) {
            ItemStack contentStack = ItemStack.EMPTY;
            CompoundTag nbt = accessor.serverData();

            // 1. Récupération via le NBT (Réseau)
            // Le pot stocke son item unique dans le tag "item" (minuscule)
            if (nbt != null && nbt.contains("item", 10)) {
                contentStack = ItemStack.parseOptional(accessor.level().registryAccess(), nbt.getCompound("item"));
            }

            // 2. Fallback sur la BlockEntity locale (Solo / Proximité)
            if (contentStack.isEmpty() && accessor.blockEntity() instanceof DecoratedPotBlockEntity pot) {
                contentStack = pot.getTheItem();
            }

            // 3. Affichage
            if (!contentStack.isEmpty()) {
                // On met l'item en preview (icône)
                accessor.setPreviewItems(List.of(contentStack));
            } else {
                info.add("§8(Empty)");
            }
        }
    }
}