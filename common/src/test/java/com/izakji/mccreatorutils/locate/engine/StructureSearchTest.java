package com.izakji.mccreatorutils.locate.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.DetectedVersion;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class StructureSearchTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.setVersion(DetectedVersion.BUILT_IN);
        Bootstrap.bootStrap();
    }

    @Test
    void visitsRandomSpreadCandidatesInVanillaRingOrder() {
        var placement = new RandomSpreadStructurePlacement(4, 1, RandomSpreadType.LINEAR, 7);
        var structure = Holder.<Structure>direct(null);
        var observed = new ArrayList<ChunkPos>();
        var input = new StructureSearchInput(
                BlockPos.ZERO,
                1,
                42L,
                false,
                List.of(new StructurePlacementGroup(placement, Set.of(structure))),
                Map.of(),
                (structures, chunk, p, skip, cancellation) -> {
                    observed.add(chunk);
                    return Optional.empty();
                },
                new LocateStats(),
                () -> false);

        assertTrue(StructureSearch.findNearest(input).isEmpty());
        assertEquals(expectedRandomSpreadChunks(placement, 42L, 1), observed);
    }

    @Test
    void returnsFirstHitFromFirstSuccessfulRing() {
        var placement = new RandomSpreadStructurePlacement(4, 1, RandomSpreadType.LINEAR, 7);
        var structure = Holder.<Structure>direct(null);
        var calls = new AtomicInteger();
        var hitChunk = placement.getPotentialStructureChunk(42L, 0, 0);
        var input = new StructureSearchInput(
                BlockPos.ZERO,
                100,
                42L,
                false,
                List.of(new StructurePlacementGroup(placement, Set.of(structure))),
                Map.of(),
                (structures, chunk, p, skip, cancellation) -> {
                    calls.incrementAndGet();
                    return chunk.equals(hitChunk)
                            ? Optional.of(new StructureHit(BlockPos.ZERO, structure))
                            : Optional.empty();
                },
                new LocateStats(),
                () -> false);

        assertEquals(structure, StructureSearch.findNearest(input).orElseThrow().structure());
        assertEquals(1, calls.get());
    }

    @Test
    void cancellationStopsBeforeProbe() {
        var placement = new RandomSpreadStructurePlacement(4, 1, RandomSpreadType.LINEAR, 7);
        var structure = Holder.<Structure>direct(null);
        var input = new StructureSearchInput(
                BlockPos.ZERO,
                100,
                42L,
                false,
                List.of(new StructurePlacementGroup(placement, Set.of(structure))),
                Map.of(),
                (structures, chunk, p, skip, cancellation) -> Optional.empty(),
                new LocateStats(),
                () -> true);

        assertThrows(CancellationException.class, () -> StructureSearch.findNearest(input));
    }

    @Test
    void probesConcentricRingPositions() {
        var placement = new ConcentricRingsStructurePlacement(1, 1, 1, HolderSet.direct());
        var structure = Holder.<Structure>direct(null);
        var ring = new ChunkPos(4, 4);
        var input = new StructureSearchInput(
                BlockPos.ZERO,
                100,
                42L,
                false,
                List.of(new StructurePlacementGroup(placement, Set.of(structure))),
                Map.of(placement, List.of(ring)),
                (structures, chunk, p, skip, cancellation) ->
                        Optional.of(new StructureHit(new BlockPos(40, 32, 40), structure)),
                new LocateStats(),
                () -> false);

        assertEquals(structure, StructureSearch.findNearest(input).orElseThrow().structure());
    }

    @Test
    void choosesClosestHitAcrossPlacementsInSameRing() {
        var first = new RandomSpreadStructurePlacement(4, 1, RandomSpreadType.LINEAR, 7);
        var second = new RandomSpreadStructurePlacement(4, 1, RandomSpreadType.LINEAR, 11);
        var structure = Holder.<Structure>direct(null);
        var groups = List.of(
                new StructurePlacementGroup(first, Set.of(structure)),
                new StructurePlacementGroup(second, Set.of(structure)));
        var input = new StructureSearchInput(
                BlockPos.ZERO,
                1,
                42L,
                false,
                groups,
                Map.of(),
                (structures, chunk, placement, skip, cancellation) -> Optional.of(new StructureHit(
                        placement == first ? new BlockPos(100, 0, 0) : new BlockPos(10, 0, 0), structure)),
                new LocateStats(),
                () -> false);

        assertEquals(
                new BlockPos(10, 0, 0),
                StructureSearch.findNearest(input).orElseThrow().position());
    }

    private static List<ChunkPos> expectedRandomSpreadChunks(
            RandomSpreadStructurePlacement placement, long seed, int radius) {
        List<ChunkPos> chunks = new ArrayList<>();
        int spacing = placement.spacing();
        for (int ring = 0; ring <= radius; ring++) {
            for (int j = -ring; j <= ring; j++) {
                boolean edgeJ = j == -ring || j == ring;
                for (int l = -ring; l <= ring; l++) {
                    boolean edgeL = l == -ring || l == ring;
                    if (edgeJ || edgeL) {
                        chunks.add(placement.getPotentialStructureChunk(seed, spacing * j, spacing * l));
                    }
                }
            }
        }
        return chunks;
    }
}
