package com.izakji.mccreatorutils.locate.engine;

/**
 * Server-side values that control locate admission and algorithm behavior.
 *
 * @param enabled whether async locate replaces vanilla locate
 * @param biomeThreads number of biome worker threads
 * @param maxQueuedSearches maximum queued searches
 * @param timeoutSeconds request timeout in seconds
 * @param structurePrefilter whether structure candidates are pruned by biome envelope
 * @param structurePrefilterMarginChunks margin added to the structure biome test
 * @param resultCacheSeconds result cache lifetime in seconds
 * @param debugStats whether per-request statistics are logged
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
