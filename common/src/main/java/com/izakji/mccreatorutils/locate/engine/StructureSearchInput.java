package com.izakji.mccreatorutils.locate.engine;

import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;

/**
 * Immutable placement snapshot and probe for one structure search.
 *
 * @param origin search origin
 * @param searchRadius maximum random-spread ring radius
 * @param seed level seed
 * @param skipKnownStructures whether already-referenced starts are skipped
 * @param placements placement groups in vanilla order
 * @param concentricPositions precomputed ring positions
 * @param probe structure presence check
 * @param stats mutable search counters
 * @param cancellationToken cooperative cancellation signal
 * @author Izakji
 */
public record StructureSearchInput(
        BlockPos origin,
        int searchRadius,
        long seed,
        boolean skipKnownStructures,
        List<StructurePlacementGroup> placements,
        Map<ConcentricRingsStructurePlacement, List<ChunkPos>> concentricPositions,
        StructureProbe probe,
        LocateStats stats,
        CancellationToken cancellationToken) {}
