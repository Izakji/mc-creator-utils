package com.izakji.mccreatorutils.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class XpAmountInputTest {

    @Test
    void acceptsValuesFromOneThroughNineHundredNinetyNine() {
        assertEquals(1, XpAmountInput.parse("1").orElseThrow());
        assertEquals(999, XpAmountInput.parse("999").orElseThrow());
        assertTrue(XpAmountInput.isValid("42"));
    }

    @Test
    void rejectsOutOfRangeAndNonDigitValues() {
        assertFalse(XpAmountInput.isValid(""));
        assertFalse(XpAmountInput.isValid("0"));
        assertFalse(XpAmountInput.isValid("1000"));
        assertFalse(XpAmountInput.isValid("12.5"));
        assertFalse(XpAmountInput.isValid("-1"));
        assertFalse(XpAmountInput.isValid("abc"));
        assertFalse(XpAmountInput.isValid(" 12"));
    }

    @Test
    void allowsOnlyAsciiDigits() {
        assertTrue(XpAmountInput.isAllowedCharacter('0'));
        assertTrue(XpAmountInput.isAllowedCharacter('9'));
        assertFalse(XpAmountInput.isAllowedCharacter('.'));
        assertFalse(XpAmountInput.isAllowedCharacter('-'));
        assertFalse(XpAmountInput.isAllowedCharacter('a'));
    }
}
