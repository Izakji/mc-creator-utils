package com.izakji.mccreatorutils.client.agent.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PerformanceReportSourceTest {

    @Test
    void buildsStatsChartsAndFindings() {
        FrameDiagnostics.Snapshot frames = new FrameDiagnostics.Snapshot(new FrameDiagnostics.Sample[] {
            new FrameDiagnostics.Sample(1_000, 60),
            new FrameDiagnostics.Sample(2_000, 60),
            new FrameDiagnostics.Sample(3_000, 30),
            new FrameDiagnostics.Sample(4_000, 25)
        });
        long[] times = new long[40];
        long[] durations = new long[40];
        long[] intervals = new long[40];
        for (int index = 0; index < 40; index++) {
            times[index] = 1_000 + index * 50;
            durations[index] = 1_000_000;
            intervals[index] = 50_000_000;
        }
        TickDiagnostics.Snapshot ticks = new TickDiagnostics.Snapshot(times, durations, intervals);
        SystemDiagnostics.Snapshot system = new SystemDiagnostics.Snapshot(new SystemDiagnostics.Sample[] {
            new SystemDiagnostics.Sample(1_000, 0.20, 0.30, 100L << 20, 128L << 20, 256L << 20, 32L << 20, 1, 10),
            new SystemDiagnostics.Sample(2_000, 0.25, 0.35, 120L << 20, 128L << 20, 256L << 20, 34L << 20, 2, 20)
        });
        NetworkDiagnostics.Snapshot network = new NetworkDiagnostics.Snapshot(new NetworkDiagnostics.Sample[] {
            new NetworkDiagnostics.Sample(1_000, 20, 100, 200), new NetworkDiagnostics.Sample(2_000, 40, 100, 200)
        });
        LogSummary logs = LogSummary.parse(
                "[12:00:00] [Render thread/ERROR] [com.foo/]: boom\n"
                        + "[12:00:01] [Render thread/WARN] [com.foo/]: slow\n",
                10);

        PerformanceReportData data = PerformanceReportSource.build(frames, ticks, system, network, logs);

        assertEquals(25.0, data.currentFps());
        assertEquals(43.75, data.averageFps(), 0.001);
        assertEquals(20.0, data.currentTps(), 0.001);
        assertEquals(1.0, data.p95Mspt(), 0.001);
        assertEquals(120.0, data.heapUsedMb(), 0.001);
        assertEquals(256.0, data.heapMaxMb(), 0.001);
        assertEquals(25.0, data.processCpuPercent(), 0.001);
        assertEquals(40, data.currentPing());
        assertTrue(data.tps().size() >= 2);
        assertTrue(data.notes().stream().anyMatch(note -> note.contains("low FPS")));
        assertTrue(data.notes().stream().anyMatch(note -> note.contains("1 recent ERROR")));
        assertTrue(data.summaryText().contains("Performance report opened"));
        assertTrue(data.summaryText().contains("ERROR 1"));
    }
}
