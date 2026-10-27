package com.izakji.mccreatorutils.client;

import com.izakji.mccreatorutils.McCreatorUtils;
import com.izakji.mccreatorutils.locate.LocateCommands;
import com.izakji.mccreatorutils.locate.LocateFeedback;
import java.util.function.Consumer;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Turns vanilla locate feedback into a teleport command.
 *
 * @author Izakji
 */
@EventBusSubscriber(modid = McCreatorUtils.MOD_ID, value = Dist.CLIENT)
public final class ClientLocateTeleporter {
    private static final long TIMEOUT_MILLIS = 15_000L;

    private record Pending(
            Component targetName,
            String successKey,
            String failureKey,
            long expiresAt,
            Consumer<ClientCommandRunner.CommandResult> callback) {}

    private static Pending pending;

    private ClientLocateTeleporter() {}

    public static boolean isPending() {
        return pending != null;
    }

    public static void requestBiome(
            ResourceLocation biome, Component targetName, Consumer<ClientCommandRunner.CommandResult> callback) {
        request(
                LocateCommands.biome(biome),
                targetName,
                "commands.locate.biome.success",
                "commands.locate.biome.not_found",
                callback);
    }

    public static void requestStructure(
            ResourceLocation structure, Component targetName, Consumer<ClientCommandRunner.CommandResult> callback) {
        request(
                LocateCommands.structure(structure),
                targetName,
                "commands.locate.structure.success",
                "commands.locate.structure.not_found",
                callback);
    }

    private static void request(
            String locateCommand,
            Component targetName,
            String successKey,
            String failureKey,
            Consumer<ClientCommandRunner.CommandResult> callback) {
        if (pending != null) {
            callback.accept(
                    new ClientCommandRunner.CommandResult(false, Component.translatable("status.mccreatorutils.busy")));
            return;
        }

        Pending request = new Pending(targetName, successKey, failureKey, Util.getMillis() + TIMEOUT_MILLIS, callback);
        pending = request;
        ClientCommandRunner.CommandResult dispatch = ClientCommandRunner.execute(locateCommand);
        if (!dispatch.success()) {
            if (pending == request) {
                pending = null;
            }
            callback.accept(dispatch);
        }
    }

    @SubscribeEvent
    static void onChat(ClientChatReceivedEvent event) {
        Pending request = pending;
        if (request == null) {
            return;
        }
        if (Util.getMillis() >= request.expiresAt()) {
            fail(request);
            return;
        }

        Component message = event.getMessage();
        if (LocateFeedback.containsTranslation(message, request.successKey())) {
            pending = null;
            String teleport = LocateFeedback.findTeleportCommand(message).orElse(null);
            if (teleport == null) {
                request.callback().accept(failure(request.targetName()));
                return;
            }
            String command = teleport.startsWith("/") ? teleport.substring(1) : teleport;
            ClientCommandRunner.CommandResult result = ClientCommandRunner.execute(command);
            if (result.success()) {
                request.callback()
                        .accept(new ClientCommandRunner.CommandResult(
                                true,
                                Component.translatable("status.mccreatorutils.teleported", request.targetName())));
            } else {
                request.callback().accept(result);
            }
            return;
        }

        if (LocateFeedback.containsTranslation(message, request.failureKey())) {
            fail(request);
        }
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Pending request = pending;
        if (request != null && Util.getMillis() >= request.expiresAt()) {
            fail(request);
        }
    }

    private static void fail(Pending request) {
        if (pending == request) {
            pending = null;
        }
        request.callback().accept(failure(request.targetName()));
    }

    private static ClientCommandRunner.CommandResult failure(Component targetName) {
        return new ClientCommandRunner.CommandResult(
                false, Component.translatable("status.mccreatorutils.locate_failed", targetName));
    }
}
