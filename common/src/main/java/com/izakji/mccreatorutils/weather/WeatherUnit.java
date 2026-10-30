package com.izakji.mccreatorutils.weather;

import java.util.Objects;

/**
 * Real-time units available for the optional thunder duration.
 *
 * <p>Durations are converted to seconds for {@code /weather}, which counts real-time seconds, not
 * Minecraft day/night cycles. One {@link #DAYS} therefore equals 86,400 seconds (72 Minecraft days
 * of 20 minutes), matching the unit label rather than the game's day length.
 *
 * @author Izakji
 */
public enum WeatherUnit {
    SECONDS("s", 1),
    MINUTES("m", 60),
    HOURS("h", 3600),
    DAYS("d", 86400);

    private final String suffix;
    private final int multiplier;

    WeatherUnit(String suffix, int multiplier) {
        this.suffix = Objects.requireNonNull(suffix, "suffix");
        this.multiplier = multiplier;
    }

    public String suffix() {
        return suffix;
    }

    public int multiplier() {
        return multiplier;
    }

    public String translationKey() {
        return "tool.mccreatorutils.world_time.unit." + suffix;
    }
}
