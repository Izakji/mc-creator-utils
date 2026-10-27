package com.izakji.mccreatorutils.locate.engine;

import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Climate;

/**
 * Pure vanilla-order biome search with climate-envelope pruning.
 *
 * @author Izakji
 */
public final class BiomeSearch {
    private BiomeSearch() {}

    public static <T> Optional<BiomeHit<T>> findClosest(BiomeSearchInput<T> input) {
        Set<Holder<T>> targets = input.targets();
        if (targets.isEmpty()) {
            return Optional.empty();
        }

        int spiralRadius = Math.floorDiv(input.radius(), input.horizontalStep());
        int[] heights = Mth.outFromOrigin(
                        input.origin().getY(), input.minBuildHeight() + 1, input.maxBuildHeight(), input.verticalStep())
                .toArray();

        for (BlockPos.MutableBlockPos offset :
                BlockPos.spiralAround(BlockPos.ZERO, spiralRadius, Direction.EAST, Direction.SOUTH)) {
            input.cancellationToken().throwIfCancelled();
            input.stats().columns++;

            int x = input.origin().getX() + offset.getX() * input.horizontalStep();
            int z = input.origin().getZ() + offset.getZ() * input.horizontalStep();
            int quartX = QuartPos.fromBlock(x);
            int quartZ = QuartPos.fromBlock(z);
            Optional<BiomeEnvelope> envelopeResult = input.envelopes().at(quartX, quartZ);
            if (envelopeResult.isPresent() && envelopeResult.get().isImpossible()) {
                input.stats().envelopeRejects++;
                continue;
            }

            ClimateSampler.Column column = input.sampler().column(x, z);
            ColumnCache cache = new ColumnCache(column);
            if (envelopeResult.isPresent()) {
                BiomeEnvelope envelope = envelopeResult.get();
                boolean outside = false;
                for (ClimateAxis axis : envelope.selectivityOrder()) {
                    if (!envelope.contains(axis, cache.value(axis))) {
                        outside = true;
                        break;
                    }
                }
                if (outside) {
                    input.stats().envelopeRejects++;
                    continue;
                }
            }

            for (int y : heights) {
                Climate.TargetPoint point = new Climate.TargetPoint(
                        cache.value(ClimateAxis.TEMPERATURE),
                        cache.value(ClimateAxis.HUMIDITY),
                        cache.value(ClimateAxis.CONTINENTALNESS),
                        cache.value(ClimateAxis.EROSION),
                        column.depth(y),
                        cache.value(ClimateAxis.WEIRDNESS));

                input.stats().exactBiomeLookups++;
                Holder<T> holder = input.sampler().resolve(point, x, y, z);
                if (targets.contains(holder)) {
                    return Optional.of(new BiomeHit<>(new BlockPos(x, y, z), holder));
                }
            }
        }

        return Optional.empty();
    }

    private static final class ColumnCache {
        private final ClimateSampler.Column column;
        private final long[] values = new long[ClimateAxis.values().length];
        private final boolean[] present = new boolean[ClimateAxis.values().length];

        private ColumnCache(ClimateSampler.Column column) {
            this.column = column;
        }

        private long value(ClimateAxis axis) {
            int index = axis.ordinal();
            if (!present[index]) {
                values[index] = column.value(axis);
                present[index] = true;
            }
            return values[index];
        }
    }
}
