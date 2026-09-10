package net.opal.irisv.recip;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.opal.irisv.option.ConfigOptions;
import java.util.ArrayList;
import java.util.List;

/** Bookmarks keep recipe identity and component-aware outputs, separate from item favorites. */
final class RecipeBookmarks {
    record Entry(String key, ItemStack output, RecipeHolder<?> recipe, RecipeLookup.BrewingEntry brewing) {}
    private static List<Entry> cache;
    private static Object cachedLevel;

    static void invalidate() { cache = null; }

    static Entry recipe(RecipeHolder<?> holder, ItemStack output) {
        return new Entry(holder.id().toString(), output.copy(), holder, null);
    }

    static Entry brewing(RecipeLookup.BrewingEntry brewing) {
        String key = "brewing/" + encodeStack(brewing.input()) + "/" + encodeStack(brewing.reagent());
        return new Entry(key, brewing.output().copy(), null, brewing);
    }

    static List<Entry> entries() {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return List.of();
        if (cache != null && cachedLevel == mc.level) return cache;
        cachedLevel = mc.level;
        List<Entry> loaded = new ArrayList<>();
        for (String saved : ConfigOptions.getInstance().savedRecipeBookmarks) {
            try {
                JsonObject json = JsonParser.parseString(saved).getAsJsonObject();
                ItemStack output = decodeStack(json.get("output"));
                if (output.isEmpty()) continue;
                if (json.has("input")) {
                    ItemStack input = decodeStack(json.get("input"));
                    ItemStack reagent = decodeStack(json.get("reagent"));
                    var brewer = mc.level.potionBrewing();
                    if (brewer.hasMix(input, reagent) && ItemStack.isSameItemSameComponents(brewer.mix(reagent, input), output))
                        loaded.add(brewing(new RecipeLookup.BrewingEntry(input, reagent, output)));
                } else {
                    mc.level.getRecipeManager().byKey(ResourceLocation.parse(json.get("id").getAsString()))
                            .ifPresent(holder -> loaded.add(recipe(holder, output)));
                }
            } catch (RuntimeException ignored) {
                // Keep unavailable mod recipes on disk so they can return with their mod/world.
            }
        }
        cache = List.copyOf(loaded);
        return cache;
    }

    static boolean contains(Entry entry) { return entries().stream().anyMatch(e -> e.key.equals(entry.key)); }

    static void toggle(Entry entry) {
        ConfigOptions config = ConfigOptions.getInstance();
        List<String> saved = new ArrayList<>(config.savedRecipeBookmarks);
        boolean removed = saved.removeIf(value -> {
            try { return JsonParser.parseString(value).getAsJsonObject().get("key").getAsString().equals(entry.key); }
            catch (RuntimeException ignored) { return false; }
        });
        if (!removed) {
            JsonObject json = new JsonObject();
            json.addProperty("key", entry.key);
            json.add("output", encodeStack(entry.output));
            if (entry.recipe != null) json.addProperty("id", entry.recipe.id().toString());
            else {
                json.add("input", encodeStack(entry.brewing.input()));
                json.add("reagent", encodeStack(entry.brewing.reagent()));
            }
            saved.add(json.toString());
        }
        config.savedRecipeBookmarks = saved;
        config.save();
        invalidate();
    }

    private static com.google.gson.JsonElement encodeStack(ItemStack stack) {
        return ItemStack.CODEC.encodeStart(Minecraft.getInstance().level.registryAccess().createSerializationContext(JsonOps.INSTANCE), stack).getOrThrow();
    }
    private static ItemStack decodeStack(com.google.gson.JsonElement value) {
        return ItemStack.CODEC.parse(Minecraft.getInstance().level.registryAccess().createSerializationContext(JsonOps.INSTANCE), value).getOrThrow();
    }
}
