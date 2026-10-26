package com.izakji.mccreatorutils.camera;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CameraEnumTest {

    @Test
    void modesUseCmdcamIdentifiers() {
        assertEquals("default", CamMode.DEFAULT.id());
        assertEquals("outside", CamMode.OUTSIDE.id());
    }

    @Test
    void unitsUseExactCmdcamSuffixes() {
        assertEquals("ms", DurationUnit.MILLISECONDS.suffix());
        assertEquals("s", DurationUnit.SECONDS.suffix());
        assertEquals("m", DurationUnit.MINUTES.suffix());
        assertEquals("h", DurationUnit.HOURS.suffix());
        assertEquals("d", DurationUnit.DAYS.suffix());
    }
}
