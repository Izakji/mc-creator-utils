package com.izakji.mccreatorutils.client.agent.context;

import java.util.List;
import java.util.Locale;

/**
 * Immutable snapshot used by the visual performance report.
 *
 * @param generatedAt epoch milliseconds
 * @param fps FPS history
 * @param tps TPS history
 * @param mspt MSPT history
 * @param heap heap-used history in MiB
 * @param ping ping history in milliseconds
 * @param currentFps current FPS, or -1
 * @param averageFps average FPS, or -1
 * @param lowFps 1% low FPS, or -1
 * @param currentTps current TPS, or -1
 * @param p95Mspt 95th percentile MSPT, or -1
 * @param heapUsedMb current heap use in MiB, or -1
 * @param heapMaxMb maximum heap in MiB, or -1
 * @param processCpuPercent current process CPU percent, or -1
 * @param currentPing current ping in milliseconds, or -1
 * @param logs structured log summary
 * @param notes automatic health findings
 * @author Izakji
 */
public record PerformanceReportData(
        long generatedAt,
        List<ReportPoint> fps,
        List<ReportPoint> tps,
        List<ReportPoint> mspt,
        List<ReportPoint> heap,
        List<ReportPoint> ping,
        double currentFps,
        double averageFps,
        double lowFps,
        double currentTps,
        double p95Mspt,
        double heapUsedMb,
        double heapMaxMb,
        double processCpuPercent,
        int currentPing,
        LogSummary logs,
        List<String> notes) {

    public PerformanceReportData {
        fps = List.copyOf(fps);
        tps = List.copyOf(tps);
        mspt = List.copyOf(mspt);
        heap = List.copyOf(heap);
        ping = List.copyOf(ping);
        notes = List.copyOf(notes);
    }

    /**
     * Builds the compact text returned to the model.
     *
     * @return one-paragraph summary
     */
    public String summaryText() {
        StringBuilder text = new StringBuilder();
        text.append("Performance report opened. FPS ")
                .append(number(currentFps))
                .append(" (avg ")
                .append(number(averageFps))
                .append(", 1% low ")
                .append(number(lowFps))
                .append("); TPS ")
                .append(number(currentTps))
                .append("; MSPT p95 ")
                .append(number(p95Mspt))
                .append(" ms; heap ")
                .append(number(heapUsedMb))
                .append('/')
                .append(number(heapMaxMb))
                .append(" MiB; CPU ")
                .append(number(processCpuPercent))
                .append("%; ping ")
                .append(currentPing < 0 ? "n/a" : currentPing + " ms")
                .append("; log FATAL ")
                .append(logs.fatal())
                .append(", ERROR ")
                .append(logs.errors())
                .append(", WARN ")
                .append(logs.warnings())
                .append('.');
        if (!notes.isEmpty()) {
            text.append(" Findings: ").append(String.join("; ", notes)).append('.');
        }
        return text.toString();
    }

    private static String number(double value) {
        if (value < 0) {
            return "n/a";
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
