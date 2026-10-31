package com.izakji.mccreatorutils.client.agent.context;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SystemStatsReportTest {

    @Test
    void reportsCpuMemoryAndGcDeltas() {
        SystemDiagnostics.Sample[] samples = {
            new SystemDiagnostics.Sample(1_000, 0.10, 0.20, 100L << 20, 128L << 20, 256L << 20, 32L << 20, 10, 100),
            new SystemDiagnostics.Sample(2_000, 0.30, 0.40, 120L << 20, 128L << 20, 256L << 20, 34L << 20, 12, 140)
        };
        String report = SystemStatsReport.format(new SystemDiagnostics.Snapshot(samples));
        assertTrue(report.contains("heap 120 / 256 MiB (46.9%)"));
        assertTrue(report.contains("GC 10s: 2 collections, 40 ms total pause, 20.00 ms avg pause"));
        assertTrue(report.contains("CPU process (10s/1m/5m avg): 20.0%"));
    }

    @Test
    void handlesMissingSamples() {
        assertTrue(SystemStatsReport.format(SystemDiagnostics.Snapshot.empty()).contains("No system samples"));
    }
}
