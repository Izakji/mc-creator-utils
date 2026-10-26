package com.izakji.mccreatorutils.camera;

import java.util.Objects;

/**
 * Camera modes understood by the CMDCam {@code /cam mode} command.
 *
 * @author Izakji
 */
public enum CamMode {
    DEFAULT("default"),
    OUTSIDE("outside");

    private final String id;

    CamMode(String id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return "tool.mccreatorutils.camera.mode." + id;
    }
}
