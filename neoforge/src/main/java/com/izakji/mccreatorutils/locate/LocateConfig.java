package com.izakji.mccreatorutils.locate;

import com.izakji.mccreatorutils.locate.engine.LocateSettings;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

/**
 * Server configuration for the asynchronous locate engine.
 *
 * @author Izakji
 */
public final class LocateConfig {
    public static final ModConfigSpec SPEC;

    private static final ConfigValue<Boolean> ENABLED;
    private static final ConfigValue<Integer> BIOME_THREADS;
    private static final ConfigValue<Integer> MAX_QUEUED_SEARCHES;
    private static final ConfigValue<Integer> TIMEOUT_SECONDS;
    private static final ConfigValue<Boolean> STRUCTURE_PREFILTER;
    private static final ConfigValue<Integer> STRUCTURE_PREFILTER_MARGIN;
    private static final ConfigValue<Integer> RESULT_CACHE_SECONDS;
    private static final ConfigValue<Boolean> DEBUG_STATS;

    static {
        Builder builder = new Builder();
        builder.push("locate");
        ENABLED = builder.comment("Enable asynchronous player /locate interception.")
                .define("enabled", true);
        BIOME_THREADS = builder.comment("Maximum concurrent biome searches.")
                .defineInRange("biomeThreads", 2, 1, 4);
        MAX_QUEUED_SEARCHES = builder.comment("Maximum searches waiting for a worker or structure lane.")
                .defineInRange("maxQueuedSearches", 32, 0, 256);
        TIMEOUT_SECONDS = builder.comment("Per-request locate deadline in seconds.")
                .defineInRange("timeoutSeconds", 60, 5, 600);
        STRUCTURE_PREFILTER = builder.comment("Enable structure-biome prefiltering.")
                .define("structurePrefilter", true);
        STRUCTURE_PREFILTER_MARGIN = builder.comment("Structure prefilter sampling margin in chunks.")
                .defineInRange("structurePrefilterMarginChunks", 1, 0, 3);
        RESULT_CACHE_SECONDS = builder.comment("Successful or completed not-found result cache lifetime.")
                .defineInRange("resultCacheSeconds", 30, 0, 300);
        DEBUG_STATS = builder.comment("Log per-request locate counters.")
                .define("debugStats", false);
        builder.pop();
        SPEC = builder.build();
    }

    private LocateConfig() {}

    public static LocateSettings settings() {
        try {
            return new LocateSettings(
                    ENABLED.get(),
                    BIOME_THREADS.get(),
                    MAX_QUEUED_SEARCHES.get(),
                    TIMEOUT_SECONDS.get(),
                    STRUCTURE_PREFILTER.get(),
                    STRUCTURE_PREFILTER_MARGIN.get(),
                    RESULT_CACHE_SECONDS.get(),
                    DEBUG_STATS.get());
        } catch (IllegalStateException notLoaded) {
            return LocateSettings.defaults();
        }
    }
}
