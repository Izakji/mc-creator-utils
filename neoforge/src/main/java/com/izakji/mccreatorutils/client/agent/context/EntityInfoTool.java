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
 * Read-only information for the crosshair entity or the nearest visible entity.
 *
 * @author Izakji
 */
public final class EntityInfoTool implements AgentTool {

    @Override
    public String name() {
        return "entity_info";
    }

    @Override
    public String description() {
        return "Inspect the entity under the crosshair or nearest visible entity";
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
        return ClientThread.supply(EntityInfoReader::build)
                .thenApply(text -> new ToolResult(invocation.callId(), text, false, List.of()))
                .exceptionally(failure -> new ToolResult(
                        invocation.callId(),
                        failure.getMessage() == null ? "Could not inspect the entity." : failure.getMessage(),
                        true,
                        List.of()));
    }
}
