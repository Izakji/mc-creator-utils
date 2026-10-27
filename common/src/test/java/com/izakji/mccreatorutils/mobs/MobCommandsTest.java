package com.izakji.mccreatorutils.mobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class MobCommandsTest {

    @Test
    void buildsMobCommands() {
        assertEquals(
                "summon minecraft:zombie",
                MobCommands.summon(ResourceLocation.parse("minecraft:zombie")));
        assertEquals("kill @e[type=!minecraft:player]", MobCommands.killAllExceptPlayers());
    }

    @Test
    void rejectsNullEntityType() {
        assertThrows(NullPointerException.class, () -> MobCommands.summon(null));
    }
}
