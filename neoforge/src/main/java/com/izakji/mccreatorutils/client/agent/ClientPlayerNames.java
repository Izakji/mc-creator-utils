package com.izakji.mccreatorutils.client.agent;

import net.minecraft.client.Minecraft;

/**
 * Reads the local player name for transcript labels.
 *
 * @author Izakji
 */
final class ClientPlayerNames {
    private ClientPlayerNames() {}

    static String current() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == null ? "You" : minecraft.player.getName().getString();
    }
}
