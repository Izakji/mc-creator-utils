package com.izakji.mccreatorutils.locate.engine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;

/**
 * First matching biome position.
 *
 * @param <T> biome value type
 * @param position block position of the hit
 * @param biome matched biome holder
 * @author Izakji
 */
public record BiomeHit<T>(BlockPos position, Holder<T> biome) {}
