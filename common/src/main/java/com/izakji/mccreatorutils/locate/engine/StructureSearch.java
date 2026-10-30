package com.izakji.mccreatorutils.locate.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

/**
 * Vanilla-compatible structure placement order.
 *
 * @author Izakji
 */
public final class StructureSearch {
    private StructureSearch() {}

    public static Optional<StructureHit> findNearest(StructureSearchInput input) {
        StructureHit best = null;
        double bestDistance = Double.MAX_VALUE;
        List<StructurePlacementGroup> randomGroups = new ArrayList<>();

        for (StructurePlacementGroup group : input.placements()) {
            StructurePlacement placement = group.placement();
            if (placement instanceof ConcentricRingsStructurePlacement concentric) {
                StructureHit bestConcentric = null;
                double bestCandidateDistance = Double.MAX_VALUE;
                for (ChunkPos chunk : input.concentricPositions().getOrDefault(concentric, List.of())) {
                    input.cancellationToken().throwIfCancelled();
                    BlockPos candidate = new BlockPos(
                            SectionPos.sectionToBlockCoord(chunk.x, 8), 32, SectionPos.sectionToBlockCoord(chunk.z, 8));
                    double candidateDistance = input.origin().distSqr(candidate);
                    if (bestConcentric != null && candidateDistance >= bestCandidateDistance) {
                        continue;
                    }

                    input.stats().candidates++;
                    Optional<StructureHit> hit = input.probe()
                            .probe(
                                    group.structures(),
                                    chunk,
                                    placement,
                                    input.skipKnownStructures(),
                                    input.cancellationToken());
                    if (hit.isPresent()) {
                        bestConcentric = hit.get();
                        bestCandidateDistance = candidateDistance;
                    }
                }

                if (bestConcentric != null) {
                    double distance = input.origin().distSqr(bestConcentric.position());
                    if (distance < bestDistance) {
                        best = bestConcentric;
                        bestDistance = distance;
                    }
                }
            } else if (placement instanceof RandomSpreadStructurePlacement) {
                randomGroups.add(group);
            }
        }

        int centerX = SectionPos.blockToSectionCoord(input.origin().getX());
        int centerZ = SectionPos.blockToSectionCoord(input.origin().getZ());
        for (int ring = 0; ring <= input.searchRadius(); ring++) {
            boolean foundInRing = false;
            for (StructurePlacementGroup group : randomGroups) {
                input.cancellationToken().throwIfCancelled();
                Optional<StructureHit> hit = probeRandom(
                        input, group, (RandomSpreadStructurePlacement) group.placement(), centerX, centerZ, ring);
                if (hit.isPresent()) {
                    foundInRing = true;
                    double distance = input.origin().distSqr(hit.get().position());
                    if (distance < bestDistance) {
                        best = hit.get();
                        bestDistance = distance;
                    }
                }
            }
            if (foundInRing) {
                return Optional.ofNullable(best);
            }
        }

        return Optional.ofNullable(best);
    }

    private static Optional<StructureHit> probeRandom(
            StructureSearchInput input,
            StructurePlacementGroup group,
            RandomSpreadStructurePlacement placement,
            int centerX,
            int centerZ,
            int ring) {
        int spacing = placement.spacing();
        for (int j = -ring; j <= ring; j++) {
            boolean edgeJ = j == -ring || j == ring;
            for (int l = -ring; l <= ring; l++) {
                boolean edgeL = l == -ring || l == ring;
                if (!edgeJ && !edgeL) {
                    continue;
                }

                input.cancellationToken().throwIfCancelled();
                int regionX = centerX + spacing * j;
                int regionZ = centerZ + spacing * l;
                ChunkPos chunk = placement.getPotentialStructureChunk(input.seed(), regionX, regionZ);
                input.stats().candidates++;
                Optional<StructureHit> hit = input.probe()
                        .probe(
                                group.structures(),
                                chunk,
                                placement,
                                input.skipKnownStructures(),
                                input.cancellationToken());
                if (hit.isPresent()) {
                    return hit;
                }
            }
        }
        return Optional.empty();
    }
}
