package com.izakji.mccreatorutils.player;

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
