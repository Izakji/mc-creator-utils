package com.izakji.mccreatorutils.locate.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import net.minecraft.world.level.biome.Climate;

/**
 * Axis-aligned climate box for target biome parameter points.
 *
 * <p>The box intentionally excludes depth. Depth depends on Y and is checked by the exact
 * target-point lookup after the five height-independent axes pass.</p>
 *
 * @author Izakji
 */
public final class BiomeEnvelope {
    private static final BiomeEnvelope IMPOSSIBLE = new BiomeEnvelope(true, new long[0], new long[0]);

    private final boolean impossible;
    private final long[] minimums;
    private final long[] maximums;

    private BiomeEnvelope(boolean impossible, long[] minimums, long[] maximums) {
        this.impossible = impossible;
        this.minimums = minimums;
        this.maximums = maximums;
    }

    /**
     * Returns the empty envelope.
     *
     * @return impossible envelope
     */
    public static BiomeEnvelope impossible() {
        return IMPOSSIBLE;
    }

    /**
     * Builds the box containing all supplied parameter points.
     *
     * @param points parameter points
     * @return envelope, or the impossible envelope for an empty collection
     */
    public static BiomeEnvelope of(Collection<Climate.ParameterPoint> points) {
        if (points.isEmpty()) {
            return IMPOSSIBLE;
        }

        long[] minimums = {Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE};
        long[] maximums = {Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE};

        for (Climate.ParameterPoint point : points) {
            add(minimums, maximums, ClimateAxis.TEMPERATURE, point.temperature());
            add(minimums, maximums, ClimateAxis.HUMIDITY, point.humidity());
            add(minimums, maximums, ClimateAxis.CONTINENTALNESS, point.continentalness());
            add(minimums, maximums, ClimateAxis.EROSION, point.erosion());
            add(minimums, maximums, ClimateAxis.WEIRDNESS, point.weirdness());
        }

        return new BiomeEnvelope(false, minimums, maximums);
    }

    /**
     * Returns the smallest box containing both envelopes.
     *
     * @param first first envelope
     * @param second second envelope
     * @return union envelope
     */
    public static BiomeEnvelope union(BiomeEnvelope first, BiomeEnvelope second) {
        if (first.impossible) {
            return second;
        }
        if (second.impossible) {
            return first;
        }

        long[] minimums = new long[ClimateAxis.values().length];
        long[] maximums = new long[ClimateAxis.values().length];
        for (ClimateAxis axis : ClimateAxis.values()) {
            int index = axis.ordinal();
            minimums[index] = Math.min(first.minimums[index], second.minimums[index]);
            maximums[index] = Math.max(first.maximums[index], second.maximums[index]);
        }
        return new BiomeEnvelope(false, minimums, maximums);
    }

    /**
     * Returns whether this envelope has no axis range.
     *
     * @return true when the envelope is empty
     */
    public boolean isImpossible() {
        return impossible;
    }

    /**
     * Returns whether one axis value is inside the envelope.
     *
     * @param axis axis to test
     * @param value axis value
     * @return true when the value is inside the box
     */
    public boolean contains(ClimateAxis axis, long value) {
        if (impossible) {
            return false;
        }
        int index = axis.ordinal();
        return value >= minimums[index] && value <= maximums[index];
    }

    /**
     * Returns whether all five axes are inside the envelope.
     *
     * @param temperature temperature
     * @param humidity humidity
     * @param continentalness continentalness
     * @param erosion erosion
     * @param weirdness weirdness
     * @return true when every axis is inside the box
     */
    public boolean contains(long temperature, long humidity, long continentalness, long erosion, long weirdness) {
        if (impossible) {
            return false;
        }
        return contains(ClimateAxis.TEMPERATURE, temperature)
                && contains(ClimateAxis.HUMIDITY, humidity)
                && contains(ClimateAxis.CONTINENTALNESS, continentalness)
                && contains(ClimateAxis.EROSION, erosion)
                && contains(ClimateAxis.WEIRDNESS, weirdness);
    }

    /**
     * Returns axes from narrowest to widest interval. Fixed axes come first.
     */
    public List<ClimateAxis> selectivityOrder() {
        List<ClimateAxis> axes = new ArrayList<>(List.of(ClimateAxis.values()));
        axes.sort(Comparator.comparingLong(this::width).thenComparingInt(Enum::ordinal));
        return List.copyOf(axes);
    }

    private long width(ClimateAxis axis) {
        if (impossible) {
            return Long.MAX_VALUE;
        }
        int index = axis.ordinal();
        return maximums[index] - minimums[index];
    }

    private static void add(long[] minimums, long[] maximums, ClimateAxis axis, Climate.Parameter parameter) {
        int index = axis.ordinal();
        minimums[index] = Math.min(minimums[index], parameter.min());
        maximums[index] = Math.max(maximums[index], parameter.max());
    }
}
