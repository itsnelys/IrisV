package net.opal.irisv.recip;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.function.Predicate;
import static org.junit.jupiter.api.Assertions.*;

class RecipeAvailabilityTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void respectsQuantitiesAndDoesNotConsumeStock() {
        Predicate<ItemStack> oak = stack -> stack.is(Items.OAK_PLANKS);
        ItemStack stock = new ItemStack(Items.OAK_PLANKS, 2);
        assertTrue(RecipeAvailability.hasIngredients(List.of(oak, oak), List.of(stock)));
        assertFalse(RecipeAvailability.hasIngredients(List.of(oak, oak, oak), List.of(stock)));
        assertEquals(2, stock.getCount());
    }

    @Test void checklistDoesNotCountTheSameItemTwice() {
        Predicate<ItemStack> oak = stack -> stack.is(Items.OAK_PLANKS);
        boolean[] counts = RecipeAvailability.availableIngredients(List.of(oak, oak), List.of(new ItemStack(Items.OAK_PLANKS)));
        assertEquals(1, (counts[0] ? 1 : 0) + (counts[1] ? 1 : 0));
    }

    @Test void reassignsOverlappingAlternatives() {
        Predicate<ItemStack> wood = stack -> stack.is(Items.OAK_PLANKS) || stack.is(Items.BIRCH_PLANKS);
        Predicate<ItemStack> oak = stack -> stack.is(Items.OAK_PLANKS);
        assertTrue(RecipeAvailability.hasIngredients(List.of(wood, oak),
                List.of(new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.BIRCH_PLANKS))));
        assertFalse(RecipeAvailability.hasIngredients(List.of(), List.of()));
    }
}
