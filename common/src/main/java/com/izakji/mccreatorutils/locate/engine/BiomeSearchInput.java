package com.izakji.mccreatorutils.locate.engine;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;

/**
 * Inputs for one pure biome search.
 *
 * @param <T> biome value type
 * @param origin search origin
 * @param radius search radius in blocks
 * @param horizontalStep horizontal sample step in blocks
 * @param verticalStep vertical sample step in blocks
 * @param minBuildHeight minimum world height
 * @param maxBuildHeight maximum world height
 * @param targets biome targets
 * @param envelopes safe-pruning lookup for the targets
 * @param sampler climate sampler
 * @param stats mutable search counters
 * @param cancellationToken cooperative cancellation signal
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
