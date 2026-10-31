package com.izakji.mccreatorutils.client.agent.context;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Formats ping and Minecraft connection traffic history for the agent.
 *
 * @author Izakji
 */
public final class NetworkStatsReport {
    private NetworkStatsReport() {}

    public static String format(NetworkDiagnostics.Snapshot snapshot) {
        if (snapshot == null || snapshot.size() == 0) {
            return "No network samples are available yet. Wait a few seconds and try again.";
        }
        NetworkDiagnostics.Sample[] samples = snapshot.samples();
        List<Long> pings = new ArrayList<>();
        boolean anyTraffic = false;
        for (NetworkDiagnostics.Sample sample : samples) {
            if (sample.pingMs() >= 0) {
                pings.add((long) sample.pingMs());
            }
            anyTraffic |= sample.receivedBytesPerSecond() > 0 || sample.sentBytesPerSecond() > 0;
        }
        StringBuilder report = new StringBuilder();
        appendPing(report, samples, pings);
        if (anyTraffic) {
            appendRate(report, "10s", samples, 10);
            appendRate(report, "1m", samples, 60);
            appendRate(report, "5m", samples, 300);
        } else {
            report.append(
                    "Network traffic: unavailable on this connection (local/singleplayer or counters not attached).\n");
        }
        report.append("Samples: ").append(samples.length).append(" (one per second).");
        return report.toString();
    }

    private static void appendPing(StringBuilder report, NetworkDiagnostics.Sample[] samples, List<Long> pings) {
        NetworkDiagnostics.Sample current = samples[samples.length - 1];
        if (pings.isEmpty()) {
            report.append("Ping: unavailable (no server connection).\n");
            return;
        }
        long[] values = pings.stream().mapToLong(Long::longValue).toArray();
        NumberStats stats = NumberStats.of(values);
        report.append("Ping: current ")
                .append(current.pingMs() < 0 ? "n/a" : current.pingMs() + " ms")
                .append(", avg ")
                .append(String.format(Locale.ROOT, "%.1f", stats.average()))
                .append(" ms, min ")
                .append(stats.min())
                .append(", median ")
                .append(stats.median())
                .append(", p95 ")
                .append(stats.p95())
                .append(", max ")
                .append(stats.max())
                .append(", jitter ")
                .append(String.format(Locale.ROOT, "%.1f", jitter(values)))
                .append(" ms\n");
    }

    private static double jitter(long[] values) {
        if (values.length < 2) {
            return 0;
        }
        long total = 0;
        for (int index = 1; index < values.length; index++) {
            total += Math.abs(values[index] - values[index - 1]);
        }
        return (double) total / (values.length - 1);
    }

    private static void appendRate(
            StringBuilder report, String label, NetworkDiagnostics.Sample[] samples, int window) {
        int start = Math.max(0, samples.length - window);
        long received = 0;
        long sent = 0;
        for (int index = start; index < samples.length; index++) {
            received += samples[index].receivedBytesPerSecond();
            sent += samples[index].sentBytesPerSecond();
        }
        int count = samples.length - start;
        report.append("Network ")
                .append(label)
                .append(": rx ")
                .append(rate(received, count))
                .append(", tx ")
                .append(rate(sent, count))
                .append('\n');
    }

    private static String rate(long totalBytes, int samples) {
        if (samples == 0) {
            return "n/a";
        }
        double kibibytes = totalBytes / (double) samples / 1024.0;
        return String.format(Locale.ROOT, "%.1f KiB/s", kibibytes);
    }
}
