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

    /**
     * Checks the structures that share one placement at a candidate chunk.
     *
     * @param structures candidate structures in vanilla placement order
     * @param chunk potential structure chunk
     * @param placement placement shared by the candidates
     * @param skipKnownStructures whether already-referenced starts are skipped
     * @param cancellationToken cooperative cancellation signal
     * @return first matching structure, or empty
     */
    Optional<StructureHit> probe(
            Set<Holder<Structure>> structures,
            ChunkPos chunk,
            StructurePlacement placement,
            boolean skipKnownStructures,
            CancellationToken cancellationToken);
}
