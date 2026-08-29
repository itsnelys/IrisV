package net.opal.irisv.recip;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;

enum RecipeCategory {
    ALL("All"),
    BLOCKS("Blocks"),
    ENCHANTMENTS("Enchants"),
    POTIONS("Potions"),
    FOOD("Food"),
    ARMOR("Armor"),
    TOOLS("Tools"),
    MISC("Misc"),
    MODS("Mods");

    private final String displayName;

    RecipeCategory(String displayName) {
        this.displayName = displayName;
    }

    String displayName() {
        return displayName;
    }

    ItemStack icon() {
        return switch (this) {
            case ALL -> new ItemStack(Items.COMPASS);
            case BLOCKS -> new ItemStack(Items.OAK_PLANKS);
            case ENCHANTMENTS -> new ItemStack(Items.ENCHANTED_BOOK);
            case POTIONS -> new ItemStack(Items.POTION);
            case FOOD -> new ItemStack(Items.APPLE);
            case ARMOR -> new ItemStack(Items.IRON_CHESTPLATE);
            case TOOLS -> new ItemStack(Items.IRON_PICKAXE);
            case MISC -> new ItemStack(Items.CHEST);
            case MODS -> new ItemStack(Items.COMMAND_BLOCK);
        };
    }

    boolean matches(ItemStack stack) {
        if (this == ALL) return true;

        Item item = stack.getItem();
        boolean block = item instanceof BlockItem;
        boolean potion = item instanceof PotionItem;
        boolean tool = item instanceof PickaxeItem
                || item instanceof AxeItem
                || item instanceof ShovelItem
                || item instanceof HoeItem
                || item instanceof SwordItem;
        boolean armor = item instanceof ArmorItem;
        boolean enchantment = stack.isEnchanted() || item instanceof EnchantedBookItem;
        boolean food = stack.get(DataComponents.FOOD) != null;
        boolean modded = !"minecraft".equals(BuiltInRegistries.ITEM.getKey(item).getNamespace());

        return switch (this) {
            case BLOCKS -> block;
            case ENCHANTMENTS -> enchantment;
            case POTIONS -> potion;
            case FOOD -> food;
            case ARMOR -> armor;
            case TOOLS -> tool;
            case MODS -> modded;
            case MISC -> !block && !potion && !tool && !armor && !enchantment && !food;
            case ALL -> true;
        };
    }
}
