package com.izakji.mccreatorutils.client.agent.context;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class PerformanceReportTest {

    @Test
    void summarizesCurrentAndHistoricalFps() {
        String report = PerformanceReport.format(List.of(
                new PerformanceSample(1_000, 60, 512, 4096, 20, 12),
                new PerformanceSample(2_000, 20, 600, 4096, 30, 12),
                new PerformanceSample(3_000, 40, 550, 4096, 25, 12)));
        assertTrue(report.contains("Current: 40 fps"));
        assertTrue(report.contains("avg 40"));
        assertTrue(report.contains("min 20"));
        assertTrue(report.contains("max 60"));
        assertTrue(report.contains("memory 550/4096 MiB"));
    }

    @Test
    void handlesNoSamples() {
        assertTrue(PerformanceReport.format(List.of()).contains("No performance samples"));
    }
}
