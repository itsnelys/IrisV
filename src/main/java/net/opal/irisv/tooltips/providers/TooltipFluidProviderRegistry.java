package net.opal.irisv.tooltips.providers;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.opal.irisv.api.IFluidStorage;
import net.opal.irisv.tooltips.providers.fluid.GenericFluidProvider;

import java.util.ArrayList;
import java.util.List;

public class TooltipFluidProviderRegistry {

    // On utilise IFluidStorage pour permettre à n'importe quel type de provider
    // (Chaudron, Machine, NBT) d'être enregistré.
    private static final List<FluidProviderFunction> PROVIDERS = new ArrayList<>();

    static {
        // LE PROVIDER GÉNÉRIQUE : Il gère les Chaudrons, les Machines (Capabilities)
        // et les cas spéciaux (Large Fluid Tank).
        // Comme GenericFluidProvider.get renvoie un IFluidStorage, ça match !
        register(GenericFluidProvider::get);
    }

    /**
     * Enregistre un provider de fluide personnalisé.
     */
    public static void register(FluidProviderFunction provider) {
        PROVIDERS.add(provider);
    }

    /**
     * La méthode principale appelée par ton système de rendu / Tooltip.
     * @return Un IFluidStorage si du fluide est trouvé, sinon null.
     */
    public static IFluidStorage get(BlockState state, BlockEntity be, Level level, BlockPos pos) {
        if (state == null || level == null || pos == null) return null;

        for (FluidProviderFunction provider : PROVIDERS) {
            IFluidStorage data = provider.apply(state, be, level, pos);
            if (data != null) return data;
        }
        return null;
    }

    /**
     * Interface fonctionnelle compatible avec IFluidStorage.
     */
    @FunctionalInterface
    public interface FluidProviderFunction {
        IFluidStorage apply(BlockState state, BlockEntity be, Level level, BlockPos pos);
    }
}