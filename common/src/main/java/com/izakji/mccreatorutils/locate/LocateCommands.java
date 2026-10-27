package com.izakji.mccreatorutils.locate;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/**
 * Builds vanilla locate command strings without a leading slash.
 *
 * @author Izakji
 */
public final class LocateCommands {
    private LocateCommands() {}

    public static String biome(ResourceLocation biome) {
        Objects.requireNonNull(biome, "biome");
        return "locate biome " + biome;
    }

    public static String structure(ResourceLocation structure) {
        Objects.requireNonNull(structure, "structure");
        return "locate structure " + structure;
    }
}
