package com.izakji.mccreatorutils.time;

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
        return value >= '0' && value <= '9';
    }

    public static OptionalInt parse(String raw) {
        if (raw == null || raw.isEmpty() || raw.length() > MAX_DIGITS) {
            return OptionalInt.empty();
        }
        for (int index = 0; index < raw.length(); index++) {
            if (!isAllowedCharacter(raw.charAt(index))) {
                return OptionalInt.empty();
            }
        }
        int value = Integer.parseInt(raw);
        return value >= MIN_VALUE && value <= MAX_VALUE ? OptionalInt.of(value) : OptionalInt.empty();
    }

    public static boolean isValid(String raw) {
        return parse(raw).isPresent();
    }
}
