package com.izakji.mccreatorutils.camera;

import java.util.Objects;

/**
 * Time units accepted by CMDCam duration arguments.
 *
 * @author Izakji
 */
public enum DurationUnit {
    MILLISECONDS("ms"),
    SECONDS("s"),
    MINUTES("m"),
    HOURS("h"),
    DAYS("d");

    private final String suffix;

    DurationUnit(String suffix) {
        this.suffix = Objects.requireNonNull(suffix, "suffix");
    }

    public String suffix() {
        return suffix;
    }

    public String translationKey() {
        return "tool.mccreatorutils.camera.unit." + suffix;
    }
}
