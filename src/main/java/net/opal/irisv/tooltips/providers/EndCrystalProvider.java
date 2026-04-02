package net.opal.irisv.tooltips.providers;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.opal.irisv.api.IBlockAccessor;
import net.opal.irisv.api.IEntityTooltipProvider;

import java.util.List;

public class EndCrystalProvider implements IEntityTooltipProvider {
    @Override
    public boolean isApplicable(Entity entity) {
        return entity instanceof EndCrystal;
    }

    @Override
    public void addTooltip(List<String> tooltip, Entity entity, IBlockAccessor accessor) {
        EndCrystal crystal = (EndCrystal) entity;

        // Cible du rayon (BeamTarget)
        BlockPos beamTarget = crystal.getBeamTarget();
        if (beamTarget != null) {
            tooltip.add("§7Lien: §dSoin en cours");
            tooltip.add("§8X: " + beamTarget.getX() + " Y: " + beamTarget.getY() + " Z: " + beamTarget.getZ());
        } else {
            tooltip.add("§7État: §7En attente");
        }
    }
}