package com.izakji.mccreatorutils.client.agent.context;

/**
 * Bounded per-tick duration and interval history.
 *
 * @author Izakji
 */
public final class TickDiagnostics {
    public static final int CAPACITY = 6000;
    private static final TickDiagnostics INSTANCE = new TickDiagnostics();

    private final long[] times = new long[CAPACITY];
    private final long[] durations = new long[CAPACITY];
    private final long[] intervals = new long[CAPACITY];
    private int next;
    private int count;
    private long tickStart;
    private long lastEnd;

    private TickDiagnostics() {}

    public static TickDiagnostics instance() {
        return INSTANCE;
    }

    public void beginTick() {
        tickStart = System.nanoTime();
    }

    public synchronized void endTick() {
        if (tickStart == 0) {
            return;
        }
        long now = System.nanoTime();
        long duration = Math.max(0, now - tickStart);
        long interval = lastEnd == 0 ? 0 : Math.max(0, now - lastEnd);
        tickStart = 0;
        lastEnd = now;
        times[next] = System.currentTimeMillis();
        durations[next] = duration;
        intervals[next] = interval;
        next = (next + 1) % CAPACITY;
        count = Math.min(CAPACITY, count + 1);
    }

    public synchronized Snapshot snapshot() {
        long[] sampledTimes = new long[count];
        long[] sampledDurations = new long[count];
        long[] sampledIntervals = new long[count];
        int start = (next - count + CAPACITY) % CAPACITY;
        for (int index = 0; index < count; index++) {
            int source = (start + index) % CAPACITY;
            sampledTimes[index] = times[source];
            sampledDurations[index] = durations[source];
            sampledIntervals[index] = intervals[source];
        }
        return new Snapshot(sampledTimes, sampledDurations, sampledIntervals);
    }

    /**
     * Immutable per-tick history in chronological order.
     *
     * @param times sample epoch milliseconds
     * @param durations tick processing time in nanoseconds
     * @param intervals time between tick starts in nanoseconds
     */
    public record Snapshot(long[] times, long[] durations, long[] intervals) {
        public int size() {
            return durations.length;
        }

        public static Snapshot empty() {
            return new Snapshot(new long[0], new long[0], new long[0]);
        }
    }
}
