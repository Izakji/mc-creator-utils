package com.izakji.mccreatorutils.locate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class LocateCommandsTest {

    @Test
    void buildsLocateCommands() {
        assertEquals("locate biome minecraft:plains", LocateCommands.biome(ResourceLocation.parse("minecraft:plains")));
        assertEquals(
                "locate structure minecraft:village_plains",
                LocateCommands.structure(ResourceLocation.parse("minecraft:village_plains")));
    }

    @Test
    void rejectsNullIds() {
        assertThrows(NullPointerException.class, () -> LocateCommands.biome(null));
        assertThrows(NullPointerException.class, () -> LocateCommands.structure(null));
    }
}
