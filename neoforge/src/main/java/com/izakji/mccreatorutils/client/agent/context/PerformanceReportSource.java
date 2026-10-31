package com.izakji.mccreatorutils.client.agent.context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * Builds visual report data from the bounded client diagnostics samplers.
 *
 * @author Izakji
 */
public final class PerformanceReportSource {
    private static final int MAX_CHART_POINTS = 300;
    private static final double MIB = 1024.0 * 1024.0;

    private PerformanceReportSource() {}

    public static CompletableFuture<PerformanceReportData> build(ClientLogReader reader) {
        FrameDiagnostics.Snapshot frames = FrameDiagnostics.instance().snapshot();
        TickDiagnostics.Snapshot ticks = TickDiagnostics.instance().snapshot();
        SystemDiagnostics.Snapshot system = SystemDiagnostics.instance().snapshot();
        NetworkDiagnostics.Snapshot network = NetworkDiagnostics.instance().snapshot();
        return reader.summaryData(10).thenApply(logs -> build(frames, ticks, system, network, logs));
    }

    static PerformanceReportData build(
            FrameDiagnostics.Snapshot frames,
            TickDiagnostics.Snapshot ticks,
            SystemDiagnostics.Snapshot system,
            NetworkDiagnostics.Snapshot network,
            LogSummary logs) {
        List<ReportPoint> fpsPoints = new ArrayList<>();
        for (FrameDiagnostics.Sample sample : frames.samples()) {
            fpsPoints.add(new ReportPoint(sample.epochMillis(), sample.fps()));
        }
        List<ReportPoint> fps = limit(fpsPoints);
        double currentFps = fps.isEmpty() ? -1 : fps.get(fps.size() - 1).value();
        double averageFps = average(fps);
        double lowFps = onePercentLow(fps);

        List<TickBucket> buckets = tickBuckets(ticks);
        List<ReportPoint> tps = limit(buckets.stream()
                .map(bucket -> new ReportPoint(bucket.epochMillis(), bucket.tps()))
                .toList());
        List<ReportPoint> mspt = limit(buckets.stream()
                .map(bucket -> new ReportPoint(bucket.epochMillis(), bucket.mspt()))
                .toList());
        double currentTps = tps.isEmpty() ? -1 : tps.get(tps.size() - 1).value();
        double p95Mspt = -1;
        if (ticks.durations().length > 0) {
            NumberStats stats = NumberStats.of(ticks.durations());
            p95Mspt = stats.p95() / 1_000_000.0;
        }

        List<ReportPoint> heap = new ArrayList<>();
        for (SystemDiagnostics.Sample sample : system.samples()) {
            heap.add(new ReportPoint(sample.epochMillis(), sample.heapUsed() / MIB));
        }
        heap = limit(heap);
        SystemDiagnostics.Sample lastSystem =
                system.samples().length == 0 ? null : system.samples()[system.samples().length - 1];
        double heapUsedMb = lastSystem == null ? -1 : lastSystem.heapUsed() / MIB;
        double heapMaxMb = lastSystem == null || lastSystem.heapMax() <= 0 ? -1 : lastSystem.heapMax() / MIB;
        double cpuPercent = lastSystem == null || lastSystem.processCpu() < 0 ? -1 : lastSystem.processCpu() * 100.0;

        List<ReportPoint> ping = new ArrayList<>();
        int currentPing = -1;
        if (network.samples().length > 0) {
            currentPing = network.samples()[network.samples().length - 1].pingMs();
        }
        for (NetworkDiagnostics.Sample sample : network.samples()) {
            if (sample.pingMs() >= 0) {
                ping.add(new ReportPoint(sample.epochMillis(), sample.pingMs()));
            }
        }
        ping = limit(ping);

        List<String> notes =
                findings(currentFps, currentTps, p95Mspt, heapUsedMb, heapMaxMb, cpuPercent, currentPing, logs);
        return new PerformanceReportData(
                System.currentTimeMillis(),
                fps,
                tps,
                mspt,
                heap,
                ping,
                currentFps,
                averageFps,
                lowFps,
                currentTps,
                p95Mspt,
                heapUsedMb,
                heapMaxMb,
                cpuPercent,
                currentPing,
                logs,
                notes);
    }

