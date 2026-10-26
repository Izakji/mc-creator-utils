package com.izakji.mccreatorutils.camera;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CameraCommandsTest {

    @Test
    void buildsModeCommands() {
        assertEquals("cam mode default", CameraCommands.mode(CamMode.DEFAULT));
        assertEquals("cam mode outside", CameraCommands.mode(CamMode.OUTSIDE));
    }

    @Test
    void buildsDurationCommandsWithoutSpaceBeforeUnit() {
        assertEquals("cam duration 20s", CameraCommands.duration(20, DurationUnit.SECONDS));
        assertEquals("cam duration 500ms", CameraCommands.duration(500, DurationUnit.MILLISECONDS));
        assertEquals("cam duration 2m", CameraCommands.duration(2, DurationUnit.MINUTES));
        assertEquals("cam duration 1h", CameraCommands.duration(1, DurationUnit.HOURS));
        assertEquals("cam duration 3d", CameraCommands.duration(3, DurationUnit.DAYS));
    }

    @Test
    void rejectsNullMode() {
        assertThrows(NullPointerException.class, () -> CameraCommands.mode(null));
    }

    @Test
    void rejectsDurationOutsideRange() {
        assertThrows(IllegalArgumentException.class, () -> CameraCommands.duration(0, DurationUnit.SECONDS));
        assertThrows(IllegalArgumentException.class, () -> CameraCommands.duration(1000, DurationUnit.SECONDS));
    }
}
