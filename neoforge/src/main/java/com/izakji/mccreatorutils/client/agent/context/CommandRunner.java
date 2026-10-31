package com.izakji.mccreatorutils.client.agent.context;

import com.izakji.mccreatorutils.McCreatorUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Sends one command and captures the server's chat response.
 *
 * @author Izakji
 */
@EventBusSubscriber(modid = McCreatorUtils.MOD_ID, value = Dist.CLIENT)
public final class CommandRunner {
    private static final Object LOCK = new Object();
    private static final long MIN_CAPTURE_MILLIS = 600;
    private static final long IDLE_MILLIS = 350;
    private static final long MAX_CAPTURE_MILLIS = 2500;
    private static final int MAX_LINES = 100;
    private static final int MAX_LINE_LENGTH = 1000;

    private static CompletableFuture<CommandResult> pending;
    private static List<String> lines;
    private static String command;
    private static long startedAt;
    private static long lastMessageAt;

    private CommandRunner() {}

    public static CompletableFuture<CommandResult> run(String raw) {
        String normalized = normalize(raw);
        if (normalized.isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Command is empty."));
        }
        if (normalized.indexOf('\n') >= 0 || normalized.indexOf('\r') >= 0) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Command must be a single line."));
        }
        if (normalized.length() > 1000) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Command is too long."));
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("No server connection is available."));
        }
        CompletableFuture<CommandResult> future;
        synchronized (LOCK) {
            if (pending != null) {
                return CompletableFuture.failedFuture(new IllegalStateException("Another command is still running."));
            }
            future = new CompletableFuture<>();
            pending = future;
            lines = new ArrayList<>();
            command = normalized;
            startedAt = System.currentTimeMillis();
            lastMessageAt = startedAt;
        }
        try {
            minecraft.getConnection().sendCommand(normalized);
        } catch (RuntimeException failure) {
            complete(failure);
        }
        return future;
    }

    @SubscribeEvent
    static void onChat(ClientChatReceivedEvent.System event) {
        if (event.isOverlay()) {
            return;
        }
        synchronized (LOCK) {
            if (pending == null || lines == null) {
                return;
            }
            String line = event.getMessage().getString();
            if (line.length() > MAX_LINE_LENGTH) {
                line = line.substring(0, MAX_LINE_LENGTH) + "...";
            }
            if (lines.size() < MAX_LINES) {
                lines.add(line);
            }
            lastMessageAt = System.currentTimeMillis();
        }
    }

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        CompletableFuture<CommandResult> finished = null;
        CommandResult result = null;
        synchronized (LOCK) {
            if (pending == null) {
                return;
            }
            long now = System.currentTimeMillis();
            boolean disconnected = Minecraft.getInstance().getConnection() == null;
            boolean timedOut = now - startedAt >= MAX_CAPTURE_MILLIS;
            boolean settled = now - startedAt >= MIN_CAPTURE_MILLIS && now - lastMessageAt >= IDLE_MILLIS;
            if (disconnected || timedOut || settled) {
                finished = pending;
                result = new CommandResult(command, List.copyOf(lines));
                pending = null;
                lines = null;
                command = "";
            }
        }
        if (finished != null) {
            if (result.lines().isEmpty() && Minecraft.getInstance().getConnection() == null) {
                finished.completeExceptionally(new IllegalStateException("Disconnected while running the command."));
            } else {
                finished.complete(result);
            }
        }
    }

    private static void complete(Throwable failure) {
        CompletableFuture<CommandResult> failed;
        synchronized (LOCK) {
            failed = pending;
            pending = null;
            lines = null;
            command = "";
        }
        if (failed != null) {
            failed.completeExceptionally(failure);
        }
    }

    private static String normalize(String raw) {
        String value = raw == null ? "" : raw.strip();
        while (value.startsWith("/")) {
            value = value.substring(1).strip();
        }
        return value;
    }

    /**
     * One command and its captured chat output.
     *
     * @param command normalized command
     * @param lines captured system chat lines
     */
    public record CommandResult(String command, List<String> lines) {
        public CommandResult {
            lines = List.copyOf(lines);
        }
    }
}
