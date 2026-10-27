package com.izakji.mccreatorutils.weather;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class WeatherCommandsTest {

    @Test
    void buildsPlainWeatherCommands() {
        assertEquals("weather clear", WeatherCommands.clear());
        assertEquals("weather rain", WeatherCommands.rain());
        assertEquals("weather thunder", WeatherCommands.thunder());
    }

    @Test
    void convertsThunderDurationToSeconds() {
        assertEquals("weather thunder 20", WeatherCommands.thunder(20, WeatherUnit.SECONDS));
        assertEquals("weather thunder 120", WeatherCommands.thunder(2, WeatherUnit.MINUTES));
        assertEquals("weather thunder 3600", WeatherCommands.thunder(1, WeatherUnit.HOURS));
        assertEquals("weather thunder 86400", WeatherCommands.thunder(1, WeatherUnit.DAYS));
        assertEquals("weather thunder 8553600", WeatherCommands.thunder(99, WeatherUnit.DAYS));
    }

    @Test
    void unitsUseExpectedSuffixesAndMultipliers() {
        assertEquals("s", WeatherUnit.SECONDS.suffix());
        assertEquals("m", WeatherUnit.MINUTES.suffix());
        assertEquals("h", WeatherUnit.HOURS.suffix());
        assertEquals("d", WeatherUnit.DAYS.suffix());
        assertEquals(1, WeatherUnit.SECONDS.multiplier());
        assertEquals(60, WeatherUnit.MINUTES.multiplier());
        assertEquals(3600, WeatherUnit.HOURS.multiplier());
        assertEquals(86400, WeatherUnit.DAYS.multiplier());
    }

    @Test
    void rejectsInvalidInputs() {
        assertThrows(NullPointerException.class, () -> WeatherCommands.thunder(20, null));
        assertThrows(IllegalArgumentException.class, () -> WeatherCommands.thunder(0, WeatherUnit.SECONDS));
        assertThrows(IllegalArgumentException.class, () -> WeatherCommands.thunder(100, WeatherUnit.SECONDS));
    }
}
