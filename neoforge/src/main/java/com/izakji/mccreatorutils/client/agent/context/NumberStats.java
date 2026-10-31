package com.izakji.mccreatorutils.client.agent.context;

import java.util.Arrays;

/**
 * Summary statistics for a bounded sample window.
 *
 * @param count sample count
 * @param average average value
 * @param min minimum value
 * @param median median value
 * @param p95 95th percentile
 * @param max maximum value
 * @author Izakji
 */
public record NumberStats(int count, double average, long min, long median, long p95, long max) {
    public static NumberStats of(long[] values) {
        if (values == null || values.length == 0) {
            return new NumberStats(0, 0, 0, 0, 0, 0);
        }
        long[] sorted = values.clone();
        Arrays.sort(sorted);
        long total = 0;
        for (long value : sorted) {
            total += value;
        }
        return new NumberStats(
                sorted.length,
                (double) total / sorted.length,
                sorted[0],
                percentile(sorted, 0.50),
                percentile(sorted, 0.95),
                sorted[sorted.length - 1]);
    }

    public boolean empty() {
        return count == 0;
    }

    private static long percentile(long[] sorted, double percentile) {
        int index = (int) Math.ceil(percentile * sorted.length) - 1;
        return sorted[Math.max(0, Math.min(sorted.length - 1, index))];
    }
}
