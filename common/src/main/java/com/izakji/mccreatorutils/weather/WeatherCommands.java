package com.izakji.mccreatorutils.weather;

import java.util.Objects;

/**
 * Builds vanilla weather command strings without a leading slash.
 *
 * @author Izakji
 */
public final class WeatherCommands {
    private WeatherCommands() {}

    public static String clear() {
        return "weather clear";
    }

    public static String rain() {
        return "weather rain";
    }

    public static String thunder() {
        return "weather thunder";
    }

    public static String thunder(int value, WeatherUnit unit) {
        Objects.requireNonNull(unit, "unit");
        if (value < WeatherDurationInput.MIN_VALUE || value > WeatherDurationInput.MAX_VALUE) {
            throw new IllegalArgumentException("Thunder duration must be between 1 and 99");
        }
        return "weather thunder " + (value * unit.multiplier()) + "s";
    }
}
