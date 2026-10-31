package com.izakji.mccreatorutils.client.agent.context;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CommandSafetyTest {

    @Test
    void autoApprovesReadOnlyCommands() {
        assertTrue(CommandSafety.isReadOnly("locate structure minecraft:stronghold"));
        assertTrue(CommandSafety.isReadOnly("/seed"));
        assertTrue(CommandSafety.isReadOnly("time query daytime"));
        assertTrue(CommandSafety.isReadOnly("data get entity @s Pos"));
        assertTrue(CommandSafety.isReadOnly("difficulty"));
        assertTrue(CommandSafety.isReadOnly("gamerule doDaylightCycle"));
        assertTrue(CommandSafety.isReadOnly("scoreboard players get Steve points"));
    }

    @Test
    void requiresApprovalForMutatingCommands() {
        assertFalse(CommandSafety.isReadOnly("tp Steve 0 64 0"));
        assertFalse(CommandSafety.isReadOnly("fill 0 0 0 1 1 1 stone"));
        assertFalse(CommandSafety.isReadOnly("time set day"));
        assertFalse(CommandSafety.isReadOnly("data merge storage demo {x:1}"));
        assertFalse(CommandSafety.isReadOnly("gamerule doDaylightCycle false"));
        assertFalse(CommandSafety.isReadOnly("ftbquests editingmod"));
        assertFalse(CommandSafety.isReadOnly(""));
    }
}
