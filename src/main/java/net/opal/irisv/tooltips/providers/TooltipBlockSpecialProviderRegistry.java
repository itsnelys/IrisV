package net.opal.irisv.tooltips.providers;

import net.opal.irisv.api.IBlockPreviewRenderer;
import net.opal.irisv.tooltips.providers.special.*;

import java.util.ArrayList;
import java.util.List;

public class TooltipBlockSpecialProviderRegistry {
    private static final List<IBlockPreviewRenderer> PROVIDERS_SPECIAL = new ArrayList<>();

    static {
        PROVIDERS_SPECIAL.add(new FurnaceTooltipProvider());
        PROVIDERS_SPECIAL.add(new BrewingStandTooltipProvider());
        PROVIDERS_SPECIAL.add(new CampfireTooltipProvider());
        PROVIDERS_SPECIAL.add(new CrafterTooltipProvider());
    }

    public static List<IBlockPreviewRenderer> getRenderers() {
        return PROVIDERS_SPECIAL;
    }
}
