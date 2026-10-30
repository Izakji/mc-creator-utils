package com.izakji.mccreatorutils.locate.engine;

import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * Canonical identity of a locate request.
 *
 * @param dimension target dimension
 * @param type biome or structure
 * @param targets sorted, distinct target ids
 * @param origin search origin
 * @param radius search radius
 * @param skipKnownStructures whether already-referenced structures are skipped
 * @author Izakji
 */
public record LocateRequestKey(
        ResourceKey<Level> dimension,
        LocateType type,
        List<ResourceLocation> targets,
        BlockPos origin,
        int radius,
        boolean skipKnownStructures) {

    public LocateRequestKey {
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(targets, "targets");
        Objects.requireNonNull(origin, "origin");
        targets = targets.stream().distinct().sorted().toList();
        origin = origin.immutable();
    }
}
