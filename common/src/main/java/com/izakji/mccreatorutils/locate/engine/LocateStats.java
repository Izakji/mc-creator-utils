package com.izakji.mccreatorutils.locate.engine;

/**
 * Per-request counters used by debug logging.
 *
 * <p>A request has one writer thread, so plain fields are sufficient.</p>
 *
 * @author Izakji
 */
public final class LocateStats {
    public long columns;
    public long envelopeRejects;
    public long exactBiomeLookups;
    public long candidates;
    public long prefilterSkips;
    public long presenceChecks;
    public long chunkLoads;

    public String summary() {
        return "columns=" + columns
                + " envelopeRejects=" + envelopeRejects
                + " exactBiomeLookups=" + exactBiomeLookups
                + " candidates=" + candidates
                + " prefilterSkips=" + prefilterSkips
                + " presenceChecks=" + presenceChecks
                + " chunkLoads=" + chunkLoads;
    }
}
