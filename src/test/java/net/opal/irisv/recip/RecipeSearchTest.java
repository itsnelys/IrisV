package net.opal.irisv.recip;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RecipeSearchTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void recognizesFoodArmorAndDurability() {
        assertTrue(RecipeSearch.matches(new ItemStack(Items.APPLE), ":food"));
        assertTrue(RecipeSearch.matches(new ItemStack(Items.APPLE), ":nourriture"));
        assertFalse(RecipeSearch.matches(new ItemStack(Items.STONE), ":food"));
        assertTrue(RecipeSearch.matches(new ItemStack(Items.IRON_CHESTPLATE), ":armor"));
        assertTrue(RecipeSearch.matches(new ItemStack(Items.IRON_PICKAXE), ":durability"));
        assertFalse(RecipeSearch.matches(new ItemStack(Items.IRON_INGOT), ":durability"));
        assertFalse(RecipeSearch.matches(new ItemStack(Items.STONE), ":unknown"));
    }

    @Test void preservesExistingSearchAndOrSemantics() {
        ItemStack stone = new ItemStack(Items.STONE);
        assertTrue(RecipeSearch.matches(stone, "@minecraft #minecraft:stone $stone"));
        assertTrue(RecipeSearch.matchesAnyToken(stone, ":food stone"));
        assertFalse(RecipeSearch.matches(stone, ":food stone"));
    }
}
