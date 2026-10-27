package com.izakji.mccreatorutils.locate.engine;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;

/**
 * Inputs for one pure biome search.
 *
 * @author Izakji
 */
public record BiomeSearchInput<T>(
        BlockPos origin,
        int radius,
        int horizontalStep,
        int verticalStep,
        int minBuildHeight,
        int maxBuildHeight,
        Set<Holder<T>> targets,
        BiomeEnvelopeProvider.EnvelopeLookup<T> envelopes,
        ClimateSampler<T> sampler,
        LocateStats stats,
        CancellationToken cancellationToken) {}
