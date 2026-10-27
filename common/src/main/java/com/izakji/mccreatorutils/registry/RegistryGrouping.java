package com.izakji.mccreatorutils.registry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.resources.ResourceLocation;

/**
 * Groups registry resource locations by namespace.
 *
 * @author Izakji
 */
public final class RegistryGrouping {
    private static final Comparator<String> NAMESPACE_ORDER = Comparator.comparing(
                    (String namespace) -> !namespace.equals("minecraft"))
            .thenComparing(Comparator.naturalOrder());

    private RegistryGrouping() {}

    public static List<RegistryGroup> group(Collection<ResourceLocation> ids) {
        Map<String, List<ResourceLocation>> byNamespace = new TreeMap<>(NAMESPACE_ORDER);
        for (ResourceLocation id : ids) {
            byNamespace.computeIfAbsent(id.getNamespace(), key -> new ArrayList<>()).add(id);
        }

        List<RegistryGroup> groups = new ArrayList<>(byNamespace.size());
        for (Map.Entry<String, List<ResourceLocation>> entry : byNamespace.entrySet()) {
            List<ResourceLocation> entries = new ArrayList<>(entry.getValue());
            entries.sort(Comparator.comparing(ResourceLocation::getPath));
            groups.add(new RegistryGroup(entry.getKey(), entries));
        }
        return List.copyOf(groups);
    }
}
