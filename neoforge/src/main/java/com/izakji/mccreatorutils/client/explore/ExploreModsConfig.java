package com.izakji.mccreatorutils.client.explore;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

/**
 * Client configuration for the Explore Mods tool.
 *
 * @author Izakji
 */
public final class ExploreModsConfig {
    public static final ModConfigSpec SPEC;

    private static final ConfigValue<String> CURSEFORGE_API_KEY;

    static {
        Builder builder = new Builder();
        builder.push("exploreMods");
        CURSEFORGE_API_KEY = builder.comment(
                        "Optional CurseForge API key used to prefer CurseForge pages. Client-only; never logged. Leave blank to use metadata and Modrinth only.")
                .define("curseForgeApiKey", "");
        builder.pop();
        SPEC = builder.build();
    }

    private ExploreModsConfig() {}

    /**
     * Returns the configured CurseForge API key, or an empty string when unavailable.
     *
     * @return trimmed API key, never null
     */
    public static String curseForgeApiKey() {
        try {
            String key = CURSEFORGE_API_KEY.get();
            return key == null ? "" : key.trim();
        } catch (IllegalStateException notLoaded) {
            return "";
        }
    }
}
