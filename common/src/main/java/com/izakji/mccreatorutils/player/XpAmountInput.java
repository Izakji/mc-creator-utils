package com.izakji.mccreatorutils.player;

import com.izakji.mccreatorutils.util.BoundedDigitInput;
import java.util.OptionalInt;

/**
 * Validation rules for the player experience amount.
 *
 * <p>The accepted form is one to three ASCII digits representing a value from 1 through 999.</p>
 *
 * @author Izakji
 */
public final class XpAmountInput {
    public static final int MIN_VALUE = 1;
    public static final int MAX_VALUE = 999;
    public static final int MAX_DIGITS = 3;

    private XpAmountInput() {}

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
