package com.izakji.mccreatorutils.locate;

import com.izakji.mccreatorutils.locate.engine.ClimateAxis;
import com.izakji.mccreatorutils.locate.engine.ClimateCoordinates;
import com.izakji.mccreatorutils.locate.engine.ClimateSampler;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Climate sampler backed by Minecraft's RandomState noise router.
 *
 * @author Izakji
 */
public final class MinecraftClimateSampler implements ClimateSampler<Biome> {
    private final Climate.Sampler sampler;
    private final BiomeResolver resolver;

    public MinecraftClimateSampler(Climate.Sampler sampler, BiomeResolver resolver) {
        this.sampler = sampler;
        this.resolver = resolver;
    }

    @Override
    public Column column(int blockX, int blockZ) {
        int sampledX = ClimateCoordinates.alignToQuart(blockX);
        int sampledZ = ClimateCoordinates.alignToQuart(blockZ);
        DensityFunction.SinglePointContext context = new DensityFunction.SinglePointContext(sampledX, 0, sampledZ);
        long[] values = new long[ClimateAxis.values().length];
        boolean[] present = new boolean[ClimateAxis.values().length];

        return new Column() {
            @Override
            public long value(ClimateAxis axis) {
                int index = axis.ordinal();
                if (!present[index]) {
                    double sampled = switch (axis) {
                        case TEMPERATURE -> sampler.temperature().compute(context);
                        case HUMIDITY -> sampler.humidity().compute(context);
                        case CONTINENTALNESS -> sampler.continentalness().compute(context);
                        case EROSION -> sampler.erosion().compute(context);
                        case WEIRDNESS -> sampler.weirdness().compute(context);
                    };
                    values[index] = Climate.quantizeCoord((float) sampled);
                    present[index] = true;
                }
                return values[index];
            }

            @Override
            public long depth(int blockY) {
                DensityFunction.SinglePointContext depthContext = new DensityFunction.SinglePointContext(
                        sampledX, ClimateCoordinates.alignToQuart(blockY), sampledZ);
                return Climate.quantizeCoord((float) sampler.depth().compute(depthContext));
            }
        };
    }

    @Override
    public Holder<Biome> resolve(Climate.TargetPoint point, int blockX, int blockY, int blockZ) {
        return resolver.resolve(point, blockX, blockY, blockZ);
    }

    @FunctionalInterface
    public interface BiomeResolver {
        Holder<Biome> resolve(Climate.TargetPoint point, int blockX, int blockY, int blockZ);
    }
}