    private static List<TickBucket> tickBuckets(TickDiagnostics.Snapshot ticks) {
        List<TickBucket> buckets = new ArrayList<>();
        long bucketTime = Long.MIN_VALUE;
        long durationSum = 0;
        long intervalSum = 0;
        int intervals = 0;
        int count = 0;
        for (int index = 0; index < ticks.size(); index++) {
            long time = ticks.times()[index];
            if (time <= 0) {
                continue;
            }
            long currentBucket = time / 1000;
            if (bucketTime != Long.MIN_VALUE && currentBucket != bucketTime) {
                buckets.add(bucket(bucketTime, durationSum, intervalSum, intervals, count));
                durationSum = 0;
                intervalSum = 0;
                intervals = 0;
                count = 0;
            }
            bucketTime = currentBucket;
            durationSum += ticks.durations()[index];
            long interval = ticks.intervals()[index];
            if (interval > 0) {
                intervalSum += interval;
                intervals++;
            }
            count++;
        }
        if (bucketTime != Long.MIN_VALUE && count > 0) {
            buckets.add(bucket(bucketTime, durationSum, intervalSum, intervals, count));
        }
        if (buckets.size() > MAX_CHART_POINTS) {
            return buckets.subList(buckets.size() - MAX_CHART_POINTS, buckets.size());
        }
        return buckets;
    }

    private static TickBucket bucket(long epochMillis, long durationSum, long intervalSum, int intervals, int count) {
        double mspt = count == 0 ? 0 : durationSum / (double) count / 1_000_000.0;
        double tps = intervals == 0 || intervalSum == 0 ? 0 : intervals * 1_000_000_000.0 / intervalSum;
        return new TickBucket(epochMillis * 1000, tps, mspt);
    }

    private static List<String> findings(
            double currentFps,
            double currentTps,
            double p95Mspt,
            double heapUsedMb,
            double heapMaxMb,
            double cpuPercent,
            int currentPing,
            LogSummary logs) {
        List<String> notes = new ArrayList<>();
        if (currentFps >= 0 && currentFps < 30) {
            notes.add("low FPS (" + format(currentFps) + ")");
        }
        if (currentTps >= 0 && currentTps < 18) {
            notes.add("low client tick rate (" + format(currentTps) + " TPS)");
        }
        if (p95Mspt >= 0 && p95Mspt > 50) {
            notes.add("tick spikes (p95 " + format(p95Mspt) + " ms)");
        }
        if (heapMaxMb > 0 && heapUsedMb / heapMaxMb > 0.85) {
            notes.add("high heap usage (" + format(heapUsedMb / heapMaxMb * 100.0) + "%)");
        }
        if (cpuPercent >= 0 && cpuPercent > 80) {
            notes.add("high CPU usage (" + format(cpuPercent) + "%)");
        }
        if (currentPing > 150) {
            notes.add("high ping (" + currentPing + " ms)");
        }
        if (logs.fatal() + logs.errors() > 0) {
            notes.add((logs.fatal() + logs.errors()) + " recent ERROR/FATAL lines");
        }
        if (notes.isEmpty()) {
            notes.add("no obvious performance problems detected");
        }
        return notes;
    }

    private static List<ReportPoint> limit(List<ReportPoint> points) {
        if (points.size() <= MAX_CHART_POINTS) {
            return List.copyOf(points);
        }
        return List.copyOf(points.subList(points.size() - MAX_CHART_POINTS, points.size()));
    }

    private static double average(List<ReportPoint> points) {
        if (points.isEmpty()) {
            return -1;
        }
        double total = 0;
        for (ReportPoint point : points) {
            total += point.value();
        }
        return total / points.size();
    }

    private static double onePercentLow(List<ReportPoint> points) {
        if (points.isEmpty()) {
            return -1;
        }
        double[] values = points.stream().mapToDouble(ReportPoint::value).toArray();
        Arrays.sort(values);
        int count = Math.max(1, values.length / 100);
        double total = 0;
        for (int index = 0; index < count; index++) {
            total += values[index];
        }
        return total / count;
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private record TickBucket(long epochMillis, double tps, double mspt) {}
}
