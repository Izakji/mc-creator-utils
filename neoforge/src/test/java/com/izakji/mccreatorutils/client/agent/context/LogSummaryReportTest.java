package com.izakji.mccreatorutils.client.agent.context;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LogSummaryReportTest {

    @Test
    void countsLevelsGroupsRepeatsAndShowsNewestStack() {
        String log = """
                [12:00:00] [Render thread/INFO] [com.foo/]: started
                [12:00:01] [Render thread/WARN] [com.foo/]: slow chunk 123
                [12:00:02] [Render thread/WARN] [com.foo/]: slow chunk 456
                [12:00:03] [Render thread/ERROR] [com.foo/]: boom
                \tat com.foo.Bar.baz(Bar.java:10)
                \tat com.foo.Bar.qux(Bar.java:20)
                [12:00:04] [Render thread/INFO] [com.foo/]: done
                """;
        String report = LogSummaryReport.format(log, 10);
        assertTrue(report.contains("FATAL 0, ERROR 1, WARN 2"));
        assertTrue(report.contains("2x WARN: slow chunk #"));
        assertTrue(report.contains("Newest problem:"));
        assertTrue(report.contains("at com.foo.Bar.baz"));
    }

    @Test
    void reportsCleanLog() {
        String report = LogSummaryReport.format("[12:00:00] [Render thread/INFO] [com.foo/]: ok\n", 10);
        assertTrue(report.contains("No ERROR or WARN lines"));
    }
}
