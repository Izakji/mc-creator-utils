package com.izakji.mccreatorutils.locate.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

class LocateRequestKeyTest {

    @Test
    void canonicalizesTargetsAndCopiesOrigin() {
        var origin = new BlockPos(1, 2, 3);
        var key = new LocateRequestKey(
                Level.OVERWORLD,
                LocateType.BIOME,
                List.of(
                        ResourceLocation.parse("minecraft:plains"),
                        ResourceLocation.parse("minecraft:forest"),
                        ResourceLocation.parse("minecraft:plains")),
                origin,
                6400,
                false);

        assertEquals(
                List.of(ResourceLocation.parse("minecraft:forest"), ResourceLocation.parse("minecraft:plains")),
                key.targets());
        assertThrows(
                UnsupportedOperationException.class,
                () -> key.targets().add(ResourceLocation.parse("minecraft:desert")));
    }
}
