package net.opal.irisv.api;

import net.neoforged.neoforge.fluids.FluidStack;

public interface IFluidStorage {
    FluidStack getFluidStack();
    long getAmount();
    long getCapacity();

    default String getFluidName() {
        if (isEmpty()) return "Vide";
        return getFluidStack().getHoverName().getString();
    }

    default boolean isEmpty() {
        return getFluidStack().isEmpty() || getAmount() <= 0;
    }

    default float getFillRatio() {
        if (getCapacity() <= 0) return 0;
        return (float) ((double) getAmount() / getCapacity());
    }

    default String getFormattedAmount() { return formatValue(getAmount()); }
    default String getFormattedCapacity() { return formatValue(getCapacity()); }

    private static String formatValue(long value) {
        if (value >= 1_000_000_000L) return String.format("%.1fG", value / 1_000_000_000.0);
        if (value >= 1_000_000L) return String.format("%.1fM", value / 1_000_000.0);
        if (value >= 10_000L) return (value / 1000) + "k";
        return String.valueOf(value);
    }
}