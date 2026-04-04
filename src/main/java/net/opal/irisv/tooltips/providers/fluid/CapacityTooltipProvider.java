package net.opal.irisv.tooltips.providers.fluid;

import net.neoforged.neoforge.fluids.FluidStack;
import net.opal.irisv.api.IFluidStorage;

public class CapacityTooltipProvider implements IFluidStorage {
    private final FluidStack stack;
    private final long amount;
    private final long capacity;

    public CapacityTooltipProvider(FluidStack stack, long amount, long capacity) {
        this.stack = stack;
        this.amount = amount;
        this.capacity = capacity;
    }

    @Override public FluidStack getFluidStack() { return stack; }
    @Override public long getAmount() { return amount; }
    @Override public long getCapacity() { return capacity; }
}