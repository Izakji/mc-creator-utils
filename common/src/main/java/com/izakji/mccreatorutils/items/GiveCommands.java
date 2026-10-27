package com.izakji.mccreatorutils.items;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/**
 * Builds item command strings without a leading slash.
 *
 * @author Izakji
 */
public final class GiveCommands {
    private GiveCommands() {}

    public static String give(ResourceLocation item, int count) {
        Objects.requireNonNull(item, "item");
        if (count < ItemCountInput.MIN_VALUE || count > ItemCountInput.MAX_VALUE) {
            throw new IllegalArgumentException("Item count must be between 1 and 512");
        }
        return "give @s " + item + " " + count;
    }

    public static String clearInventory() {
        return "clear";
    }

    public static String clearWorldItems() {
        return "kill @e[type=item]";
    }
}
