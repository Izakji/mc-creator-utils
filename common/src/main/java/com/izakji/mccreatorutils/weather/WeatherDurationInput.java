package com.izakji.mccreatorutils.weather;

import com.izakji.mccreatorutils.util.BoundedDigitInput;
import java.util.OptionalInt;

/**
 * Validation rules for the optional thunder duration.
 *
 * <p>The accepted form is one or two ASCII digits representing a value from 1 through 99.
 *
 * @author Izakji
 */
public final class WeatherDurationInput {
    public static final int MIN_VALUE = 1;
    public static final int MAX_VALUE = 99;
    public static final int MAX_DIGITS = 2;

    private WeatherDurationInput() {}

    public static boolean isAllowedCharacter(char value) {
        return BoundedDigitInput.isAsciiDigit(value);
    }

    public static OptionalInt parse(String raw) {
        return BoundedDigitInput.parse(raw, MIN_VALUE, MAX_VALUE, MAX_DIGITS);
    }

    public static boolean isValid(String raw) {
        return parse(raw).isPresent();
    }

    public static boolean isValidOrEmpty(String raw) {
        return raw == null || raw.isEmpty() || isValid(raw);
    }
}
