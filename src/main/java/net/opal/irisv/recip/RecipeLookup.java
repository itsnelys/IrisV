package net.opal.irisv.recip;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.alchemy.PotionBrewing;

import java.util.ArrayList;
import java.util.List;

public final class RecipeLookup {
    private RecipeLookup() {}

    public static List<RecipeHolder<?>> findRecipesForOutput(ItemStack target) {
        return find(target, false);
    }

    public static List<RecipeHolder<?>> findUses(ItemStack target) {
        return find(target, true);
    }

    private static List<RecipeHolder<?>> find(ItemStack target, boolean uses) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || target.isEmpty()) return List.of();

        List<RecipeHolder<?>> matches = new ArrayList<>();
        var registries = mc.level.registryAccess();

        for (RecipeHolder<?> holder : mc.level.getRecipeManager().getRecipes()) {
            try {
                ItemStack result = holder.value().getResultItem(registries);
                boolean matchesInput = inputMatches(holder.value(), target);
                boolean matchesOutput = holder.value() instanceof SmithingTrimRecipe trim
                        ? trim.isBaseIngredient(target) : outputMatches(result, target);
                if (uses ? matchesInput : matchesOutput) {
                    matches.add(holder);
                }
            } catch (RuntimeException ignored) {
            }
        }

        matches.sort(java.util.Comparator.comparing(holder -> holder.id().toString()));
        return matches;
    }

    static boolean inputMatches(net.minecraft.world.item.crafting.Recipe<?> recipe, ItemStack target) {
        if (target.isEmpty()) return false;
        if (recipe instanceof net.minecraft.world.item.crafting.AbstractCookingRecipe
                && recipe.getType() != net.minecraft.world.item.crafting.RecipeType.CAMPFIRE_COOKING
                && target.getBurnTime(recipe.getType()) > 0) return true;
        return recipe instanceof SmithingRecipe smithing
                ? smithing.isTemplateIngredient(target) || smithing.isBaseIngredient(target) || smithing.isAdditionIngredient(target)
                : recipe.getIngredients().stream().anyMatch(ingredient -> ingredient.test(target));
    }

    static boolean outputMatches(ItemStack result, ItemStack target) {
        if (result.isEmpty() || target.isEmpty() || !ItemStack.isSameItem(result, target)) return false;
        // Damage and custom names do not change how an item is crafted; potion contents do.
        return java.util.Objects.equals(result.get(DataComponents.POTION_CONTENTS), target.get(DataComponents.POTION_CONTENTS));
    }

    public record BrewingEntry(ItemStack input, ItemStack reagent, ItemStack output) {}

    public static List<BrewingEntry> findBrewing(ItemStack target, boolean uses) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || target.isEmpty()) return List.of();
        return findBrewing(mc.level.potionBrewing(), RecipeItemIndex.getItems(), target, uses);
    }

    static List<BrewingEntry> findBrewing(PotionBrewing brewing, List<ItemStack> items, ItemStack target, boolean uses) {
        List<ItemStack> inputs = items.stream().filter(brewing::isInput).toList();
        List<ItemStack> reagents = items.stream().filter(brewing::isIngredient).toList();
        List<BrewingEntry> result = new ArrayList<>();
        for (ItemStack input : inputs) {
            for (ItemStack reagent : reagents) {
                if (uses && !outputMatches(input, target) && !outputMatches(reagent, target)
                        && !target.is(net.minecraft.world.item.Items.BLAZE_POWDER)) continue;
                if (!brewing.hasMix(input, reagent)) continue;
                ItemStack output = brewing.mix(reagent, input);
                if (!output.isEmpty() && (uses || outputMatches(output, target))) {
                    result.add(new BrewingEntry(input.copy(), reagent.copy(), output.copy()));
                }
            }
        }
        return List.copyOf(result);
    }

    public static List<Ingredient> ingredients(RecipeHolder<?> holder) {
        if (holder.value() instanceof SmithingRecipe smithing) {
            return List.of(matching(smithing::isTemplateIngredient), matching(smithing::isBaseIngredient),
                    matching(smithing::isAdditionIngredient));
        }
        return List.copyOf(holder.value().getIngredients());
    }

    private static Ingredient matching(java.util.function.Predicate<ItemStack> predicate) {
        return Ingredient.of(BuiltInRegistries.ITEM.stream().map(ItemStack::new).filter(predicate));
    }
}
