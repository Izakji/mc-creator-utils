package com.izakji.mccreatorutils.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TimeCommandsTest {

    @Test
    void buildsPresetCommands() {
        assertEquals("time set day", TimeCommands.setPreset(TimePreset.DAY));
        assertEquals("time set night", TimeCommands.setPreset(TimePreset.NIGHT));
        assertEquals("time set noon", TimeCommands.setPreset(TimePreset.NOON));
        assertEquals("time set midnight", TimeCommands.setPreset(TimePreset.MIDNIGHT));
    }

    @Test
    void buildsTickCommands() {
        assertEquals("time set 0", TimeCommands.setTicks(0));
        assertEquals("time set 6000", TimeCommands.setTicks(6000));
        assertEquals("time set 24000", TimeCommands.setTicks(24000));
    }

    @Test
    void rejectsInvalidInputs() {
        assertThrows(NullPointerException.class, () -> TimeCommands.setPreset(null));
        assertThrows(IllegalArgumentException.class, () -> TimeCommands.setTicks(-1));
        assertThrows(IllegalArgumentException.class, () -> TimeCommands.setTicks(24001));
    }

    @Test
    void freezeMapsToDaylightCycleRule() {
        assertEquals("gamerule doDaylightCycle false", TimeCommands.setFrozen(true));
        assertEquals("gamerule doDaylightCycle true", TimeCommands.setFrozen(false));
    }
}
