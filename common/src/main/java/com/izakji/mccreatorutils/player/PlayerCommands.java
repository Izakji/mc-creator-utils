package com.izakji.mccreatorutils.player;

import java.util.Objects;

/**
 * Builds player command strings without a leading slash.
 *
 * @author Izakji
 */
public final class PlayerCommands {
    private PlayerCommands() {}

    public static String giveInvincible() {
        return "effect give @p resistance 99999 255 true";
    }

    public static String clearInvincible() {
        return "effect clear @p minecraft:resistance";
    }

    public static String clearEffects() {
        return "effect clear @p";
    }

    public static String giveWaterBreathing() {
        return "effect give @p minecraft:water_breathing 9999 0 true";
    }

    public static String clearWaterBreathing() {
        return "effect clear @p minecraft:water_breathing";
    }

    public static String addXp(int amount, XpType type) {
        Objects.requireNonNull(type, "type");
        if (amount < XpAmountInput.MIN_VALUE || amount > XpAmountInput.MAX_VALUE) {
            throw new IllegalArgumentException("XP amount must be between 1 and 999");
        }
        return "xp set @s " + amount + " " + type.commandToken();
    }
}
