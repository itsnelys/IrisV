package net.opal.irisv.tooltips.providers.fluid;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.opal.irisv.api.IFluidStorage;
import net.opal.irisv.network.ClientDataCache;
import net.opal.irisv.option.ConfigOptions;

import java.util.List;
import java.util.Locale;

public class GenericFluidProvider {

    /**
     * Point d'entrée principal pour récupérer les données de fluide d'un bloc.
     */
    public static IFluidStorage get(BlockState state, BlockEntity be, Level level, BlockPos pos) {
        if (state == null || level == null || pos == null) return null;

        CompoundTag serverData = ClientDataCache.get(pos);
        IFluidHandler handler = findFluidHandler(level, pos, state, be);

        // 1. CAS STANDARD : Utilisation des Capabilities (NeoForge)
        if (handler != null && handler.getTanks() > 0) {
            FluidStack stack = handler.getFluidInTank(0);
            long amount = stack.getAmount();
            long capacity = handler.getTankCapacity(0);

            // Priorité aux données synchronisées via le paquet serveur (plus précis en client)
            if (serverData != null) {
                if (serverData.contains("RealAmount")) amount = serverData.getLong("RealAmount");
                if (serverData.contains("RealCapacity")) capacity = serverData.getLong("RealCapacity");
            }
            return new CapacityTooltipProvider(stack, amount, capacity);
        }

        // 2. CAS FALLBACK : Données NBT directes
        else if (serverData != null && serverData.contains("RealCapacity")) {
            long amount = serverData.getLong("RealAmount");
            long capacity = serverData.getLong("RealCapacity");
            String fluidId = serverData.getString("FluidName");

            FluidStack finalStack = FluidStack.EMPTY;
            if (!fluidId.isEmpty()) {
                var fluidKey = ResourceKey.create(Registries.FLUID, ResourceLocation.parse(fluidId));
                var holder = BuiltInRegistries.FLUID.get(fluidKey);
                if (holder.isPresent()) {
                    finalStack = new FluidStack(holder.get().value(), (int) Math.min(amount, Integer.MAX_VALUE));
                }
            }
            return new CapacityTooltipProvider(finalStack, amount, capacity);
        }

        // 3. CAS SPÉCIFIQUE : Chaudrons Vanille
        return getCauldronStorage(state);
    }

    /**
     * Ajoute les détails techniques (Temp, Densité, Viscosité) si CTRL est pressé.
     */
    public static void addFluidStats(FluidStack stack, List<Component> lines) {
        if (stack.isEmpty()) return;

        if (ConfigOptions.getInstance().advancedLiquidStats) {
            if (Screen.hasControlDown()) {
                FluidType type = stack.getFluid().getFluidType();

                // --- TEMPÉRATURE ---
                int tempK = type.getTemperature();
                int tempC = tempK - 273;
                String tempColor = (tempK >= 1000) ? "§c§l" : (tempK >= 450) ? "§6" : (tempK <= 273) ? "§b§l" : "§f";
                String stateSuffix = (tempK >= 1000) ? " §7(Extremely Hot)" : (tempK <= 273) ? " §7(Freezing)" : "";
                lines.add(Component.literal("§eTemperature: " + tempColor + tempK + "K §7(" + tempC + "°C)" + stateSuffix));

                // --- ÉTAT & DENSITÉ ---
                int density = type.getDensity();
                if (density < 0 || type.isLighterThanAir()) {
                    lines.add(Component.literal("§aState: §lGaseous §7(" + density + " kg/m³)"));
                } else {
                    lines.add(Component.literal("§9State: §lLiquid" + (density > 1000 ? " §7(" + density + " kg/m³)" : "")));
                }

                // --- VISCOSITÉ ---
                int viscosity = type.getViscosity();
                String viscDesc = (viscosity > 5000) ? "§4Molten" : (viscosity > 1500) ? "§6Thick" : (viscosity < 100) ? "§fThin" : "§7Normal";
                lines.add(Component.literal("§dViscosity: " + viscDesc + " §7(" + viscosity + " mPa·s)"));
            } else {
                lines.add(Component.literal("§8[Hold §fCTRL§8 for details]"));
            }
        }
    }

    /**
     * Formate les quantités de fluides (mB, B, kB, etc.)
     */
    public static String formatCompact(long value) {
        if (value < 1000) return value + " mB";
        String[] suffixes = new String[]{"B", "kB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double doubleValue = value / 1000.0;

        while (doubleValue >= 1000 && unitIndex < suffixes.length - 1) {
            doubleValue /= 1000.0;
            unitIndex++;
        }

        if (doubleValue == (long) doubleValue) {
            return String.format(Locale.ROOT, "%d%s", (long) doubleValue, suffixes[unitIndex]);
        } else {
            return String.format(Locale.ROOT, "%.1f%s", doubleValue, suffixes[unitIndex]);
        }
    }

    private static IFluidHandler findFluidHandler(Level level, BlockPos pos, BlockState state, BlockEntity be) {
        IFluidHandler h = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, state, be, null);
        if (h != null) return h;

        for (Direction dir : Direction.values()) {
            h = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, state, be, dir);
            if (h != null) return h;
        }
        return null;
    }

    private static IFluidStorage getCauldronStorage(BlockState state) {
        if (!(state.getBlock() instanceof AbstractCauldronBlock) || state.is(Blocks.CAULDRON)) return null;

        long capacity = 1000L;
        long amount;

        if (state.hasProperty(LayeredCauldronBlock.LEVEL)) {
            amount = (state.getValue(LayeredCauldronBlock.LEVEL) * capacity) / 3;
        } else {
            amount = capacity;
        }

        FluidStack fs = FluidStack.EMPTY;
        if (state.is(Blocks.WATER_CAULDRON)) {
            fs = new FluidStack(Fluids.WATER, (int) amount);
        } else if (state.is(Blocks.LAVA_CAULDRON)) {
            fs = new FluidStack(Fluids.LAVA, (int) amount);
        }

        return !fs.isEmpty() ? new CapacityTooltipProvider(fs, amount, capacity) : null;
    }
}