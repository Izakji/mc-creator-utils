package com.izakji.mccreatorutils.locate.engine;

import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

/**
 * Structures that share one placement, in vanilla placement-map order.
 *
 * @author Izakji
 */
public record StructurePlacementGroup(
        StructurePlacement placement, Set<Holder<Structure>> structures) {}
