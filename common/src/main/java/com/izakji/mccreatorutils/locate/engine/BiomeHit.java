package com.izakji.mccreatorutils.locate.engine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;

/**
 * First matching biome position.
 *
 * @author Izakji
 */
public record BiomeHit<T>(BlockPos position, Holder<T> biome) {}
