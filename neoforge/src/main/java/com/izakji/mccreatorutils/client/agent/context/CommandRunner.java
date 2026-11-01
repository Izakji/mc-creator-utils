package com.izakji.mccreatorutils.client.agent.context;

import com.izakji.mccreatorutils.McCreatorUtils;
import com.izakji.mccreatorutils.locate.LocateFeedback;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.ClientCommandHandler;
import net.neoforged.neoforge.client.ClientCommandSourceStack;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Sends one command and captures the server or client chat response.
 *
 * @author Izakji
 */
@EventBusSubscriber(modid = McCreatorUtils.MOD_ID, value = Dist.CLIENT)
public final class CommandRunner {
    private static final Object LOCK = new Object();
    private static final long MIN_CAPTURE_MILLIS = 600;
    private static final long IDLE_MILLIS = 350;
    private static final long MAX_CAPTURE_MILLIS = 5000;
    private static final long MAX_LOCATE_MILLIS = 55_000;
    private static final int MAX_LINES = 100;
    private static final int MAX_LINE_LENGTH = 1000;
    private static final int MAX_TOTAL_CHARS = 24 * 1024;
    private static final List<String> LOCATE_RESULT_KEYS = List.of(
            "commands.locate.biome.success",
            "commands.locate.structure.success",
            "commands.locate.biome.not_found",
            "commands.locate.structure.not_found",
            "commands.mccreatorutils.locate.queue_full",
            "commands.mccreatorutils.locate.timeout",
            "commands.mccreatorutils.locate.failed");

    private static CompletableFuture<CommandResult> pending;
    private static List<String> lines;
    private static int droppedLines;
    private static int capturedChars;
    private static String command;
    private static long startedAt;
    private static long lastMessageAt;
    private static long maxCaptureMillis = MAX_CAPTURE_MILLIS;
    private static volatile boolean locateMode;
    private static boolean locateResultSeen;
    private static BooleanSupplier cancelled = () -> false;

    private CommandRunner() {}

    public static CompletableFuture<CommandResult> run(String raw) {
        return run(raw, () -> false);
    }

    public static CompletableFuture<CommandResult> run(String raw, BooleanSupplier cancellation) {
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
        for (int index = 0; index < normalized.length(); index++) {
            if (!StringUtil.isAllowedChatCharacter(normalized.charAt(index))) {
                return CompletableFuture.failedFuture(
                        new IllegalArgumentException("Command contains characters the server rejects."));
            }
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
            droppedLines = 0;
            capturedChars = 0;
            command = normalized;
            startedAt = System.currentTimeMillis();
            lastMessageAt = startedAt;
            maxCaptureMillis = captureLimit(normalized);
            locateMode = isLocate(normalized);
            locateResultSeen = false;
            cancelled = cancellation;
        }
        try {
            if (!runClientCommand(minecraft, normalized)) {
                minecraft.getConnection().sendCommand(normalized);
            }
        } catch (RuntimeException failure) {
            complete(failure);
        }
        return future;
    }

    public static boolean isLocateRunning() {
        synchronized (LOCK) {
            return pending != null && locateMode;
        }
    }

    public static void cancel() {
        CompletableFuture<CommandResult> abandoned;
        synchronized (LOCK) {
            abandoned = pending;
            clearPending();
        }
        if (abandoned != null) {
            abandoned.completeExceptionally(new CancellationException("Command cancelled."));
        }
    }

    @SubscribeEvent
    static void onSystemChat(ClientChatReceivedEvent.System event) {
        if (!event.isOverlay()) {
            capture(event.getMessage());
        }
    }

