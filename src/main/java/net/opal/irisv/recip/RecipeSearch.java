package net.opal.irisv.recip;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

final class RecipeSearch {
    private RecipeSearch() {}

    static boolean matches(ItemStack stack, String query) {
        if (query == null || query.isBlank()) return true;

        SearchData data = data(stack);

        for (String token : query.toLowerCase(Locale.ROOT).split("\\s+")) {
            if (token.isBlank()) continue;
            if (!matchesToken(stack, data, token)) return false;
        }

        return true;
    }

    static boolean matchesAnyToken(ItemStack stack, String query) {
        if (query == null || query.isBlank()) return true;

        SearchData data = data(stack);
        for (String token : query.toLowerCase(Locale.ROOT).split("\\s+")) {
            if (!token.isBlank() && matchesToken(stack, data, token)) return true;
        }

        return false;
    }

    private static SearchData data(ItemStack stack) {
        var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return new SearchData(
                stack.getHoverName().getString().toLowerCase(Locale.ROOT),
                key.toString().toLowerCase(Locale.ROOT),
                key.getNamespace().toLowerCase(Locale.ROOT),
                key.getPath().toLowerCase(Locale.ROOT)
        );
    }

    private static boolean matchesToken(ItemStack stack, SearchData data, String token) {
        if (token.startsWith(":")) return switch (token) {
            case ":food", ":nourriture" -> stack.has(net.minecraft.core.component.DataComponents.FOOD);
            case ":armor", ":protection" -> stack.getItem() instanceof net.minecraft.world.item.ArmorItem;
            case ":durability", ":durabilite" -> stack.isDamageableItem();
            case ":fuel", ":combustible" -> stack.getBurnTime(net.minecraft.world.item.crafting.RecipeType.SMELTING) > 0;
            default -> false;
        };
        if (token.startsWith("@")) {
            return data.namespace.contains(token.substring(1));
        }
        if (token.startsWith("#")) {
            return data.id.contains(token.substring(1));
        }
        if (token.startsWith("$")) {
            return data.path.contains(token.substring(1));
        }
        return data.name.contains(token) || data.id.contains(token);
    }

    private record SearchData(String name, String id, String namespace, String path) {}
}
