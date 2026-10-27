package com.izakji.mccreatorutils.locate.engine;

import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * Cheap necessary-condition filter for structure-biome checks.
 *
 * @author Izakji
 */
@FunctionalInterface
public interface StructureBiomeFilter {

    boolean mayMatch(Holder<Structure> structure, ChunkPos chunk);
}
