package net.opal.irisv.tooltips;

import net.opal.irisv.api.IEntityTooltipProvider;
import net.opal.irisv.tooltips.providers.ArmorStandProvider;
import net.opal.irisv.tooltips.providers.DecorationProvider;
import net.opal.irisv.tooltips.providers.EndCrystalProvider;
import net.opal.irisv.tooltips.providers.generic.LivingEntityProvider;
import net.opal.irisv.tooltips.providers.generic.VehicleProvider;

import java.util.ArrayList;
import java.util.List;

public class TooltipEntityProviderRegistry {
    private static final List<IEntityTooltipProvider> PROVIDERS = new ArrayList<>();

    static {
        PROVIDERS.add(new ArmorStandProvider());
        PROVIDERS.add(new DecorationProvider());
        PROVIDERS.add(new EndCrystalProvider());
        PROVIDERS.add(new VehicleProvider());
        PROVIDERS.add(new LivingEntityProvider());
    }

    public static List<IEntityTooltipProvider> getProviders() {
        return PROVIDERS;
    }
}