    @SubscribeEvent
    static void onPlayerChat(ClientChatReceivedEvent.Player event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (!locateMode && player != null && player.getUUID().equals(event.getSender())) {
            capture(event.getMessage());
        }
    }

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        CompletableFuture<CommandResult> finished = null;
        CommandResult result = null;
        boolean failed = false;
        synchronized (LOCK) {
            if (pending == null) {
                return;
            }
            long now = System.currentTimeMillis();
            Minecraft minecraft = Minecraft.getInstance();
            boolean disconnected = minecraft.getConnection() == null;
            boolean cancelledNow = cancelled.getAsBoolean();
            boolean paused = minecraft.isPaused();
            boolean timedOut = !paused && now - startedAt >= maxCaptureMillis;
            boolean settled = !lines.isEmpty()
                    && (!locateMode || locateResultSeen)
                    && now - startedAt >= MIN_CAPTURE_MILLIS
                    && now - lastMessageAt >= IDLE_MILLIS;
            if (disconnected || cancelledNow || timedOut || settled) {
                finished = pending;
                result = new CommandResult(command, List.copyOf(lines), droppedLines);
                failed = disconnected || cancelledNow;
                clearPending();
            }
        }
        if (finished != null) {
            if (failed) {
                finished.completeExceptionally(new CancellationException(
                        Minecraft.getInstance().getConnection() == null
                                ? "Disconnected while running the command."
                                : "Command cancelled."));
            } else {
                finished.complete(result);
            }
        }
    }

    private static boolean runClientCommand(Minecraft minecraft, String commandText) {
        CommandDispatcher<CommandSourceStack> dispatcher = ClientCommandHandler.getDispatcher();
        LocalPlayer player = minecraft.player;
        if (dispatcher == null || player == null) {
            return false;
        }
        String[] tokens = CommandSafety.tokens(commandText);
        if (tokens.length == 0 || dispatcher.getRoot().getChild(tokens[0]) == null) {
            return false;
        }
        CapturingSource source = new CapturingSource(
                player,
                player.position(),
                player.getRotationVector(),
                player.getPermissionLevel(),
                player.getName().getString(),
                player.getDisplayName(),
                player,
                CommandRunner::capture);
        try {
            dispatcher.execute(commandText, source);
        } catch (CommandSyntaxException syntax) {
            if (syntax.getType() == CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand()
                    || syntax.getType() == CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument()) {
                return false;
            }
            capture(Component.literal("Command error: " + syntax.getMessage()));
        } catch (RuntimeException failure) {
            capture(Component.literal("Command error: " + failure.getMessage()));
        }
        return true;
    }

    private static void capture(Component message) {
        synchronized (LOCK) {
            if (pending == null || lines == null) {
                return;
            }
            String line = message.getString();
            if (line.length() > MAX_LINE_LENGTH) {
                line = line.substring(0, MAX_LINE_LENGTH) + "...";
            }
            if (lines.size() >= MAX_LINES || capturedChars + line.length() > MAX_TOTAL_CHARS) {
                droppedLines++;
            } else {
                lines.add(line);
                capturedChars += line.length();
            }
            if (locateMode
                    && LOCATE_RESULT_KEYS.stream().anyMatch(key -> LocateFeedback.containsTranslation(message, key))) {
                locateResultSeen = true;
            }
            lastMessageAt = System.currentTimeMillis();
        }
    }

    private static void clearPending() {
        pending = null;
        lines = null;
        command = "";
        droppedLines = 0;
        capturedChars = 0;
        cancelled = () -> false;
        maxCaptureMillis = MAX_CAPTURE_MILLIS;
        locateMode = false;
        locateResultSeen = false;
    }

    private static long captureLimit(String commandText) {
        return isLocate(commandText) ? MAX_LOCATE_MILLIS : MAX_CAPTURE_MILLIS;
    }

    private static boolean isLocate(String commandText) {
        String[] tokens = CommandSafety.tokens(commandText);
        return tokens.length > 0 && tokens[0].equals("locate");
    }

    private static void complete(Throwable failure) {
        CompletableFuture<CommandResult> failed;
        synchronized (LOCK) {
            failed = pending;
            clearPending();
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

    private static final class CapturingSource extends ClientCommandSourceStack {
        private final Consumer<Component> capture;

        private CapturingSource(
                LocalPlayer player,
                net.minecraft.world.phys.Vec3 position,
                net.minecraft.world.phys.Vec2 rotation,
                int permission,
                String name,
                Component displayName,
                net.minecraft.world.entity.Entity executing,
                Consumer<Component> capture) {
            super(player, position, rotation, permission, name, displayName, executing);
            this.capture = capture;
        }

        @Override
        public void sendSuccess(java.util.function.Supplier<Component> message, boolean sendToAdmins) {
            capture.accept(message.get());
        }

        @Override
        public void sendFailure(Component message) {
            capture.accept(message);
        }
    }

    /**
     * One command and its captured chat output.
     *
     * @param command normalized command
     * @param lines captured chat lines
     * @param droppedLines lines omitted by the capture cap
     */
    public record CommandResult(String command, List<String> lines, int droppedLines) {
        public CommandResult {
            lines = List.copyOf(lines);
        }
    }
}
