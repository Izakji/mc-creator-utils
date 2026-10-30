package com.izakji.mccreatorutils.camera;

import com.izakji.mccreatorutils.util.BoundedDigitInput;
import java.util.OptionalInt;

/**
 * Validation rules for the numeric part of a CMDCam scene duration.
 *
 * <p>The accepted form is one to three ASCII digits representing a value from 1 through 999.
 *
 * @author Izakji
 */
public final class DurationInput {
    public static final int MIN_VALUE = 1;
    public static final int MAX_VALUE = 999;
    public static final int MAX_DIGITS = 3;

    private DurationInput() {}

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
