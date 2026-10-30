package com.izakji.mccreatorutils.locate.engine;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Climate;

/**
 * Minecraft-facing climate sampling used by the pure biome search.
 *
 * @param <T> biome value type
 * @author Izakji
 */
public interface ClimateSampler<T> {

    /**
     * Samples the five height-independent axes at a column.
     *
     * @param blockX block x
     * @param blockZ block z
     * @return column sample
     */
    Column column(int blockX, int blockZ);

    /**
     * Resolves the biome at a climate point.
     *
     * @param point climate target point
     * @param blockX block x
     * @param blockY block y
     * @param blockZ block z
     * @return resolved biome
     */
    Holder<T> resolve(Climate.TargetPoint point, int blockX, int blockY, int blockZ);

    /** Height-independent axes plus depth at one column. */
    interface Column {
        /**
         * Returns one axis value.
         *
         * @param axis axis to read
         * @return axis value
         */
        long value(ClimateAxis axis);

        /**
         * Returns depth at a height.
         *
         * @param blockY block y
         * @return depth value
         */
        long depth(int blockY);
    }
}
