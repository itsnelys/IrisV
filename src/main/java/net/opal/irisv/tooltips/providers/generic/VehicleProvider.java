package net.opal.irisv.tooltips.providers.generic;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.*;
import net.opal.irisv.api.IBlockAccessor;
import net.opal.irisv.api.IEntityTooltipProvider;
import net.opal.irisv.option.ConfigOptions;

import java.util.List;

public class VehicleProvider implements IEntityTooltipProvider {

    @Override
    public boolean isApplicable(Entity entity) {
        // AbstractMinecart couvre : Chest, Hopper, TNT, Furnace, CommandBlock, Spawner
        // AbstractBoat couvre : Boat et ChestBoat
        return entity instanceof AbstractMinecart || entity instanceof AbstractBoat;
    }

    @Override
    public void addTooltip(List<String> tooltip, Entity entity, IBlockAccessor accessor) {
        if (entity instanceof AbstractBoat boat) {
            // Extraction du bois via le nom de l'entité (plus fiable que getVariant)
            String id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(boat.getType()).getPath();

            // On nettoie pour ne garder que le type de bois
            String wood = id.replace("_chest_boat", "").replace("_boat", "").replace("_", " ");

            if (ConfigOptions.getInstance().advancedTooltips) {
                // Affichage des passagers
                int passengers = boat.getPassengers().size();
                int max = (entity instanceof ChestBoat) ? 1 : 2; // Un bateau coffre n'a qu'une place

                if (passengers > 0) {
                    tooltip.add("§7Passagers: §f" + passengers + "/" + max);
                }
            }
        }
    }
}