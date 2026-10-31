package com.izakji.mccreatorutils.client.agent.context;

import java.util.Locale;

/**
 * Registry types exposed by the registry search tool.
 *
 * @author Izakji
 */
enum RegistryKind {
    ITEM("item"),
    BLOCK("block"),
    ENTITY("entity"),
    BIOME("biome"),
    STRUCTURE("structure"),
    DIMENSION("dimension"),
    EFFECT("effect"),
    ENCHANTMENT("enchantment");

    final String id;

    RegistryKind(String id) {
        this.id = id;
    }

    static RegistryKind parse(String value) {
        String normalized = value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
        for (RegistryKind kind : values()) {
            if (kind.id.equals(normalized)) {
                return kind;
            }
        }
        return null;
    }
}
