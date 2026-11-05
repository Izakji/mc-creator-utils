package com.izakji.mccreatorutils.client.agent.context;

import com.izakji.mccreatorutils.client.explore.ExploreModsConfig;

/**
 * Reads the CurseForge key without coupling agent tools to config internals.
 *
 * @author Izakji
 */
public final class CurseForgeKey {
    private CurseForgeKey() {}

    public static String current() {
        try {
            return ExploreModsConfig.curseForgeApiKey();
        } catch (LinkageError | RuntimeException unavailable) {
            return "";
        }
    }
}
