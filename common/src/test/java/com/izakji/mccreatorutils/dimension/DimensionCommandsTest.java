package com.izakji.mccreatorutils.dimension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class DimensionCommandsTest {

    @Test
    void buildsDimensionTeleportCommand() {
        assertEquals(
                "execute in minecraft:the_nether run teleport @s ~ ~ ~",
                DimensionCommands.teleport(ResourceLocation.parse("minecraft:the_nether")));
        assertEquals(
                "execute in moddimensions:sky_islands run teleport @s ~ ~ ~",
                DimensionCommands.teleport(ResourceLocation.parse("moddimensions:sky_islands")));
    }

    @Test
    void rejectsNullDimension() {
        assertThrows(NullPointerException.class, () -> DimensionCommands.teleport(null));
    }
}
