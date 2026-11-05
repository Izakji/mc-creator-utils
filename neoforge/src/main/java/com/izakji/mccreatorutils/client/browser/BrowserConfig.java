package com.izakji.mccreatorutils.client.browser;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

/**
 * Client configuration for the in-game browser.
 *
 * @author Izakji
 */
public final class BrowserConfig {
    public static final ModConfigSpec SPEC;
    private static final ConfigValue<Boolean> BLOCK_ADS;

    static {
        Builder builder = new Builder();
        builder.push("browser");
        BLOCK_ADS = builder.comment("Block known advertising and tracking requests in the in-game browser. "
                        + "Extra patterns can be added to config/mccreatorutils/browser-blocklist.txt.")
                .define("blockAds", true);
        builder.pop();
        SPEC = builder.build();
    }

    private BrowserConfig() {}

    /**
     * Returns whether browser ad blocking is enabled.
     *
     * @return true when enabled; defaults to true before config load
     */
    public static boolean blockAds() {
        try {
            return BLOCK_ADS.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }
}
