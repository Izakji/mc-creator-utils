package com.izakji.mccreatorutils.client;

import com.izakji.mccreatorutils.McCreatorUtils;
import com.izakji.mccreatorutils.locate.LocateCommands;
import com.izakji.mccreatorutils.locate.LocateFeedback;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Turns vanilla locate feedback into a teleport command.
 *
 * @author Izakji
 */
@EventBusSubscriber(modid = McCreatorUtils.MOD_ID, value = Dist.CLIENT)
public final class ClientLocateTeleporter {
    private static final long TIMEOUT_MILLIS = 120_000L;
    private static final Consumer<ClientCommandRunner.CommandResult> DROP_RESULT = result -> {};

    private record Pending(
            Component targetName,
            String expectedTarget,
            String successKey,
            Set<String> failureKeys,
            long expiresAt,
            Consumer<ClientCommandRunner.CommandResult> callback) {}

    private static Pending pending;

    private ClientLocateTeleporter() {}

    public static boolean isPending() {
        return pending != null;
    }

    /**
     * Stops delivering the current result to its panel while the request finishes on the server.
     *
     * <p>Keeping the request pending avoids interpreting its late success message as the result of a
     * newer request. The next locate request is available when this one completes or times out.
     */
    public static void abandon() {
        Pending current = pending;
        if (current != null) {
            pending = new Pending(
                    current.targetName(),
                    current.expectedTarget(),
                    current.successKey(),
                    current.failureKeys(),
                    current.expiresAt(),
                    DROP_RESULT);
        }
    }

    /**
     * Forgets the current request, for example when the client leaves the server.
     */
    public static void cancelPending() {
        pending = null;
    }

    public static void requestBiome(
            ResourceLocation biome, Component targetName, Consumer<ClientCommandRunner.CommandResult> callback) {
        request(
                LocateCommands.biome(biome),
                targetName,
                biome.toString(),
                "commands.locate.biome.success",
                Set.of(
                        "commands.locate.biome.not_found",
                        "commands.mccreatorutils.locate.queue_full",
                        "commands.mccreatorutils.locate.timeout",
                        "commands.mccreatorutils.locate.failed"),
                callback);
    }

    public static void requestStructure(
            ResourceLocation structure, Component targetName, Consumer<ClientCommandRunner.CommandResult> callback) {
        request(
                LocateCommands.structure(structure),
                targetName,
                structure.toString(),
                "commands.locate.structure.success",
                Set.of(
                        "commands.locate.structure.not_found",
                        "commands.mccreatorutils.locate.queue_full",
                        "commands.mccreatorutils.locate.timeout",
                        "commands.mccreatorutils.locate.failed"),
                callback);
    }

    private static void request(
            String locateCommand,
            Component targetName,
            String expectedTarget,
            String successKey,
            Set<String> failureKeys,
            Consumer<ClientCommandRunner.CommandResult> callback) {
        if (pending != null) {
            callback.accept(
                    new ClientCommandRunner.CommandResult(false, Component.translatable("status.mccreatorutils.busy")));
            return;
        }

        Pending request = new Pending(
                targetName, expectedTarget, successKey, failureKeys, Util.getMillis() + TIMEOUT_MILLIS, callback);
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
        if (LocateFeedback.containsTranslation(message, request.successKey()) && matchesTarget(message, request)) {
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
                                Component.translatable("status.mccreatorutils.teleport_sent", request.targetName())));
            } else {
                request.callback().accept(result);
            }
            return;
        }

        if (request.failureKeys().stream().anyMatch(key -> LocateFeedback.containsTranslation(message, key))) {
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

    @SubscribeEvent
    static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        cancelPending();
    }

    private static boolean matchesTarget(Component message, Pending request) {
        return LocateFeedback.translationArgumentText(message, request.successKey(), 0)
                .map(text -> text.equalsIgnoreCase(request.expectedTarget()))
                .orElse(true);
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
