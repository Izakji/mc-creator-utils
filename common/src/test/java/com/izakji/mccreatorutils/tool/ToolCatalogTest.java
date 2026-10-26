package com.izakji.mccreatorutils.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ToolCatalogTest {

    @Test
    void catalogContainsCameraExactlyOnce() {
        assertEquals(1, ToolCatalog.tools().stream()
                .filter(tool -> tool.id() == ToolId.CAMERA)
                .count());
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
