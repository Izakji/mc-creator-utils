package com.izakji.mccreatorutils.client.agent.context;

import java.util.List;
import java.util.LongSummaryStatistics;

/**
 * Formats the recent performance samples for the agent.
 *
 * @author Izakji
 */
public final class PerformanceReport {
    private PerformanceReport() {}

    /**
     * Summarizes current and rolling FPS.
     *
     * @param samples chronological samples
     * @return compact report
     */
    public static String format(List<PerformanceSample> samples) {
        if (samples == null || samples.isEmpty()) {
            return "No performance samples are available yet.";
        }
        PerformanceSample current = samples.get(samples.size() - 1);
        LongSummaryStatistics fps =
                samples.stream().mapToLong(PerformanceSample::fps).summaryStatistics();
        long average = Math.round(fps.getAverage());
        StringBuilder report = new StringBuilder();
        report.append("Current: ")
                .append(current.fps())
                .append(" fps, memory ")
                .append(current.memoryUsedMb())
                .append('/')
                .append(current.memoryMaxMb())
                .append(" MiB, ping ")
                .append(current.ping())
                .append(" ms, render distance ")
                .append(current.renderDistance())
                .append('\n');
        report.append("Last ")
                .append(samples.size())
                .append("s FPS: avg ")
                .append(average)
                .append(", min ")
                .append(fps.getMin())
                .append(", max ")
                .append(fps.getMax())
                .append('\n');
        report.append("Recent FPS: ");
        int start = Math.max(0, samples.size() - 10);
        for (int index = start; index < samples.size(); index++) {
            if (index > start) {
                report.append(", ");
            }
            report.append(samples.get(index).fps());
        }
        report.append('\n').append("For a suspected drop, inspect logs_tail or search_logs for errors.");
        return report.toString();
    }
}
