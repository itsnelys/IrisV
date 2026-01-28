package net.opal.irisv.tooltips.providers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.opal.irisv.api.IBlockAccessor;
import net.opal.irisv.api.IBlockTooltipProvider;

import java.util.List;

public class JukeboxTooltipProvider implements IBlockTooltipProvider {

    @Override
    public boolean isApplicable(BlockState state, BlockEntity be) {
        return state.is(Blocks.JUKEBOX);
    }

    @Override
    public void addTooltip(List<String> info, IBlockAccessor accessor) {
        BlockState state = accessor.state();

        // Vérification rapide via le BlockState
        boolean hasRecord = state.hasProperty(BlockStateProperties.HAS_RECORD) && state.getValue(BlockStateProperties.HAS_RECORD);

        if (!hasRecord) {
            info.add("§8(Empty)");
            return;
        }

        ItemStack recordStack = ItemStack.EMPTY;
        CompoundTag nbt = accessor.serverData();

        // 1. Récupération de l'item (Priorité NBT Réseau, puis BlockEntity locale)
        // Note : En 1.21, le tag peut être "RecordItem" ou "record_item" selon la source
        if (nbt != null && nbt.contains("Items", 9)) {
            ListTag items = nbt.getList("Items", 10);
            if (!items.isEmpty()) {
                // Le disque est dans notre liste universelle
                recordStack = ItemStack.parseOptional(accessor.level().registryAccess(), items.getCompound(0));
            }
        }

        if (recordStack.isEmpty() && accessor.blockEntity() instanceof JukeboxBlockEntity jukebox) {
            recordStack = jukebox.getTheItem();
        }

        // 2. Affichage
        if (!recordStack.isEmpty()) {
            // Récupération de la description (Artiste - Titre)
            List<Component> tooltipLines = recordStack.getTooltipLines(
                    net.minecraft.world.item.Item.TooltipContext.of(accessor.level()),
                    accessor.player(),
                    TooltipFlag.Default.NORMAL
            );

            String[] colors = {"§c", "§6", "§e", "§a", "§b", "§d"}; // Rouge, Orange, Jaune, Vert, Bleu, Rose
            int colorIndex = (int) ((System.currentTimeMillis() / 600) % colors.length);
            String rainbowColor = colors[colorIndex];

            if (tooltipLines.size() > 1) {
                info.add(rainbowColor + tooltipLines.get(1).getString());
            }

            // AJOUT : Affiche l'icône du disque dans la preview
            accessor.setPreviewItems(List.of(recordStack));
        } else {
            info.add("§8Chargement...");
        }
    }
}