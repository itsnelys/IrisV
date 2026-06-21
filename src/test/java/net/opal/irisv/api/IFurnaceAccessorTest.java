package net.opal.irisv.api;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IFurnaceAccessorTest {
    @Test
    void computesClampedProgressValues() {
        CompoundTag data = new CompoundTag();
        data.putInt("BurnTime", 50);
        data.putInt("BurnDuration", 100);
        data.putInt("CookTime", 250);
        data.putInt("CookTimeTotal", 200);

        IFurnaceAccessor accessor = new IFurnaceAccessor(data);

        assertEquals(0.5f, accessor.getBurnProgress());
        assertEquals(1.0f, accessor.getCookProgress());
    }
}
