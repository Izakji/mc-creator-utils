package com.izakji.mccreatorutils.tool;

import java.util.Objects;

/**
 * Stable identifiers for the tools exposed by Creator Utils.
 *
 * @author Izakji
 */
public enum ToolId {
    CAMERA("camera");

    private final String id;

    ToolId(String id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    public String id() {
        return id;
    }
}
