package net.opal.irisv.tooltips.providers;

import net.opal.irisv.api.IEntityTooltipProvider;
import net.opal.irisv.tooltips.providers.specific.*;
import net.opal.irisv.tooltips.providers.generic.*;

import java.util.ArrayList;
import java.util.List;

public class TooltipEntityProviderRegistry {
    private static final List<IEntityTooltipProvider> PROVIDERS_ENTITY = new ArrayList<>();

    static {
        PROVIDERS_ENTITY.add(new ArmorStandProvider());
        PROVIDERS_ENTITY.add(new DecorationProvider());
        PROVIDERS_ENTITY.add(new EndCrystalProvider());
        PROVIDERS_ENTITY.add(new VehicleProvider());
        PROVIDERS_ENTITY.add(new LivingEntityProvider());
    }

    public static List<IEntityTooltipProvider> getProviders() {
        return PROVIDERS_ENTITY;
    }
}
