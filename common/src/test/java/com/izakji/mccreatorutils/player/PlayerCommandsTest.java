package com.izakji.mccreatorutils.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PlayerCommandsTest {

    @Test
    void buildsEffectCommands() {
        assertEquals("effect give @p resistance 99999 255 true", PlayerCommands.giveInvincible());
        assertEquals("effect clear @p minecraft:resistance", PlayerCommands.clearInvincible());
        assertEquals("effect clear @p", PlayerCommands.clearEffects());
        assertEquals("effect give @p minecraft:water_breathing 9999 0 true", PlayerCommands.giveWaterBreathing());
        assertEquals("effect clear @p minecraft:water_breathing", PlayerCommands.clearWaterBreathing());
    }

    @Test
    void buildsXpCommand() {
        assertEquals("xp add @s 1 levels", PlayerCommands.addXp(1, XpType.LEVELS));
        assertEquals("xp add @s 999 points", PlayerCommands.addXp(999, XpType.POINTS));
    }

    @Test
    void validatesXpAmount() {
        assertThrows(IllegalArgumentException.class, () -> PlayerCommands.addXp(0, XpType.LEVELS));
        assertThrows(IllegalArgumentException.class, () -> PlayerCommands.addXp(1000, XpType.POINTS));
        assertThrows(NullPointerException.class, () -> PlayerCommands.addXp(1, null));
    }
}
