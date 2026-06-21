package net.opal.irisv.network.server;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

final class ServerDataSanitizer {
    private ServerDataSanitizer() {}

    static CompoundTag sanitize(CompoundTag source, boolean operator) {
        CompoundTag data = new CompoundTag();
        copy(source, data, "front_text");
        copy(source, data, "profile");
        copy(source, data, "SkullOwner");
        copy(source, data, "item");
        copy(source, data, "Book");
        copy(source, data, "Fuel");
        copy(source, data, "BrewTime");
        copy(source, data, "melting_support_current_fuel");

        if (source.contains("Bees", Tag.TAG_LIST)) {
            ListTag bees = new ListTag();
            int count = source.getList("Bees", Tag.TAG_COMPOUND).size();
            for (int i = 0; i < count; i++) bees.add(new CompoundTag());
            data.put("Bees", bees);
        }

        if (operator) {
            copy(source, data, "Command");
            copy(source, data, "name");
            copy(source, data, "target");
            copy(source, data, "pool");
            copy(source, data, "joint");
            copy(source, data, "mode");
        }
        return data;
    }

    private static void copy(CompoundTag source, CompoundTag target, String key) {
        Tag value = source.get(key);
        if (value != null) target.put(key, value.copy());
    }
}
