package com.izakji.mccreatorutils.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class GiveCommandsTest {

    @Test
    void buildsExactCommands() {
        assertEquals(
                "give @s minecraft:stone 64",
                GiveCommands.give(ResourceLocation.parse("minecraft:stone"), 64));
        assertEquals("clear", GiveCommands.clearInventory());
        assertEquals("kill @e[type=item]", GiveCommands.clearWorldItems());
    }

    @Test
    void rejectsInvalidInputs() {
        assertThrows(NullPointerException.class, () -> GiveCommands.give(null, 1));
        assertThrows(
                IllegalArgumentException.class,
                () -> GiveCommands.give(ResourceLocation.parse("minecraft:stone"), 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> GiveCommands.give(ResourceLocation.parse("minecraft:stone"), 513));
    }
}
