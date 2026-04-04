package net.opal.irisv.tooltips.providers.fluid;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidType;
import net.opal.irisv.api.IBlockTooltipProvider;
import net.opal.irisv.api.IBlockAccessor;
import net.opal.irisv.option.ConfigOptions;

import java.util.List;

public class FluidTooltipProvider implements IBlockTooltipProvider {

    @Override
    public boolean isApplicable(BlockState state, BlockEntity be) {
        return !state.getFluidState().isEmpty();
    }

    @Override
    public void addTooltip(List<String> info, IBlockAccessor accessor) {
        FluidState fluidState = accessor.state().getFluidState();
        if (fluidState.isEmpty()) return;

        ConfigOptions config = ConfigOptions.getInstance();

        // On ne traite que si Advanced Tooltips est ON
        if (config.advancedTooltips && config.advancedLiquidStats) {

            // SI CTRL EST PRESSÉ : On affiche toutes les données techniques
            if (Screen.hasControlDown()) {
                var type = fluidState.getFluidType();

                // 1. TYPE ET ÉCOULEMENT
                if (fluidState.isSource()) {
                    info.add("§bType: §3Source Block");
                } else {
                    int flowPercent = (fluidState.getAmount() * 100) / 8;
                    info.add("§bFlowing: §f" + flowPercent + "%");
                }

                // 2. TEMPÉRATURE
                int tempK = type.getTemperature();
                int tempC = tempK - 273;
                String tempColor = "§f";
                String stateSuffix = "";

                if (tempK >= 1000) { tempColor = "§c§l"; stateSuffix = " §7(Extremely Hot)"; }
                else if (tempK >= 450) { tempColor = "§6"; }
                else if (tempK <= 273) { tempColor = "§b§l"; stateSuffix = " §7(Freezing)"; }

                info.add("§eTemperature: " + tempColor + tempK + "K §7(" + tempC + "°C)" + stateSuffix);

                // 3. DENSITÉ ET ÉTAT
                int density = type.getDensity();
                if (density < 0) {
                    info.add("§aState: §lGaseous");
                    info.add("§8Density: §7" + density + " kg/m³");
                } else {
                    info.add("§9State: §lLiquid");
                    if (density > 1000) {
                        info.add("§8Density: §7" + density + " kg/m³");
                    }
                }

                // 4. VISCOSITÉ
                int viscosity = type.getViscosity();
                String viscDesc;
                if (viscosity > 5000) viscDesc = "§4Molten";
                else if (viscosity > 1500) viscDesc = "§6Thick";
                else if (viscosity < 100) viscDesc = "§fThin";
                else viscDesc = "§7Normal";

                info.add("§dViscosity: " + viscDesc + " §7(" + viscosity + " mPa·s)");

            } else {
                // SI CTRL N'EST PAS PRESSÉ : On affiche juste l'indice
                info.add("§8[Hold §fCTRL§8 for details]");
            }
        }
    }
}