package com.izakji.mccreatorutils.player;

import java.util.Objects;

/**
 * Minecraft experience command units.
 *
 * @author Izakji
 */
public enum XpType {
    LEVELS("levels", "tool.mccreatorutils.player.xp.type.levels"),
    POINTS("points", "tool.mccreatorutils.player.xp.type.points");

    private final String commandToken;
    private final String translationKey;

    XpType(String commandToken, String translationKey) {
        this.commandToken = Objects.requireNonNull(commandToken, "commandToken");
        this.translationKey = Objects.requireNonNull(translationKey, "translationKey");
    }

    public String commandToken() {
        return commandToken;
    }

    public String translationKey() {
        return translationKey;
    }
}
