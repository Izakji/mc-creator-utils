package com.izakji.mccreatorutils.client.agent.context;

import java.util.Locale;

/**
 * Case-insensitive registry id filter.
 *
 * @author Izakji
 */
final class RegistrySearchFilter {
    private RegistrySearchFilter() {}

    static boolean matches(String id, String query, String namespace) {
        String normalizedId = id == null ? "" : id.toLowerCase(Locale.ROOT);
        String normalizedQuery = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        String normalizedNamespace = namespace == null ? "" : namespace.strip().toLowerCase(Locale.ROOT);
        if (!normalizedQuery.isEmpty() && !normalizedId.contains(normalizedQuery)) {
            return false;
        }
        return normalizedNamespace.isEmpty() || normalizedId.startsWith(normalizedNamespace + ":");
    }
}
