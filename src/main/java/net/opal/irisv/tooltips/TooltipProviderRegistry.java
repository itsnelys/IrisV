package net.opal.irisv.tooltips;

import net.opal.irisv.api.IBlockTooltipProvider;
import net.opal.irisv.tooltips.modded.providers.MeltingSupportTooltipProvider;
import net.opal.irisv.tooltips.providers.*;
import net.opal.irisv.tooltips.providers.NoteblockTooltipProvider;
import net.opal.irisv.tooltips.providers.generic.FluidTooltipProvider;
import net.opal.irisv.tooltips.providers.generic.InteractionTooltipProvider;

import java.util.ArrayList;
import java.util.List;

public class TooltipProviderRegistry {
    private static final List<IBlockTooltipProvider> PROVIDERS = new ArrayList<>();

    static {
        // --- Enregistrement des modules Vanillas ---
        TooltipProviderRegistry.register(new CopperTooltipProvider());
        TooltipProviderRegistry.register(new BeehiveTooltipProvider());
        TooltipProviderRegistry.register(new SignTooltipProvider());
        TooltipProviderRegistry.register(new RedstoneTooltipProvider());
        TooltipProviderRegistry.register(new EnderFrameTooltipProvider());
        TooltipProviderRegistry.register(new AgricultureTooltipProvider());
        TooltipProviderRegistry.register(new EnchantmentTooltipProvider());
        TooltipProviderRegistry.register(new ChiseledBookshelfTooltipProvider());
        TooltipProviderRegistry.register(new InteractionTooltipProvider());
        TooltipProviderRegistry.register(new NoteblockTooltipProvider());
        TooltipProviderRegistry.register(new FluidTooltipProvider());
        TooltipProviderRegistry.register(new JukeboxTooltipProvider());

        // --- Enregistrement des modules Moddés ---
        TooltipProviderRegistry.register(new MeltingSupportTooltipProvider());
    }

    public static void register(IBlockTooltipProvider provider) {
        PROVIDERS.add(provider);
    }

    public static List<IBlockTooltipProvider> getProviders() {
        return PROVIDERS;
    }
}