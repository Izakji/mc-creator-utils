package com.izakji.mccreatorutils.client.agent.context;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TickStatsReportTest {

    @Test
    void reportsTpsAndMsptWindows() {
        long[] durations = new long[200];
        long[] intervals = new long[200];
        for (int index = 0; index < durations.length; index++) {
            durations[index] = 1_000_000;
            intervals[index] = index == 0 ? 0 : 50_000_000;
        }
        String report = TickStatsReport.format(new TickDiagnostics.Snapshot(durations, intervals));
        assertTrue(report.contains("20.00 TPS"));
        assertTrue(report.contains("MSPT avg 1.00"));
        assertTrue(report.contains("max 1.00"));
        assertTrue(report.contains("200 ticks"));
    }

    @Test
    void handlesMissingSamples() {
        assertTrue(TickStatsReport.format(TickDiagnostics.Snapshot.empty()).contains("No client tick"));
    }
}
