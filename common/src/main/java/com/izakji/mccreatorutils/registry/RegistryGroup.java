package com.izakji.mccreatorutils.registry;

import java.util.List;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/**
 * Registry entries belonging to one namespace.
 *
 * @param namespace registry namespace, usually the mod id
 * @param entries sorted resource locations in that namespace
 * @author Izakji
 */
public record RegistryGroup(String namespace, List<ResourceLocation> entries) {
    public RegistryGroup {
        Objects.requireNonNull(namespace, "namespace");
        entries = List.copyOf(entries);
    }
}
