package net.opal.irisv.tooltips.providers.specific;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.opal.irisv.api.IBlockAccessor;
import net.opal.irisv.api.IBlockTooltipProvider;
import net.opal.irisv.option.ConfigOptions;

import java.util.List;

public class LecternTooltipProvider implements IBlockTooltipProvider {

    @Override
    public boolean isApplicable(BlockState state, BlockEntity be) {
        return state.is(Blocks.LECTERN);
    }

    @Override
    public void addTooltip(List<String> info, IBlockAccessor accessor) {
        if (ConfigOptions.getInstance().advancedTooltips) {
            ItemStack bookStack = ItemStack.EMPTY;
            CompoundTag nbt = accessor.serverData();

            // 1. Récupération du livre via les données serveur (NBT)
            if (nbt != null && nbt.contains("Book", 10)) {
                bookStack = ItemStack.parseOptional(accessor.level().registryAccess(), nbt.getCompound("Book"));
            }

            // 2. Fallback local si les données réseau ne sont pas encore arrivées
            if (bookStack.isEmpty() && accessor.blockEntity() instanceof LecternBlockEntity lectern) {
                bookStack = lectern.getBook();
            }

            // 3. Affichage uniquement si le livre existe
            if (!bookStack.isEmpty()) {
                // On affiche le nom du livre proprement
                info.add("§fLivre : " + bookStack.getHoverName().getString());

                // On ajoute l'icône du livre dans la preview
                accessor.setPreviewItems(List.of(bookStack));
            } else {
                // Optionnel : ne rien mettre du tout, ou "Vide"
                info.add("§8(Empty)");
            }
        }
    }
}