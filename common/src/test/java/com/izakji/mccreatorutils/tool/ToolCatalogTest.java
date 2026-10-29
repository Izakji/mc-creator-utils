package com.izakji.mccreatorutils.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ToolCatalogTest {

    @Test
    void catalogContainsCameraExactlyOnce() {
        assertEquals(
                1,
                ToolCatalog.tools().stream()
                        .filter(tool -> tool.id() == ToolId.CAMERA)
                        .count());
    }

    @Test
    void catalogContainsWorldTimeExactlyOnce() {
        assertEquals(
                1,
                ToolCatalog.tools().stream()
                        .filter(tool -> tool.id() == ToolId.WORLD_TIME)
                        .count());
    }

    @Test
    void catalogPreservesDeclarationOrder() {
        assertEquals(
                List.of(
                        ToolId.CAMERA,
                        ToolId.WORLD_TIME,
                        ToolId.BIOMES,
                        ToolId.STRUCTURES,
                        ToolId.DIMENSIONS,
                        ToolId.ITEMS,
                        ToolId.MOBS,
                        ToolId.PLAYER,
                        ToolId.EXPLORE_MODS),
                ToolCatalog.tools().stream().map(ToolDescriptor::id).toList());
    }

    @Test
    void exploreModsDescriptorRequiresRinku() {
        var explore = ToolCatalog.EXPLORE_MODS;
        assertEquals(ToolId.EXPLORE_MODS, explore.id());
        assertEquals("explore_mods", explore.id().id());
        assertEquals("tool.mccreatorutils.explore_mods", explore.translationKey());
        assertEquals("rinku", explore.requiredModId());
        assertTrue(explore.hasRequiredMod());
    }

    @Test
    void playerDescriptorHasNoRequiredMod() {
        var player = ToolCatalog.PLAYER;
        assertEquals(ToolId.PLAYER, player.id());
        assertEquals("player", player.id().id());
        assertEquals("tool.mccreatorutils.player", player.translationKey());
        assertFalse(player.hasRequiredMod());
    }

    @Test
    void dimensionsDescriptorHasNoRequiredMod() {
        var dimensions = ToolCatalog.DIMENSIONS;
        assertEquals(ToolId.DIMENSIONS, dimensions.id());
        assertEquals("dimensions", dimensions.id().id());
        assertEquals("tool.mccreatorutils.dimensions", dimensions.translationKey());
        assertFalse(dimensions.hasRequiredMod());
    }

    @Test
    void worldTimeDescriptorHasNoRequiredMod() {
        var worldTime = ToolCatalog.WORLD_TIME;
        assertEquals(ToolId.WORLD_TIME, worldTime.id());
        assertEquals("world_time", worldTime.id().id());
        assertEquals("tool.mccreatorutils.world_time", worldTime.translationKey());
        assertFalse(worldTime.hasRequiredMod());
    }

    @Test
    void registryToolsRequireNoMod() {
        for (ToolDescriptor tool : List.of(
                ToolCatalog.BIOMES,
                ToolCatalog.STRUCTURES,
                ToolCatalog.DIMENSIONS,
                ToolCatalog.ITEMS,
                ToolCatalog.MOBS)) {
            assertFalse(tool.hasRequiredMod());
        }
    }

    @Test
    void cameraDescriptorHasExpectedMetadata() {
        var camera = ToolCatalog.CAMERA;
        assertEquals(ToolId.CAMERA, camera.id());
        assertEquals("camera", camera.id().id());
        assertEquals("tool.mccreatorutils.camera", camera.translationKey());
        assertEquals("cmdcam", camera.requiredModId());
        assertTrue(camera.hasRequiredMod());
    }
}
