package com.izakji.mccreatorutils.client.agent.context;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * One-sample-per-second rolling performance history.
 *
 * @author Izakji
 */
public final class PerformanceMonitor {
    private static final int CAPACITY = 60;
    private static final long SAMPLE_INTERVAL_MILLIS = 1000;
    private static final PerformanceMonitor INSTANCE = new PerformanceMonitor();

    private final PerformanceSample[] samples = new PerformanceSample[CAPACITY];
    private int next;
    private int count;
    private long lastSample;

    private PerformanceMonitor() {}

    public static PerformanceMonitor instance() {
        return INSTANCE;
    }

    /**
     * Samples at most once per second. Called from the client tick.
     *
     * @param minecraft client
     */
    public void tick(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        synchronized (this) {
            if (lastSample != 0 && now - lastSample < SAMPLE_INTERVAL_MILLIS) {
                return;
            }
            lastSample = now;
            samples[next] = new PerformanceSample(
                    now,
                    minecraft.getFps(),
                    usedMemoryMb(),
                    Runtime.getRuntime().maxMemory() / (1024 * 1024),
                    ping(minecraft, player),
                    minecraft.options.renderDistance().get());
            next = (next + 1) % CAPACITY;
            count = Math.min(CAPACITY, count + 1);
        }
    }

    /**
     * Returns samples in chronological order.
     *
     * @return immutable snapshot
     */
    public synchronized List<PerformanceSample> samples() {
        List<PerformanceSample> result = new ArrayList<>(count);
        int start = (next - count + CAPACITY) % CAPACITY;
        for (int index = 0; index < count; index++) {
            result.add(samples[(start + index) % CAPACITY]);
        }
        return List.copyOf(result);
    }

    private static long usedMemoryMb() {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
    }

    private static int ping(Minecraft minecraft, LocalPlayer player) {
        if (minecraft.getConnection() == null) {
            return 0;
        }
        var info = minecraft.getConnection().getPlayerInfo(player.getUUID());
        return info == null ? 0 : info.getLatency();
    }
}
