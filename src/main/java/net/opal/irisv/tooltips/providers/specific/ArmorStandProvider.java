package net.opal.irisv.tooltips.providers.specific;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.opal.irisv.api.IBlockAccessor;
import net.opal.irisv.api.IEntityTooltipProvider;
import net.opal.irisv.option.ConfigOptions;

import java.util.List;

public class ArmorStandProvider implements IEntityTooltipProvider {
    @Override
    public boolean isApplicable(Entity entity) {
        return entity instanceof ArmorStand;
    }

    @Override
    public void addTooltip(List<String> tooltip, Entity entity, IBlockAccessor accessor) {
        if (ConfigOptions.getInstance().advancedTooltips) {
            ArmorStand armorStand = (ArmorStand) entity;
            List<ItemStack> preview = accessor.getPreviewItems();

            // 1. GESTION DES ITEMS (Preview)
            // Main droite et gauche
            addIfNotEmpty(preview, armorStand.getMainHandItem());
            addIfNotEmpty(preview, armorStand.getOffhandItem());

            // Armure (Casque, Plastron, Jambières, Bottes)
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                    addIfNotEmpty(preview, armorStand.getItemBySlot(slot));
                }
            }
        }
    }
    private void addIfNotEmpty(List<ItemStack> list, ItemStack stack) {
        if (ConfigOptions.getInstance().advancedTooltips) {
            if (stack != null && !stack.isEmpty()) {
                list.add(stack);
            }
        }
    }
}