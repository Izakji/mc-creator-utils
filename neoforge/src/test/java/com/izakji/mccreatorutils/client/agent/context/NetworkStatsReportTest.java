package com.izakji.mccreatorutils.client.agent.context;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NetworkStatsReportTest {

    @Test
    void reportsPingAndTraffic() {
        NetworkDiagnostics.Sample[] samples = {
            new NetworkDiagnostics.Sample(1_000, 20, 1024, 512),
            new NetworkDiagnostics.Sample(2_000, 30, 1024, 512),
            new NetworkDiagnostics.Sample(3_000, 40, 1024, 512)
        };
        String report = NetworkStatsReport.format(new NetworkDiagnostics.Snapshot(samples));
        assertTrue(report.contains("median 30"));
        assertTrue(report.contains("jitter 10.0"));
        assertTrue(report.contains("Network 10s: rx 1.0 KiB/s, tx 0.5 KiB/s"));
    }

    @Test
    void reportsMissingPing() {
        NetworkDiagnostics.Sample[] samples = {new NetworkDiagnostics.Sample(1_000, -1, 0, 0)};
        String report = NetworkStatsReport.format(new NetworkDiagnostics.Snapshot(samples));
        assertTrue(report.contains("Ping: unavailable"));
        assertTrue(report.contains("Network traffic: unavailable"));
    }
}
