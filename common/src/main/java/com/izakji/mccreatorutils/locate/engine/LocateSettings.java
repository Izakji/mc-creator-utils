package com.izakji.mccreatorutils.locate.engine;

/**
 * Server-side values that control locate admission and algorithm behavior.
 *
 * @author Izakji
 */
public record LocateSettings(
        boolean enabled,
        int biomeThreads,
        int maxQueuedSearches,
        int timeoutSeconds,
        boolean structurePrefilter,
        int structurePrefilterMarginChunks,
        int resultCacheSeconds,
        boolean debugStats) {

    public static LocateSettings defaults() {
        return new LocateSettings(true, 2, 32, 60, false, 1, 30, false);
    }
}
