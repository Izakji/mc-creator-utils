package com.izakji.mccreatorutils.client.agent.context;

import com.google.gson.JsonObject;
import com.izakji.agent.api.AgentTool;
import com.izakji.agent.api.ImagePart;
import com.izakji.agent.api.ToolContext;
import com.izakji.agent.api.ToolInvocation;
import com.izakji.agent.api.ToolResult;
import com.izakji.agent.api.ToolRisk;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

/**
 * Read-only screenshot of the current world view.
 *
 * @author Izakji
 */
public final class ScreenshotTool implements AgentTool {
    private final Path gameDirectory;
    private final Executor ioExecutor;

    public ScreenshotTool(Path gameDirectory, Executor ioExecutor) {
        this.gameDirectory = gameDirectory;
        this.ioExecutor = ioExecutor;
    }

    @Override
    public String name() {
        return "screenshot";
    }

    @Override
    public String description() {
        return "Capture the current world view and attach it as an image for visual analysis";
    }

    @Override
    public JsonObject inputSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.add("properties", new JsonObject());
        return schema;
    }

    @Override
    public ToolRisk risk() {
        return ToolRisk.READ_ONLY;
    }

    @Override
    public CompletableFuture<ToolResult> execute(ToolInvocation invocation, ToolContext context) {
        return ScreenshotCapture.request()
                .thenApplyAsync(
                        image -> {
                            try {
                                return ScreenshotCapture.save(gameDirectory, image);
                            } catch (IOException failure) {
                                throw new CompletionException(failure);
                            }
                        },
                        ioExecutor)
                .thenApply(captured -> new ToolResult(
                        invocation.callId(),
                        "Screenshot saved to "
                                + captured.file().toAbsolutePath()
                                + " ("
                                + captured.width()
                                + "x"
                                + captured.height()
                                + "). A downscaled JPEG was prepared for visual analysis.",
                        false,
                        List.of(new ImagePart("image/jpeg", captured.jpeg()))))
                .exceptionally(failure -> {
                    Throwable cause = failure.getCause() == null ? failure : failure.getCause();
                    String message;
                    if (cause instanceof java.util.concurrent.TimeoutException) {
                        message = "Screenshot capture timed out. Make sure a world is loaded and visible.";
                    } else {
                        message = cause.getMessage() == null ? "Screenshot capture failed." : cause.getMessage();
                    }
                    return new ToolResult(invocation.callId(), message, true, List.of());
                });
    }
}
