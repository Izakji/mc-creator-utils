package com.izakji.mccreatorutils.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class ItemCountInputTest {

    @Test
    void acceptsValidCounts() {
        assertEquals(OptionalInt.of(1), ItemCountInput.parse("1"));
        assertEquals(OptionalInt.of(64), ItemCountInput.parse("64"));
        assertEquals(OptionalInt.of(512), ItemCountInput.parse("512"));
    }

    @Test
    void rejectsInvalidCounts() {
        for (String raw : List.of("", "0", "513", "12a", " 5", "-1", "1.5")) {
            assertTrue(ItemCountInput.parse(raw).isEmpty(), raw);
        }
        assertTrue(ItemCountInput.parse(null).isEmpty());
    }

    @Test
    void characterValidatorAllowsOnlyAsciiDigits() {
        assertTrue(ItemCountInput.isAllowedCharacter('0'));
        assertTrue(ItemCountInput.isAllowedCharacter('9'));
        assertFalse(ItemCountInput.isAllowedCharacter('a'));
        assertFalse(ItemCountInput.isAllowedCharacter('-'));
        assertFalse(ItemCountInput.isAllowedCharacter(' '));
    }
}
