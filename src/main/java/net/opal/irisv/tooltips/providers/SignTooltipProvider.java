package net.opal.irisv.tooltips.providers;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.opal.irisv.api.IBlockTooltipProvider;
import net.opal.irisv.network.ClientDataCache;

import java.util.ArrayList;
import java.util.List;

public class SignTooltipProvider implements IBlockTooltipProvider {

    @Override
    public boolean isApplicable(BlockState state, BlockEntity be) {
        return be instanceof SignBlockEntity;
    }

    @Override
    public void addTooltip(List<String> info, BlockState state, Level level, BlockPos pos, BlockEntity be) {
        CompoundTag data = ClientDataCache.get(pos);

        if (data.isEmpty() && be != null) {
            data = be.saveWithFullMetadata(level.registryAccess());
        }

        List<String> lines = new ArrayList<>();
        if (data.contains("front_text")) {
            CompoundTag front = data.getCompound("front_text");
            ListTag messages = front.getList("messages", Tag.TAG_STRING);

            for (int i = 0; i < messages.size(); i++) {
                try {
                    String rawJson = messages.getString(i);
                    // NOUVELLE MÉTHODE 1.21 : Utilisation de Component.Serializer.fromJson avec RegistryAccess
                    // Ou plus simplement via le décodeur de composants :
                    Component comp = Component.Serializer.fromJson(rawJson, level.registryAccess());

                    if (comp != null) {
                        String text = comp.getString();
                        if (!text.trim().isEmpty()) lines.add(text);
                    }
                } catch (Exception e) {
                    // Ignorer les erreurs de parsing JSON
                }
            }
        }

        if (lines.isEmpty()) {
            info.add("§8(Empty Sign)");
            return;
        }

        if (Screen.hasControlDown()) {
            info.add("§6--- Sign Text ---");
            for (String line : lines) {
                info.add("§f" + line);
            }
        } else {
            String summary = lines.get(0);
            if (summary.length() > 20) summary = summary.substring(0, 17) + "...";
            info.add("Text: §f\"" + summary + "\"");
            if (lines.size() > 1) info.add("§8[Hold CTRL for " + (lines.size() - 1) + " more]");
        }
    }
}