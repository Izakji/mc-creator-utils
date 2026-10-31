package com.izakji.mccreatorutils.client.agent.context;

import java.util.Locale;

/**
 * Formats bounded tick timing history for the agent.
 *
 * @author Izakji
 */
public final class TickStatsReport {
    private static final int TICKS_10_SECONDS = 200;
    private static final int TICKS_1_MINUTE = 1200;
    private static final int TICKS_5_MINUTES = 6000;

    private TickStatsReport() {}

    public static String format(TickDiagnostics.Snapshot snapshot) {
        if (snapshot == null || snapshot.size() == 0) {
            return "No client tick samples are available yet.";
        }
        StringBuilder report = new StringBuilder();
        report.append("Client tick: ").append(snapshot.size()).append(" samples available.\n");
        window(report, "10s", snapshot, TICKS_10_SECONDS);
        window(report, "1m", snapshot, TICKS_1_MINUTE);
        window(report, "5m", snapshot, TICKS_5_MINUTES);
        report.append("MSPT measures client tick processing time; server tick time is not visible to the client.");
        return report.toString();
    }

    private static void window(StringBuilder report, String label, TickDiagnostics.Snapshot snapshot, int ticks) {
        int available = snapshot.size();
        int start = Math.max(0, available - ticks);
        int length = available - start;
        long[] durations = new long[length];
        System.arraycopy(snapshot.durations(), start, durations, 0, length);
        NumberStats stats = NumberStats.of(durations);
        if (stats.empty()) {
            return;
        }
        report.append("- ")
                .append(label)
                .append(": ")
                .append(formatTps(snapshot.intervals(), start, available))
                .append(" TPS, MSPT avg ")
                .append(ms(stats.average()))
                .append(", min ")
                .append(ms(stats.min()))
                .append(", median ")
                .append(ms(stats.median()))
                .append(", p95 ")
                .append(ms(stats.p95()))
                .append(", max ")
                .append(ms(stats.max()))
                .append(" (")
                .append(stats.count())
                .append(" ticks)\n");
    }

    private static String formatTps(long[] intervals, int start, int end) {
        long total = 0;
        int samples = 0;
        for (int index = Math.max(start, 1); index < end; index++) {
            long interval = intervals[index];
            if (interval > 0) {
                total += interval;
                samples++;
            }
        }
        if (samples == 0 || total == 0) {
            return "n/a";
        }
        double tps = samples * 1_000_000_000.0 / total;
        return String.format(Locale.ROOT, "%.2f", tps);
    }

    private static String ms(double nanos) {
        return String.format(Locale.ROOT, "%.2f", nanos / 1_000_000.0);
    }

    private static String ms(long nanos) {
        return String.format(Locale.ROOT, "%.2f", nanos / 1_000_000.0);
    }
}
