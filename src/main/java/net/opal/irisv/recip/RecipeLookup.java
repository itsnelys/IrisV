package net.opal.irisv.recip;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.List;

public final class RecipeLookup {
    private RecipeLookup() {}

    public static List<RecipeHolder<?>> findRecipesForOutput(ItemStack target) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || target.isEmpty()) return List.of();

        List<RecipeHolder<?>> matches = new ArrayList<>();
        var registries = mc.level.registryAccess();

        for (RecipeHolder<?> holder : mc.level.getRecipeManager().getRecipes()) {
            try {
                ItemStack result = holder.value().getResultItem(registries);
                if (!result.isEmpty() && ItemStack.isSameItemSameComponents(result, target)) {
                    matches.add(holder);
                }
            } catch (RuntimeException ignored) {
            }
        }

        return matches;
    }
}
