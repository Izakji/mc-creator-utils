package com.izakji.mccreatorutils.tool;

import java.util.Objects;

/**
 * Stable identifiers for the tools exposed by Creator Utils.
 *
 * @author Izakji
 */
public enum ToolId {
    CAMERA("camera"),
    WORLD_TIME("world_time"),
    BIOMES("biomes"),
    STRUCTURES("structures"),
    DIMENSIONS("dimensions"),
    ITEMS("items"),
    MOBS("mobs"),
    PLAYER("player"),
    EXPLORE_MODS("explore_mods");

    private final String id;

    ToolId(String id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    public String id() {
        return id;
    }
}
