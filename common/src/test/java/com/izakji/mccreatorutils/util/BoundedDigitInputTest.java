package com.izakji.mccreatorutils.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BoundedDigitInputTest {

    @Test
    void acceptsInclusiveBoundsAndRejectsOutsideThem() {
        assertEquals(1, BoundedDigitInput.parse("1", 1, 99, 2).orElseThrow());
        assertEquals(99, BoundedDigitInput.parse("99", 1, 99, 2).orElseThrow());
        assertTrue(BoundedDigitInput.parse("0", 1, 99, 2).isEmpty());
        assertTrue(BoundedDigitInput.parse("100", 1, 99, 3).isEmpty());
    }

    @Test
    void rejectsNonDigitsEmptyAndTooManyDigits() {
        assertTrue(BoundedDigitInput.parse(null, 0, 10, 2).isEmpty());
        assertTrue(BoundedDigitInput.parse("", 0, 10, 2).isEmpty());
        assertTrue(BoundedDigitInput.parse("1a", 0, 10, 2).isEmpty());
        assertTrue(BoundedDigitInput.parse("123", 0, 999, 2).isEmpty());
        assertTrue(
                BoundedDigitInput.parse("9999999999", 0, Integer.MAX_VALUE, 10).isEmpty());
    }

    @Test
    void recognisesAsciiDigitsOnly() {
        assertTrue(BoundedDigitInput.isAsciiDigit('0'));
        assertTrue(BoundedDigitInput.isAsciiDigit('9'));
        assertFalse(BoundedDigitInput.isAsciiDigit('a'));
        assertFalse(BoundedDigitInput.isAsciiDigit('٤'));
    }
}
