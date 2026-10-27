package com.izakji.mccreatorutils.locate.engine;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Climate;

final class TestClimateSampler implements ClimateSampler<String> {
    private static final Holder<String> OTHER = Holder.direct("other");

    private final Map<Long, long[]> values = new HashMap<>();
    private final Map<Long, Holder<String>> hits = new HashMap<>();
    private long defaultValue = 100;
    private int columnCalls;
    private int resolveCalls;
    private int duplicateAxisSamples;

    void fillValues(long value) {
        defaultValue = value;
    }

    void putValues(int x, int z, long... axisValues) {
        values.put(BlockPos.asLong(x, 0, z), axisValues);
    }

    void putHit(int x, int y, int z, Holder<String> holder) {
        hits.put(BlockPos.asLong(x, y, z), holder);
    }

    int columnCalls() {
        return columnCalls;
    }

    int resolveCalls() {
        return resolveCalls;
    }

    int duplicateAxisSamples() {
        return duplicateAxisSamples;
    }

    @Override
    public Column column(int blockX, int blockZ) {
        columnCalls++;
        long key = BlockPos.asLong(blockX, 0, blockZ);
        long[] source = values.getOrDefault(key, filledValues());
        boolean[] sampled = new boolean[ClimateAxis.values().length];
        return new Column() {
            @Override
            public long value(ClimateAxis axis) {
                if (sampled[axis.ordinal()]) {
                    duplicateAxisSamples++;
                }
                sampled[axis.ordinal()] = true;
                return source[axis.ordinal()];
            }

            @Override
            public long depth(int blockY) {
                return 0;
            }
        };
    }

    @Override
    public Holder<String> resolve(Climate.TargetPoint point, int blockX, int blockY, int blockZ) {
        resolveCalls++;
        return hits.getOrDefault(BlockPos.asLong(blockX, blockY, blockZ), OTHER);
    }

    private long[] filledValues() {
        return new long[] {defaultValue, defaultValue, defaultValue, defaultValue, defaultValue};
    }
}
