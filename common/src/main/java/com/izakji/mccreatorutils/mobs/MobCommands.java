package com.izakji.mccreatorutils.mobs;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/**
 * Builds mob command strings without a leading slash.
 *
 * @author Izakji
 */
public final class MobCommands {
    private MobCommands() {}

    public static String summon(ResourceLocation entityType) {
        Objects.requireNonNull(entityType, "entityType");
        return "summon " + entityType;
    }

    public static String killAllExceptPlayers() {
        return "kill @e[type=!minecraft:player]";
    }
}
