package net.opal.irisv.recip;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RecipeLookupTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void ignoresCountDamageAndCustomNameForCrafting() {
        ItemStack target = new ItemStack(Items.IRON_PICKAXE);
        target.setDamageValue(12);
        target.set(DataComponents.CUSTOM_NAME, Component.literal("My pickaxe"));
        assertTrue(RecipeLookup.outputMatches(new ItemStack(Items.IRON_PICKAXE), target));
        assertTrue(RecipeLookup.outputMatches(new ItemStack(Items.OAK_PLANKS, 4), new ItemStack(Items.OAK_PLANKS)));
    }

    @Test void distinguishesPotionEffectsAndContainers() {
        ItemStack healing = PotionContents.createItemStack(Items.POTION, Potions.HEALING);
        assertTrue(RecipeLookup.outputMatches(healing.copy(), healing));
        assertFalse(RecipeLookup.outputMatches(healing, PotionContents.createItemStack(Items.POTION, Potions.SWIFTNESS)));
        assertFalse(RecipeLookup.outputMatches(healing, PotionContents.createItemStack(Items.SPLASH_POTION, Potions.HEALING)));
    }

    @Test void rejectsEmptyAndUnrelatedItems() {
        assertFalse(RecipeLookup.outputMatches(ItemStack.EMPTY, ItemStack.EMPTY));
        assertFalse(RecipeLookup.outputMatches(new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.BIRCH_PLANKS)));
    }

    @Test void findsSmithingTemplateBaseAndAddition() {
        var recipe = new net.minecraft.world.item.crafting.SmithingTransformRecipe(
                net.minecraft.world.item.crafting.Ingredient.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                net.minecraft.world.item.crafting.Ingredient.of(Items.DIAMOND_SWORD),
                net.minecraft.world.item.crafting.Ingredient.of(Items.NETHERITE_INGOT), new ItemStack(Items.NETHERITE_SWORD));
        assertTrue(RecipeLookup.inputMatches(recipe, new ItemStack(Items.DIAMOND_SWORD)));
        assertTrue(RecipeLookup.inputMatches(recipe, new ItemStack(Items.NETHERITE_INGOT)));
        assertTrue(RecipeLookup.inputMatches(recipe, new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)));
        assertFalse(RecipeLookup.inputMatches(recipe, new ItemStack(Items.IRON_SWORD)));
        var ingredients = RecipeLookup.ingredients(new net.minecraft.world.item.crafting.RecipeHolder<>(
                net.minecraft.resources.ResourceLocation.withDefaultNamespace("test"), recipe));
        assertEquals(3, ingredients.size());
        assertTrue(ingredients.get(0).test(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)));
        assertTrue(ingredients.get(1).test(new ItemStack(Items.DIAMOND_SWORD)));
        assertTrue(ingredients.get(2).test(new ItemStack(Items.NETHERITE_INGOT)));
    }

    @Test void brewingWorksInBothDirectionsAndPreservesPotionContents() {
        var builder = new net.minecraft.world.item.alchemy.PotionBrewing.Builder(net.minecraft.world.flag.FeatureFlags.VANILLA_SET);
        net.minecraft.world.item.alchemy.PotionBrewing.addVanillaMixes(builder);
        var brewing = builder.build();
        ItemStack water = PotionContents.createItemStack(Items.POTION, Potions.WATER);
        ItemStack awkward = PotionContents.createItemStack(Items.POTION, Potions.AWKWARD);
        ItemStack wart = new ItemStack(Items.NETHER_WART);
        var items = java.util.List.of(water, awkward, wart);
        var outputs = RecipeLookup.findBrewing(brewing, items, awkward, false);
        assertEquals(1, outputs.size());
        assertTrue(ItemStack.isSameItemSameComponents(water, outputs.getFirst().input()));
        assertEquals(1, RecipeLookup.findBrewing(brewing, items, wart, true).size());
        assertEquals(1, RecipeLookup.findBrewing(brewing, items, water, true).size());
        assertTrue(RecipeLookup.findBrewing(brewing, items, water, false).isEmpty());
    }
}
