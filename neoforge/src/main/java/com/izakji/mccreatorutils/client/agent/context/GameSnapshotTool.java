package com.izakji.mccreatorutils.client.agent.context;

import com.google.gson.JsonObject;
import com.izakji.agent.api.AgentTool;
import com.izakji.agent.api.ToolContext;
import com.izakji.agent.api.ToolInvocation;
import com.izakji.agent.api.ToolResult;
import com.izakji.agent.api.ToolRisk;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Read-only tool that refreshes the live client game snapshot.
 *
 * @author Izakji
 */
public final class GameSnapshotTool implements AgentTool {
    private final GameSnapshotProvider provider;

    public GameSnapshotTool(GameSnapshotProvider provider) {
        this.provider = provider;
    }

    @Override
    public String name() {
        return "game_snapshot";
    }

    @Override
    public String description() {
        return "Refresh live player, world, biome, target, screen, and performance data";
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
        return provider.capture()
                .thenApply(text -> new ToolResult(invocation.callId(), text, false, List.of()))
                .exceptionally(failure ->
                        new ToolResult(invocation.callId(), "Could not read the game snapshot.", true, List.of()));
    }
}
