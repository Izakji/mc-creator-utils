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
 * Read-only command list exposed by the current server.
 *
 * @author Izakji
 */
public final class ListCommandsTool implements AgentTool {

    @Override
    public String name() {
        return "list_commands";
    }

    @Override
    public String description() {
        return "List available server and modded commands by name or path; structure ids like stronghold are not command names";
    }

    @Override
    public JsonObject inputSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject properties = new JsonObject();
        JsonObject query = new JsonObject();
        query.addProperty("type", "string");
        properties.add("query", query);
        JsonObject limit = new JsonObject();
        limit.addProperty("type", "integer");
        limit.addProperty("minimum", 1);
        limit.addProperty("maximum", 500);
        properties.add("limit", limit);
        schema.add("properties", properties);
        return schema;
    }

    @Override
    public ToolRisk risk() {
        return ToolRisk.READ_ONLY;
    }

    @Override
    public CompletableFuture<ToolResult> execute(ToolInvocation invocation, ToolContext context) {
        String query = invocation.arguments() != null && invocation.arguments().has("query")
                ? invocation.arguments().get("query").getAsString()
                : "";
        int limit = invocation.arguments() != null && invocation.arguments().has("limit")
                ? invocation.arguments().get("limit").getAsInt()
                : 150;
        return ClientThread.supply(() -> CommandCatalog.describe(query, limit))
                .thenApply(text -> new ToolResult(invocation.callId(), text, false, List.of()))
                .exceptionally(failure -> new ToolResult(
                        invocation.callId(),
                        failure.getMessage() == null ? "Could not list commands." : failure.getMessage(),
                        true,
                        List.of()));
    }
}
