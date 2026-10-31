package com.izakji.mccreatorutils.client.agent.context;

import net.minecraft.client.Minecraft;

/**
 * One-hertz client FPS history used by visual reports.
 *
 * @author Izakji
 */
public final class FrameDiagnostics {
    private static final int CAPACITY = 300;
    private static final long SAMPLE_INTERVAL_NANOS = 1_000_000_000L;
    private static final FrameDiagnostics INSTANCE = new FrameDiagnostics();

    private final Sample[] samples = new Sample[CAPACITY];
    private int next;
    private int count;
    private long lastSample;

    private FrameDiagnostics() {}

    public static FrameDiagnostics instance() {
        return INSTANCE;
    }

    public void sampleIfDue(Minecraft minecraft) {
        long now = System.nanoTime();
        synchronized (this) {
            if (lastSample != 0 && now - lastSample < SAMPLE_INTERVAL_NANOS) {
                return;
            }
            lastSample = now;
            samples[next] = new Sample(System.currentTimeMillis(), minecraft.getFps());
            next = (next + 1) % CAPACITY;
            count = Math.min(CAPACITY, count + 1);
        }
    }

    public synchronized Snapshot snapshot() {
        Sample[] ordered = new Sample[count];
        int start = (next - count + CAPACITY) % CAPACITY;
        for (int index = 0; index < count; index++) {
            ordered[index] = samples[(start + index) % CAPACITY];
        }
        return new Snapshot(ordered);
    }

    /**
     * One FPS sample.
     *
     * @param epochMillis sample time
     * @param fps frames per second
     */
    public record Sample(long epochMillis, int fps) {}

    /**
     * Immutable FPS history in chronological order.
     *
     * @param samples samples
     */
    public record Snapshot(Sample[] samples) {
        public int size() {
            return samples.length;
        }

        public static Snapshot empty() {
            return new Snapshot(new Sample[0]);
        }
    }
}
