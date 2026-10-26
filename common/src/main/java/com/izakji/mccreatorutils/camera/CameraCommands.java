package com.izakji.mccreatorutils.camera;

import java.util.Objects;

/**
 * Builds CMDCam client command strings without a leading slash.
 *
 * @author Izakji
 */
public final class CameraCommands {
    private CameraCommands() {}

    public static String mode(CamMode mode) {
        Objects.requireNonNull(mode, "mode");
        return "cam mode " + mode.id();
    }

    public static String duration(int value, DurationUnit unit) {
        Objects.requireNonNull(unit, "unit");
        if (value < DurationInput.MIN_VALUE || value > DurationInput.MAX_VALUE) {
            throw new IllegalArgumentException("Duration must be between 1 and 999");
        }
        return "cam duration " + value + unit.suffix();
    }
}
