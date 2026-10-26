package com.izakji.mccreatorutils.camera;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class DurationInputTest {

    @Test
    void acceptsValidValues() {
        assertEquals(OptionalInt.of(1), DurationInput.parse("1"));
        assertEquals(OptionalInt.of(20), DurationInput.parse("20"));
        assertEquals(OptionalInt.of(999), DurationInput.parse("999"));
    }

    @Test
    void rejectsInvalidValues() {
        for (String raw : List.of("", "0", "000", "1000", "12a", " 20", "-5", "1.5")) {
            assertTrue(DurationInput.parse(raw).isEmpty(), raw);
        }
        assertTrue(DurationInput.parse(null).isEmpty());
    }

    @Test
    void characterValidatorAllowsOnlyAsciiDigits() {
        assertTrue(DurationInput.isAllowedCharacter('0'));
        assertTrue(DurationInput.isAllowedCharacter('9'));
        assertFalse(DurationInput.isAllowedCharacter('a'));
        assertFalse(DurationInput.isAllowedCharacter('-'));
        assertFalse(DurationInput.isAllowedCharacter('.'));
        assertFalse(DurationInput.isAllowedCharacter(' '));
    }
}
