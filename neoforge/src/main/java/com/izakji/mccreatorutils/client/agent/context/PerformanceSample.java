package com.izakji.mccreatorutils.client.agent.context;

/**
 * One low-frequency client performance sample.
 *
 * @param epochMillis sample time
 * @param fps frames per second
 * @param memoryUsedMb used JVM memory
 * @param memoryMaxMb maximum JVM memory
 * @param ping server ping in milliseconds
 * @param renderDistance render distance in chunks
 * @author Izakji
 */
public record PerformanceSample(
        long epochMillis, int fps, long memoryUsedMb, long memoryMaxMb, int ping, int renderDistance) {}
