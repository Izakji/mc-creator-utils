package com.izakji.mccreatorutils.locate.engine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * One located structure.
 *
 * @author Izakji
 */
public record StructureHit(BlockPos position, Holder<Structure> structure) {}
