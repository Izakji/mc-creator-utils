package com.izakji.mccreatorutils.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class TimeInputTest {

    @Test
    void acceptsValidTicks() {
        assertEquals(OptionalInt.of(0), TimeInput.parse("0"));
        assertEquals(OptionalInt.of(1), TimeInput.parse("1"));
        assertEquals(OptionalInt.of(6000), TimeInput.parse("6000"));
        assertEquals(OptionalInt.of(24000), TimeInput.parse("24000"));
        assertEquals(OptionalInt.of(0), TimeInput.parse("00000"));
    }

    @Test
    void rejectsInvalidTicks() {
        for (String raw : List.of("", "24001", "99999", "123456", "12a", " 1", "-1", "1.5")) {
            assertTrue(TimeInput.parse(raw).isEmpty(), raw);
        }
        assertTrue(TimeInput.parse(null).isEmpty());
    }

    @Test
    void characterValidatorAllowsOnlyAsciiDigits() {
        assertTrue(TimeInput.isAllowedCharacter('0'));
        assertTrue(TimeInput.isAllowedCharacter('9'));
        assertFalse(TimeInput.isAllowedCharacter('a'));
        assertFalse(TimeInput.isAllowedCharacter('-'));
        assertFalse(TimeInput.isAllowedCharacter(' '));
    }
}
