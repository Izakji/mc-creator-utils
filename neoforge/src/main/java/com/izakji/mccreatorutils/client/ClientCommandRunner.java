package com.izakji.mccreatorutils.client;

import com.izakji.mccreatorutils.McCreatorUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Dispatches client-side commands through the local player connection.
 *
 * @author Izakji
 */
@OnlyIn(Dist.CLIENT)
public final class ClientCommandRunner {
    public record CommandResult(boolean success, Component message) {}

    private ClientCommandRunner() {}

    public static CommandResult execute(String command) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return new CommandResult(false, Component.translatable("status.mccreatorutils.no_player"));
        }
        try {
            player.connection.sendCommand(command);
            return new CommandResult(true, Component.translatable("status.mccreatorutils.ran", command));
        } catch (RuntimeException exception) {
            McCreatorUtils.LOGGER.error("Failed to execute client command: {}", command, exception);
            return new CommandResult(false, Component.translatable("status.mccreatorutils.failed", command));
        }
    }
}
