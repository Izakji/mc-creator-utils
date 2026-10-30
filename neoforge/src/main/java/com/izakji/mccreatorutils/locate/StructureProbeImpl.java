package com.izakji.mccreatorutils.locate;

import com.izakji.mccreatorutils.locate.engine.CancellationToken;
import com.izakji.mccreatorutils.locate.engine.LocateStats;
import com.izakji.mccreatorutils.locate.engine.StructureBiomeFilter;
import com.izakji.mccreatorutils.locate.engine.StructureHit;
import com.izakji.mccreatorutils.locate.engine.StructureProbe;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

/**
 * World-side structure presence checks, run only on the structure lane.
 *
 * @author Izakji
 */
public final class StructureProbeImpl implements StructureProbe {
    private final ServerLevel level;
    private final StructureBiomeFilter filter;
    private final LocateStats stats;

    public StructureProbeImpl(ServerLevel level, StructureBiomeFilter filter, LocateStats stats) {
        this.level = level;
        this.filter = filter;
        this.stats = stats;
    }

    @Override
    public Optional<StructureHit> probe(
            Set<Holder<Structure>> structures,
            ChunkPos chunk,
            StructurePlacement placement,
            boolean skipKnownStructures,
            CancellationToken cancellationToken) {
        for (Holder<Structure> holder : structures) {
            cancellationToken.throwIfCancelled();
            if (!filter.mayMatch(holder, chunk)) {
                stats.prefilterSkips++;
                continue;
            }

            stats.presenceChecks++;
            StructureCheckResult result = level.structureManager()
                    .checkStructurePresence(chunk, holder.value(), placement, skipKnownStructures);
            if (result == StructureCheckResult.START_NOT_PRESENT) {
                continue;
            }
            if (!skipKnownStructures && result == StructureCheckResult.START_PRESENT) {
                return Optional.of(new StructureHit(placement.getLocatePos(chunk), holder));
            }

            // Shutdown cancels the request before the lane blocks inside a chunk load.
            cancellationToken.throwIfCancelled();
            stats.chunkLoads++;
            ChunkAccess chunkAccess = level.getChunk(chunk.x, chunk.z, ChunkStatus.STRUCTURE_STARTS);
            cancellationToken.throwIfCancelled();
            StructureStart start = level.structureManager()
                    .getStartForStructure(SectionPos.bottomOf(chunkAccess), holder.value(), chunkAccess);
            if (start != null && start.isValid() && (!skipKnownStructures || addReference(start))) {
                return Optional.of(new StructureHit(placement.getLocatePos(start.getChunkPos()), holder));
            }
        }
        return Optional.empty();
    }

    private boolean addReference(StructureStart start) {
        synchronized (start) {
            if (start.canBeReferenced()) {
                level.structureManager().addReference(start);
                return true;
            }
            return false;
        }
    }
}
