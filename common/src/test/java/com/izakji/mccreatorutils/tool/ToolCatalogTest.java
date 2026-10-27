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
                        ToolId.ITEMS,
                        ToolId.MOBS),
                ToolCatalog.tools().stream().map(ToolDescriptor::id).toList());
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
                ToolCatalog.BIOMES, ToolCatalog.STRUCTURES, ToolCatalog.ITEMS, ToolCatalog.MOBS)) {
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
