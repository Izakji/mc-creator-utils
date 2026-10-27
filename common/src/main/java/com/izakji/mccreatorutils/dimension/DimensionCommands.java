package com.izakji.mccreatorutils.dimension;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/**
 * Builds dimension teleport commands without a leading slash.
 *
 * @author Izakji
 */
public final class DimensionCommands {
    private DimensionCommands() {}

    public static String teleport(ResourceLocation dimension) {
        Objects.requireNonNull(dimension, "dimension");
        return "execute in " + dimension + " run teleport @s ~ ~ ~";
    }
}
