package net.opal.irisv.tooltips.providers.generic;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.opal.irisv.api.IBlockAccessor;
import net.opal.irisv.api.IBlockTooltipProvider;

import java.util.List;

public class PlayerHeadTooltipProvider implements IBlockTooltipProvider {

    @Override
    public boolean isApplicable(BlockState state, BlockEntity be) {
        // Supporte les têtes au sol et au mur
        return state.getBlock() instanceof SkullBlock ;
    }

    @Override
    public void addTooltip(List<String> info, IBlockAccessor accessor) {
        CompoundTag nbt = accessor.serverData();
        if (nbt == null) return;

        String ownerName = "";
        if (nbt.contains("profile", 10)) {
            ownerName = nbt.getCompound("profile").getString("name");
        } else if (nbt.contains("SkullOwner", 8)) {
            ownerName = nbt.getString("SkullOwner");
        }

        if (!ownerName.isEmpty()) {
            // Change le titre en haut
            accessor.setTitleOverride(ownerName + " Head");

            // Crée l'item avec la texture pour l'icône de gauche
            ItemStack headStack = new ItemStack(Items.PLAYER_HEAD);
            if (accessor.blockEntity() != null) {
                headStack.applyComponents(accessor.blockEntity().collectComponents());
            }
            accessor.setIcon(headStack);

            // Vide la preview du bas pour éviter les doublons
            accessor.setPreviewItems(new java.util.ArrayList<>());
        }
    }
}