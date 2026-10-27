package com.izakji.mccreatorutils.time;

import java.util.Objects;

/**
 * Builds vanilla time command strings without a leading slash.
 *
 * @author Izakji
 */
public final class TimeCommands {
    private TimeCommands() {}

    public static String setPreset(TimePreset preset) {
        Objects.requireNonNull(preset, "preset");
        return "time set " + preset.id();
    }

    public static String setTicks(int ticks) {
        if (ticks < TimeInput.MIN_VALUE || ticks > TimeInput.MAX_VALUE) {
            throw new IllegalArgumentException("Time must be between 0 and 24000");
        }
        return "time set " + ticks;
    }

    public static String setFrozen(boolean frozen) {
        return "gamerule doDaylightCycle " + (frozen ? "false" : "true");
    }
}
