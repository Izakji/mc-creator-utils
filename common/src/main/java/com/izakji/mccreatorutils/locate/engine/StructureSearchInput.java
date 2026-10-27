package com.izakji.mccreatorutils.locate.engine;

import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;

/**
 * Immutable placement snapshot and probe for one structure search.
 *
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
