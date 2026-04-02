package net.opal.irisv.api;

import net.minecraft.world.entity.Entity;
import java.util.List;

public interface IEntityTooltipProvider {
    /**
     * @return true si ce provider peut fournir des infos pour cette entité
     */
    boolean isApplicable(Entity entity);

    /**
     * Ajoute des lignes de texte ou modifie l'accessor
     */
    void addTooltip(List<String> tooltip, Entity entity, IBlockAccessor accessor);
}