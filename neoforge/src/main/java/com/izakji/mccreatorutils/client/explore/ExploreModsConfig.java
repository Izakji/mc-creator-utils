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
    public static final String CURSEFORGE_API_KEY_ENV = "CURSEFORGE_API_KEY";
    public static final ModConfigSpec SPEC;

    private static final ConfigValue<String> CURSEFORGE_API_KEY;

    static {
        Builder builder = new Builder();
        builder.push("exploreMods");
        CURSEFORGE_API_KEY = builder.comment(
                        "Optional CurseForge API key used to prefer CurseForge pages. Client-only; never logged. "
                                + "The CURSEFORGE_API_KEY environment variable takes precedence. "
                                + "Leave both blank to use metadata and Modrinth only.")
                .define("curseForgeApiKey", "");
        builder.pop();
        SPEC = builder.build();
    }

    private ExploreModsConfig() {}

    /**
     * Returns the configured CurseForge API key, or an empty string when unavailable.
     *
     * <p>The {@code CURSEFORGE_API_KEY} environment variable takes precedence over the client
     * config so normal users can keep the secret outside all Minecraft config files.</p>
     *
     * @return trimmed API key, never null
     */
    public static String curseForgeApiKey() {
        return ExploreModsSecrets.resolve(System.getenv(CURSEFORGE_API_KEY_ENV), configuredKey());
    }

    private static String configuredKey() {
        try {
            String key = CURSEFORGE_API_KEY.get();
            return key == null ? "" : key;
        } catch (IllegalStateException notLoaded) {
            return "";
        }
    }
}
