package com.izakji.mccreatorutils.weather;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class WeatherDurationInputTest {

    @Test
    void acceptsValidDurations() {
        assertEquals(OptionalInt.of(1), WeatherDurationInput.parse("1"));
        assertEquals(OptionalInt.of(20), WeatherDurationInput.parse("20"));
        assertEquals(OptionalInt.of(99), WeatherDurationInput.parse("99"));
    }

    @Test
    void rejectsInvalidDurations() {
        for (String raw : List.of("", "0", "100", "12a", " 5", "-1", "1.5")) {
            assertTrue(WeatherDurationInput.parse(raw).isEmpty(), raw);
        }
        assertTrue(WeatherDurationInput.parse(null).isEmpty());
    }

    @Test
    void characterValidatorAllowsOnlyAsciiDigits() {
        assertTrue(WeatherDurationInput.isAllowedCharacter('0'));
        assertTrue(WeatherDurationInput.isAllowedCharacter('9'));
        assertFalse(WeatherDurationInput.isAllowedCharacter('a'));
        assertFalse(WeatherDurationInput.isAllowedCharacter('-'));
        assertFalse(WeatherDurationInput.isAllowedCharacter(' '));
    }

    @Test
    void emptyIsValidForTheOptionalDuration() {
        assertTrue(WeatherDurationInput.isValidOrEmpty(null));
        assertTrue(WeatherDurationInput.isValidOrEmpty(""));
        assertTrue(WeatherDurationInput.isValidOrEmpty("1"));
        assertTrue(WeatherDurationInput.isValidOrEmpty("99"));
        assertFalse(WeatherDurationInput.isValidOrEmpty("0"));
        assertFalse(WeatherDurationInput.isValidOrEmpty("100"));
        assertFalse(WeatherDurationInput.isValidOrEmpty("a"));
    }
}
