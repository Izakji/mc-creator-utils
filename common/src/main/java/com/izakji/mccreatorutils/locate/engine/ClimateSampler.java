package com.izakji.mccreatorutils.locate.engine;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Climate;

/**
 * Minecraft-facing climate sampling used by the pure biome search.
 *
 * @author Izakji
 */
public interface ClimateSampler<T> {

    Column column(int blockX, int blockZ);

    Holder<T> resolve(Climate.TargetPoint point, int blockX, int blockY, int blockZ);

    interface Column {
        long value(ClimateAxis axis);

        long depth(int blockY);
    }
}
