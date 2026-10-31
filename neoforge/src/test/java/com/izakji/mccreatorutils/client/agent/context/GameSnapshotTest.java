package com.izakji.mccreatorutils.client.agent.context;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GameSnapshotTest {

    @Test
    void formatsTheCurrentBiomePositionAndWorld() {
        GameSnapshot snapshot = new GameSnapshot(
                "Alex",
                "survival",
                20.0f,
                20.0f,
                18,
                4,
                2,
                "minecraft:stone_sword",
                "minecraft:overworld",
                "minecraft:plains",
                12,
                64,
                -8,
                6000,
                "clear",
                "minecraft:cow",
                "none",
                120,
                34,
                1024,
                4096,
                12,
                42);
        String text = snapshot.format();
        assertTrue(text.contains("biome minecraft:plains"));
        assertTrue(text.contains("position 12 64 -8"));
        assertTrue(text.contains("minecraft:overworld"));
    }
}
