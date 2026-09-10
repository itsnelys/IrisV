package net.opal.irisv.recip;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Locale;

public enum RecipeHudCategory {
    INVENTORY, CREATIVE, CRAFTING, CHEST, FURNACE, BLAST_FURNACE, SMOKER,
    BREWING, SMITHING, STONECUTTER, ANVIL, GRINDSTONE, ENCHANTING,
    LOOM, CARTOGRAPHY, HOPPER, DISPENSER, SHULKER, BEACON, OTHER;

    public String key() { return name().toLowerCase(Locale.ROOT); }
    public String labelKey() { return "menu.irisv.hud_category." + key(); }

    static RecipeHudCategory of(Screen screen) {
        if (screen instanceof InventoryScreen) return INVENTORY;
        if (screen instanceof CreativeModeInventoryScreen) return CREATIVE;
        if (!(screen instanceof AbstractContainerScreen<?> container)) return OTHER;
        var id = BuiltInRegistries.MENU.getKey(container.getMenu().getType());
        if (id == null || !id.getNamespace().equals("minecraft")) return OTHER;
        return switch (id.getPath()) {
            case "crafting" -> CRAFTING;
            case "generic_9x1", "generic_9x2", "generic_9x3", "generic_9x4", "generic_9x5", "generic_9x6" -> CHEST;
            case "furnace" -> FURNACE;
            case "blast_furnace" -> BLAST_FURNACE;
            case "smoker" -> SMOKER;
            case "brewing_stand" -> BREWING;
            case "smithing" -> SMITHING;
            case "stonecutter" -> STONECUTTER;
            case "anvil" -> ANVIL;
            case "grindstone" -> GRINDSTONE;
            case "enchantment" -> ENCHANTING;
            case "loom" -> LOOM;
            case "cartography_table" -> CARTOGRAPHY;
            case "hopper" -> HOPPER;
            case "generic_3x3" -> DISPENSER;
            case "shulker_box" -> SHULKER;
            case "beacon" -> BEACON;
            default -> OTHER;
        };
    }
}
