package com.izakji.mccreatorutils.client.agent.context;

import java.util.Locale;

/**
 * Formats CPU, heap, and GC history for the agent.
 *
 * @author Izakji
 */
public final class SystemStatsReport {
    private SystemStatsReport() {}

    public static String format(SystemDiagnostics.Snapshot snapshot) {
        if (snapshot == null || snapshot.size() == 0) {
            return "No system samples are available yet. Wait a few seconds and try again.";
        }
        SystemDiagnostics.Sample[] samples = snapshot.samples();
        SystemDiagnostics.Sample current = samples[samples.length - 1];
        StringBuilder report = new StringBuilder();
        report.append("Memory: heap ")
                .append(mib(current.heapUsed()))
                .append(" / ")
                .append(mib(current.heapMax()))
                .append(" MiB (")
                .append(percent(current.heapUsed(), current.heapMax()))
                .append("), committed ")
                .append(mib(current.heapCommitted()))
                .append(" MiB, non-heap ")
                .append(mib(current.nonHeapUsed()))
                .append(" MiB\n");
        report.append("CPU process (10s/1m/5m avg): ")
                .append(cpuAverages(samples, true))
                .append('\n');
        report.append("CPU system (10s/1m/5m avg): ")
                .append(cpuAverages(samples, false))
                .append('\n');
        gcWindow(report, samples, "10s", 10);
        gcWindow(report, samples, "1m", 60);
        gcWindow(report, samples, "5m", 300);
        report.append("Samples: ").append(samples.length).append(" (one per second).");
        return report.toString();
    }

    private static String cpuAverages(SystemDiagnostics.Sample[] samples, boolean process) {
        String ten = cpuAverage(samples, 10, process);
        String minute = cpuAverage(samples, 60, process);
        String five = cpuAverage(samples, 300, process);
        return ten + ", " + minute + ", " + five;
    }

    private static String cpuAverage(SystemDiagnostics.Sample[] samples, int window, boolean process) {
        int start = Math.max(0, samples.length - window);
        double total = 0;
        int count = 0;
        for (int index = start; index < samples.length; index++) {
            double value = process ? samples[index].processCpu() : samples[index].systemCpu();
            if (value >= 0) {
                total += value;
                count++;
            }
        }
        return count == 0 ? "n/a" : String.format(Locale.ROOT, "%.1f%%", total * 100.0 / count);
    }

    private static void gcWindow(StringBuilder report, SystemDiagnostics.Sample[] samples, String label, int window) {
        int start = Math.max(0, samples.length - window);
        SystemDiagnostics.Sample first = samples[start];
        SystemDiagnostics.Sample last = samples[samples.length - 1];
        if (first.gcCount() < 0 || last.gcCount() < 0 || last.gcTimeMs() < 0) {
            report.append("GC ").append(label).append(": unavailable\n");
            return;
        }
        long collections = Math.max(0, last.gcCount() - first.gcCount());
        long pauseMs = Math.max(0, last.gcTimeMs() - first.gcTimeMs());
        double averagePause = collections == 0 ? 0 : (double) pauseMs / collections;
        report.append("GC ")
                .append(label)
                .append(": ")
                .append(collections)
                .append(" collections, ")
                .append(pauseMs)
                .append(" ms total pause, ")
                .append(String.format(Locale.ROOT, "%.2f", averagePause))
                .append(" ms avg pause\n");
    }

    private static String mib(long bytes) {
        if (bytes < 0) {
            return "unlimited";
        }
        return Long.toString(bytes / (1024 * 1024));
    }

    private static String percent(long used, long max) {
        if (max <= 0) {
            return "n/a";
        }
        return String.format(Locale.ROOT, "%.1f%%", used * 100.0 / max);
    }
}
