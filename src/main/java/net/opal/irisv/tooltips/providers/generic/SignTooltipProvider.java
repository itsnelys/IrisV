package net.opal.irisv.tooltips.providers.generic;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.opal.irisv.api.IBlockTooltipProvider;
import net.opal.irisv.api.IBlockAccessor;
import net.opal.irisv.option.ConfigOptions;

import java.util.ArrayList;
import java.util.List;

public class SignTooltipProvider implements IBlockTooltipProvider {

    @Override
    public boolean isApplicable(BlockState state, BlockEntity be) {
        return be instanceof SignBlockEntity;
    }

    @Override
    public void addTooltip(List<String> info, IBlockAccessor accessor) {
        if (ConfigOptions.getInstance().advancedTooltips) {
            // On récupère le NBT synchronisé (serverData)
            CompoundTag data = accessor.serverData();

            // Fallback si la BlockEntity est accessible localement (Solo)
            if ((data == null || data.isEmpty()) && accessor.blockEntity() instanceof SignBlockEntity sign) {
                data = sign.saveWithFullMetadata(accessor.level().registryAccess());
            }

            if (data == null) return;

            List<String> lines = new ArrayList<>();

            // En 1.21, le texte est dans "front_text" (Face avant)
            if (data.contains("front_text", 10)) {
                CompoundTag front = data.getCompound("front_text");
                ListTag messages = front.getList("messages", Tag.TAG_STRING);

                for (int i = 0; i < messages.size(); i++) {
                    try {
                        String rawJson = messages.getString(i);
                        // Décodage du composant avec le registre du monde
                        Component comp = Component.Serializer.fromJson(rawJson, accessor.level().registryAccess());

                        if (comp != null) {
                            String text = comp.getString();
                            if (!text.trim().isEmpty()) {
                                lines.add(text);
                            }
                        }
                    } catch (Exception e) {
                        // On ignore les lignes corrompues
                    }
                }
            }

            // Affichage final
            if (lines.isEmpty()) {
                info.add("§8(Panneau vide)");
                return;
            }

            // Système d'affichage intelligent (Résumé ou Complet via CTRL)
            if (Screen.hasControlDown()) {
                info.add("§6--- Texte ---");
                for (String line : lines) {
                    info.add("§f" + line);
                }
            } else {
                String firstLine = lines.get(0);
                if (firstLine.length() > 20) firstLine = firstLine.substring(0, 17) + "...";
                info.add("Texte: §f\"" + firstLine + "\"");
                if (lines.size() > 1) {
                    info.add("§8[Maintenir CTRL pour voir la suite]");
                }
            }
        }
    }
}