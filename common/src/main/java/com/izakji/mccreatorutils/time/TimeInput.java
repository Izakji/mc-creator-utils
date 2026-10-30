package com.izakji.mccreatorutils.time;

import com.izakji.mccreatorutils.util.BoundedDigitInput;
import java.util.OptionalInt;

/**
 * Validation rules for a direct Minecraft time tick value.
 *
 * <p>The accepted form is one to five ASCII digits representing a value from 0 through 24000.
 *
 * @author Izakji
 */
public final class TimeInput {
    public static final int MIN_VALUE = 0;
    public static final int MAX_VALUE = 24000;
    public static final int MAX_DIGITS = 5;

    private TimeInput() {}

    public static boolean isAllowedCharacter(char value) {
        return BoundedDigitInput.isAsciiDigit(value);
    }

    public static OptionalInt parse(String raw) {
        return BoundedDigitInput.parse(raw, MIN_VALUE, MAX_VALUE, MAX_DIGITS);
    }

    public static boolean isValid(String raw) {
        return parse(raw).isPresent();
    }
}
