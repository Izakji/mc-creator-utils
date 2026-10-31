package com.izakji.mccreatorutils.client.agent.context;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.List;

/**
 * One-hertz CPU, heap, and garbage-collection history.
 *
 * @author Izakji
 */
public final class SystemDiagnostics {
    private static final int CAPACITY = 300;
    private static final long SAMPLE_INTERVAL_NANOS = 1_000_000_000L;
    private static final SystemDiagnostics INSTANCE = new SystemDiagnostics();

    private final MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
    private final List<GarbageCollectorMXBean> collectors = ManagementFactory.getGarbageCollectorMXBeans();
    private final com.sun.management.OperatingSystemMXBean operatingSystem = operatingSystem();
    private final Sample[] samples = new Sample[CAPACITY];
    private int next;
    private int count;
    private long lastSample;

    private SystemDiagnostics() {}

    public static SystemDiagnostics instance() {
        return INSTANCE;
    }

    public void sampleIfDue() {
        long now = System.nanoTime();
        synchronized (this) {
            if (lastSample != 0 && now - lastSample < SAMPLE_INTERVAL_NANOS) {
                return;
            }
            lastSample = now;
            samples[next] = sample();
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

    private Sample sample() {
        MemoryUsage heap = memory.getHeapMemoryUsage();
        MemoryUsage nonHeap = memory.getNonHeapMemoryUsage();
        long gcCount = 0;
        long gcTime = 0;
        boolean gcAvailable = false;
        for (GarbageCollectorMXBean collector : collectors) {
            try {
                long collectorCount = collector.getCollectionCount();
                long collectorTime = collector.getCollectionTime();
                if (collectorCount >= 0 && collectorTime >= 0) {
                    gcCount += collectorCount;
                    gcTime += collectorTime;
                    gcAvailable = true;
                }
            } catch (RuntimeException ignored) {
                // A collector without statistics must not break sampling.
            }
        }
        double processCpu = -1;
        double systemCpu = -1;
        if (operatingSystem != null) {
            try {
                processCpu = operatingSystem.getProcessCpuLoad();
                systemCpu = operatingSystem.getCpuLoad();
            } catch (RuntimeException ignored) {
                // CPU load is optional.
            }
        }
        return new Sample(
                System.currentTimeMillis(),
                processCpu,
                systemCpu,
                heap.getUsed(),
                heap.getCommitted(),
                heap.getMax(),
                nonHeap.getUsed(),
                gcAvailable ? gcCount : -1,
                gcAvailable ? gcTime : -1);
    }

    private static com.sun.management.OperatingSystemMXBean operatingSystem() {
        try {
            java.lang.management.OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();
            return bean instanceof com.sun.management.OperatingSystemMXBean extended ? extended : null;
        } catch (RuntimeException | LinkageError unavailable) {
            return null;
        }
    }

    /**
     * One system sample.
     *
     * @param epochMillis sample time
     * @param processCpu process CPU load in [0,1], or negative when unavailable
     * @param systemCpu system CPU load in [0,1], or negative when unavailable
     * @param heapUsed used heap bytes
     * @param heapCommitted committed heap bytes
     * @param heapMax maximum heap bytes, or -1
     * @param nonHeapUsed used non-heap bytes
     * @param gcCount cumulative collection count, or -1
     * @param gcTimeMs cumulative collection time in milliseconds, or -1
     */
    public record Sample(
            long epochMillis,
            double processCpu,
            double systemCpu,
            long heapUsed,
            long heapCommitted,
            long heapMax,
            long nonHeapUsed,
            long gcCount,
            long gcTimeMs) {}

    /**
     * Immutable system history in chronological order.
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
