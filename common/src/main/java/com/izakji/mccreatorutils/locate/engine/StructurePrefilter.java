package com.izakji.mccreatorutils.locate.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * Samples a conservative area around a structure candidate before the expensive structure check.
 *
 * @author Izakji
 */
public final class StructurePrefilter<T> implements StructureBiomeFilter {
    private final BiomeEnvelopeProvider<T> provider;
    private final ClimateSampler<T> sampler;
    private final Function<Holder<Structure>, Set<Holder<T>>> biomes;
    private final int marginChunks;
    private final Map<Holder<Structure>, BiomeEnvelopeProvider.EnvelopeLookup<T>> lookups =
            new HashMap<>();

    public StructurePrefilter(
            BiomeEnvelopeProvider<T> provider,
            ClimateSampler<T> sampler,
            Function<Holder<Structure>, Set<Holder<T>>> biomes,
            int marginChunks) {
        this.provider = provider;
        this.sampler = sampler;
        this.biomes = biomes;
        this.marginChunks = Math.max(0, marginChunks);
    }

    public static <T> StructureBiomeFilter disabled() {
        return (structure, chunk) -> true;
    }

    @Override
    public boolean mayMatch(Holder<Structure> structure, ChunkPos chunk) {
        Set<Holder<T>> targetBiomes = biomes.apply(structure);
        if (targetBiomes.isEmpty()) {
            return true;
        }

        BiomeEnvelopeProvider.EnvelopeLookup<T> lookup = lookups.computeIfAbsent(
                structure, ignored -> provider.forTargets(targetBiomes));
        int marginBlocks = marginChunks * 16;
        int minQuartX = QuartPos.fromBlock(chunk.getMinBlockX() - marginBlocks);
        int maxQuartX = QuartPos.fromBlock(chunk.getMinBlockX() + 15 + marginBlocks);
        int minQuartZ = QuartPos.fromBlock(chunk.getMinBlockZ() - marginBlocks);
        int maxQuartZ = QuartPos.fromBlock(chunk.getMinBlockZ() + 15 + marginBlocks);

        for (int quartX = minQuartX; quartX <= maxQuartX; quartX++) {
            for (int quartZ = minQuartZ; quartZ <= maxQuartZ; quartZ++) {
                Optional<BiomeEnvelope> envelope = lookup.at(quartX, quartZ);
                if (envelope.isEmpty()) {
                    return true;
                }
                if (envelope.get().isImpossible()) {
                    continue;
                }

                ClimateSampler.Column column = sampler.column(QuartPos.toBlock(quartX), QuartPos.toBlock(quartZ));
                boolean inside = true;
                for (ClimateAxis axis : ClimateAxis.values()) {
                    if (!envelope.get().contains(axis, column.value(axis))) {
                        inside = false;
                        break;
                    }
                }
                if (inside) {
                    return true;
                }
            }
        }

        return false;
    }
}
