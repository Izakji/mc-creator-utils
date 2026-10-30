package com.izakji.agent.plugin;

import java.util.List;

/**
 * Result of loading declarative packs.
 *
 * @param packs loaded packs
 * @param errors human-readable load errors
 * @author Izakji
 */
public record PackLoadResult(List<PluginPack> packs, List<String> errors) {
    public PackLoadResult {
        packs = List.copyOf(packs);
        errors = List.copyOf(errors);
    }
}
