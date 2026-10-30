package com.izakji.mccreatorutils.util;

import java.util.OptionalInt;

/**
 * Parses small, bounded, non-negative decimal inputs.
 *
 * <p>Only ASCII digits are accepted. The shared parser keeps the validation rules and error
 * boundaries identical across the camera, item, time, weather, and experience fields.
 *
 * @author Izakji
 */
public final class BoundedDigitInput {
    private BoundedDigitInput() {}

    /**
     * Parses an inclusive bounded integer.
     *
     * @param raw input text
     * @param minValue smallest accepted value
     * @param maxValue largest accepted value
     * @param maxDigits maximum accepted digit count
     * @return parsed value, or empty when the text is empty, too long, non-numeric, or out of range
     */
    public static OptionalInt parse(String raw, int minValue, int maxValue, int maxDigits) {
        if (raw == null || raw.isEmpty() || raw.length() > maxDigits) {
            return OptionalInt.empty();
        }
        for (int index = 0; index < raw.length(); index++) {
            if (!isAsciiDigit(raw.charAt(index))) {
                return OptionalInt.empty();
            }
        }
        int value;
        try {
            value = Integer.parseInt(raw);
        } catch (NumberFormatException overflow) {
            return OptionalInt.empty();
        }
        return value >= minValue && value <= maxValue ? OptionalInt.of(value) : OptionalInt.empty();
    }

    /**
     * Returns whether a character is an ASCII decimal digit.
     *
     * @param value character to test
     * @return true for {@code '0'} through {@code '9'}
     */
    public static boolean isAsciiDigit(char value) {
        return value >= '0' && value <= '9';
    }
}
