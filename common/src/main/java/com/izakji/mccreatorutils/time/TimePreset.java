package com.izakji.mccreatorutils.time;

import java.util.Objects;

/**
 * Named times accepted by the vanilla {@code /time set} command.
 *
 * @author Izakji
 */
public enum TimePreset {
    DAY("day"),
    NIGHT("night"),
    NOON("noon"),
    MIDNIGHT("midnight");

    private final String id;

    TimePreset(String id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return "tool.mccreatorutils.world_time.preset." + id;
    }
}
