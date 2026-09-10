package net.opal.irisv.recip;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Advisory ingredient check only; never moves items or sends crafting packets. */
final class RecipeAvailability {
    private RecipeAvailability() {}

    static boolean isWorkstation(Screen screen) {
        if (net.opal.irisv.api.compat.IrisVCompatibility.find(screen) != null) return true;
        if (!(screen instanceof AbstractContainerScreen<?> container)) return false;
        var menu = container.getMenu();
        return menu instanceof CraftingMenu || menu instanceof AbstractFurnaceMenu
                || menu instanceof StonecutterMenu || menu instanceof SmithingMenu || menu instanceof BrewingStandMenu;
    }

    static boolean compatible(Screen screen, RecipeBookmarks.Entry entry) {
        var integration = net.opal.irisv.api.compat.IrisVCompatibility.find(screen);
        if (integration != null) return entry.recipe() != null && net.opal.irisv.api.compat.IrisVCompatibility.call(integration,
                () -> integration.supportsRecipe((AbstractContainerScreen<?>) screen, entry.recipe()), false);
        if (!isWorkstation(screen)) return false;
        var menu = ((AbstractContainerScreen<?>) screen).getMenu();
        if (entry.brewing() != null) return menu instanceof BrewingStandMenu;
        var recipe = entry.recipe().value();
        var type = recipe.getType();
        return menu instanceof CraftingMenu && type == RecipeType.CRAFTING && recipe.canCraftInDimensions(3, 3)
                || menu instanceof FurnaceMenu && type == RecipeType.SMELTING
                || menu instanceof BlastFurnaceMenu && type == RecipeType.BLASTING
                || menu instanceof SmokerMenu && type == RecipeType.SMOKING
                || menu instanceof StonecutterMenu && type == RecipeType.STONECUTTING
                || menu instanceof SmithingMenu && type == RecipeType.SMITHING;
    }

    static boolean ingredientsAvailable(Screen screen, RecipeBookmarks.Entry entry) {
        if (!compatible(screen, entry) || Minecraft.getInstance().player == null) return false;
        List<Predicate<ItemStack>> needs = new ArrayList<>();
        if (entry.brewing() != null) {
            needs.add(stack -> ItemStack.isSameItemSameComponents(stack, entry.brewing().input()));
            needs.add(stack -> ItemStack.isSameItemSameComponents(stack, entry.brewing().reagent()));
        } else {
            for (Ingredient ingredient : RecipeLookup.ingredients(entry.recipe())) if (!ingredient.isEmpty()) needs.add(ingredient::test);
        }
        if (needs.isEmpty()) return false;
        List<ItemStack> stock = new ArrayList<>();
        var player = Minecraft.getInstance().player;
        for (int i = 0; i < 36; i++) stock.add(player.getInventory().getItem(i));
        var menu = ((AbstractContainerScreen<?>) screen).getMenu();
        int start = menu instanceof CraftingMenu ? 1 : 0;
        int end = menu instanceof CraftingMenu ? 10 : menu instanceof SmithingMenu ? 3 : menu instanceof BrewingStandMenu ? 4 : 1;
        for (int i = start; i < end; i++) stock.add(menu.getSlot(i).getItem());
        return hasIngredients(needs, stock);
    }

    static boolean hasIngredients(List<Predicate<ItemStack>> needs, List<ItemStack> stock) {
        if (needs.isEmpty()) return false;
        for (boolean available : availableIngredients(needs, stock)) if (!available) return false;
        return true;
    }

    static boolean[] availableIngredients(List<Predicate<ItemStack>> needs, List<ItemStack> stock) {
        List<ItemStack> units = new ArrayList<>();
        for (ItemStack stack : stock) for (int n = 0; n < Math.min(stack.getCount(), needs.size()); n++) units.add(stack);
        int[] owners = new int[units.size()];
        java.util.Arrays.fill(owners, -1);
        for (int i = 0; i < needs.size(); i++) assign(i, needs, units, owners, new boolean[units.size()]);
        boolean[] available = new boolean[needs.size()];
        for (int owner : owners) if (owner >= 0) available[owner] = true;
        return available;
    }

    // Augmenting paths avoid consuming a rare ingredient for a broader tag match.
    private static boolean assign(int need, List<Predicate<ItemStack>> needs, List<ItemStack> units, int[] owners, boolean[] visited) {
        for (int i = 0; i < units.size(); i++) {
            if (visited[i] || !needs.get(need).test(units.get(i))) continue;
            visited[i] = true;
            if (owners[i] < 0 || assign(owners[i], needs, units, owners, visited)) {
                owners[i] = need;
                return true;
            }
        }
        return false;
    }
}
