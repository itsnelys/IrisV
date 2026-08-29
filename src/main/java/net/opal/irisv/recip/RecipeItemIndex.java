package net.opal.irisv.recip;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class RecipeItemIndex {
    private static final List<ItemStack> ITEMS = new ArrayList<>();

    private RecipeItemIndex() {}

    public static List<ItemStack> getItems() {
        if (ITEMS.isEmpty()) rebuild();
        return ITEMS;
    }

    public static void rebuild() {
        ITEMS.clear();
        Set<String> seen = new HashSet<>();

        addCreativeSearchItems(seen);

        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) continue;

            ItemStack stack = new ItemStack(item);
            if (stack.isEmpty()) continue;
            addUnique(stack, seen);
        }
    }

    private static boolean addCreativeSearchItems(Set<String> seen) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;

        try {
            CreativeModeTabs.tryRebuildTabContents(mc.level.enabledFeatures(), true, mc.level.registryAccess());
            CreativeModeTab searchTab = CreativeModeTabs.searchTab();
            for (ItemStack stack : searchTab.getSearchTabDisplayItems()) {
                addUnique(stack, seen);
            }
            return !seen.isEmpty();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static void addUnique(ItemStack stack, Set<String> seen) {
        if (stack.isEmpty() || stack.getItem() == Items.AIR) return;
        String key = BuiltInRegistries.ITEM.getKey(stack.getItem()) + "|" + stack.getComponents();
        if (seen.add(key)) {
            ITEMS.add(stack.copy());
        }
    }
}
