package com.izakji.mccreatorutils.locate.engine;

import java.util.Optional;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

/**
 * Checks one structure placement candidate. Production runs this on the structure lane.
 *
 * @author Izakji
 */
@FunctionalInterface
public interface StructureProbe {

    Optional<StructureHit> probe(
            Set<Holder<Structure>> structures,
            ChunkPos chunk,
            StructurePlacement placement,
            boolean skipKnownStructures);
}
