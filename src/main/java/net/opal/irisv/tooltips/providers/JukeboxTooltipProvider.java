package net.opal.irisv.tooltips.providers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.opal.irisv.api.IBlockTooltipProvider;
import net.opal.irisv.network.ClientDataCache;

import java.util.List;

public class JukeboxTooltipProvider implements IBlockTooltipProvider {

    @Override
    public boolean isApplicable(BlockState state, BlockEntity be) {
        return state.is(Blocks.JUKEBOX);
    }

    @Override
    public void addTooltip(List<String> info, BlockState state, Level level, BlockPos pos, BlockEntity be) {
        // 1. Vérification de l'état du bloc (Synchro native Minecraft)
        boolean hasRecord = state.hasProperty(BlockStateProperties.HAS_RECORD) && state.getValue(BlockStateProperties.HAS_RECORD);

        if (!hasRecord) {
            info.add("§8(Vide)");
            return;
        }

        ItemStack stack = ItemStack.EMPTY;

        // 2. Tentative de récupération via la BlockEntity (Priorité Solo)
        if (be instanceof JukeboxBlockEntity jukebox) {
            stack = jukebox.getTheItem();
        }

        // 3. Tentative de récupération via le Cache Réseau (Priorité Multi)
        if (stack.isEmpty()) {
            CompoundTag data = ClientDataCache.get(pos);
            if (data != null && data.contains("RecordItem")) {
                // Utilisation de parseOptional pour reconstruire l'item avec ses composants 1.21
                stack = ItemStack.parseOptional(level.registryAccess(), data.getCompound("RecordItem"));
            }
        }

        // 4. Traitement et affichage du nom
        if (!stack.isEmpty()) {
            Component name;
            // Récupération du composant JukeboxPlayable (Nouveauté 1.21)
            JukeboxPlayable playable = stack.get(DataComponents.JUKEBOX_PLAYABLE);

            if (playable != null) {
                // Extraction de la description de la chanson depuis les registres
                name = playable.song().unwrap(level.registryAccess())
                        .map(ref -> ref.value().description())
                        .orElse(stack.getHoverName());
            } else {
                name = stack.getHoverName();
            }

            info.add("Disque: §b" + name.getString());
        } else {
            // Message d'attente si le disque est présent mais les données NBT non encore reçues
            info.add("Disque: §bChargement...");
        }
    }
}