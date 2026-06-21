package net.opal.irisv.network.server;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerDataSanitizerTest {
    @Test
    void removesSensitiveAndUnknownDataForRegularPlayers() {
        CompoundTag source = new CompoundTag();
        source.putString("Command", "say secret");
        source.putString("CustomSecret", "hidden");
        source.putInt("BrewTime", 42);

        CompoundTag result = ServerDataSanitizer.sanitize(source, false);

        assertEquals(42, result.getInt("BrewTime"));
        assertFalse(result.contains("Command"));
        assertFalse(result.contains("CustomSecret"));
    }

    @Test
    void exposesAdministrativeDataOnlyToOperators() {
        CompoundTag source = new CompoundTag();
        source.putString("Command", "say allowed");
        source.putString("name", "irisv:test");

        CompoundTag result = ServerDataSanitizer.sanitize(source, true);

        assertEquals("say allowed", result.getString("Command"));
        assertEquals("irisv:test", result.getString("name"));
    }

    @Test
    void keepsOnlyBeeCount() {
        CompoundTag source = new CompoundTag();
        ListTag bees = new ListTag();
        CompoundTag bee = new CompoundTag();
        bee.putString("EntityData", "private");
        bees.add(bee);
        source.put("Bees", bees);

        CompoundTag result = ServerDataSanitizer.sanitize(source, false);

        assertTrue(result.contains("Bees"));
        assertEquals(1, result.getList("Bees", Tag.TAG_COMPOUND).size());
        assertTrue(result.getList("Bees", Tag.TAG_COMPOUND).getCompound(0).isEmpty());
    }
}
