package net.opal.irisv.tooltips;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.opal.irisv.api.IToolRequirementProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TooltipsRequiredTools {

    // Liste des extensions (utilisée par les autres mods)
    private static final List<IToolRequirementProvider> EXTERNAL_PROVIDERS = new CopyOnWriteArrayList<>();

    /**
     * Permet aux autres moddeurs d'ajouter leurs propres outils (ex: Ruby, Hammer, etc.)
     */
    public static void registerProvider(IToolRequirementProvider provider) {
        EXTERNAL_PROVIDERS.add(provider);
    }

    public static List<ItemStack> getRequiredTools(BlockState state) {
        List<ItemStack> list = new ArrayList<>();
        boolean requiresTool = state.requiresCorrectToolForDrops();

        // 1. Gestion des blocs incassables (Bedrock, Barrier, etc.)
        if (state.getDestroySpeed(null, null) < 0) {
            list.add(new ItemStack(Items.BARRIER));
            return list;
        }

        // 2. Logique Vanilla (Optimisée)
        handleVanillaLogic(state, list);

        // 3. Appel des extensions (Les autres moddeurs se branchent ici)
        for (IToolRequirementProvider provider : EXTERNAL_PROVIDERS) {
            provider.addRequirements(state, list);
        }

        // 4. Cas de secours : Si le bloc nécessite un outil mais aucun n'est défini
        if (requiresTool && list.isEmpty()) {
            list.add(new ItemStack(Items.MACE)); // Ou une icône d'alerte
        }

        return list;
    }

    private static void handleVanillaLogic(BlockState state, List<ItemStack> list) {
        // Pioche
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            list.add(getBestVanillaTool(state, "pickaxe"));
        }
        // Pelle
        if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            list.add(getBestVanillaTool(state, "shovel"));
        }
        // Hache
        if (state.is(BlockTags.MINEABLE_WITH_AXE)) {
            list.add(getBestVanillaTool(state, "axe"));
        }
        // Houe
        if (state.is(BlockTags.MINEABLE_WITH_HOE)) {
            list.add(getBestVanillaTool(state, "hoe"));
        }
        // Cas spéciaux
        if (state.is(Blocks.COBWEB)) {
            list.add(new ItemStack(Items.SHEARS));
            list.add(new ItemStack(Items.WOODEN_SWORD));
        }
        if (state.is(BlockTags.LEAVES)) {
            list.add(new ItemStack(Items.SHEARS));
        }
    }

    private static ItemStack getBestVanillaTool(BlockState state, String type) {
        if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) {
            return switch (type) {
                case "pickaxe" -> new ItemStack(Items.DIAMOND_PICKAXE);
                case "shovel" -> new ItemStack(Items.DIAMOND_SHOVEL);
                case "axe" -> new ItemStack(Items.DIAMOND_AXE);
                default -> new ItemStack(Items.DIAMOND_HOE);
            };
        }
        if (state.is(BlockTags.NEEDS_IRON_TOOL)) {
            return switch (type) {
                case "pickaxe" -> new ItemStack(Items.IRON_PICKAXE);
                case "shovel" -> new ItemStack(Items.IRON_SHOVEL);
                case "axe" -> new ItemStack(Items.IRON_AXE);
                default -> new ItemStack(Items.IRON_HOE);
            };
        }
        if (state.is(BlockTags.NEEDS_STONE_TOOL)) {
            return switch (type) {
                case "pickaxe" -> new ItemStack(Items.STONE_PICKAXE);
                case "shovel" -> new ItemStack(Items.STONE_SHOVEL);
                case "axe" -> new ItemStack(Items.STONE_AXE);
                default -> new ItemStack(Items.STONE_HOE);
            };
        }
        return switch (type) {
            case "pickaxe" -> new ItemStack(Items.WOODEN_PICKAXE);
            case "shovel" -> new ItemStack(Items.WOODEN_SHOVEL);
            case "axe" -> new ItemStack(Items.WOODEN_AXE);
            default -> new ItemStack(Items.WOODEN_HOE);
        };
    }
}