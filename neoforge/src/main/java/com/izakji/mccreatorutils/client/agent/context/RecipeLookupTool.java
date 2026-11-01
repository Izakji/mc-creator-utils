package com.izakji.mccreatorutils.client.agent.context;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.izakji.agent.api.AgentTool;
import com.izakji.agent.api.ToolContext;
import com.izakji.agent.api.ToolInvocation;
import com.izakji.agent.api.ToolResult;
import com.izakji.agent.api.ToolRisk;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Read-only, lazily cached recipe lookup by id, output, or input.
 *
 * @author Izakji
 */
public final class RecipeLookupTool implements AgentTool {

    @Override
    public String name() {
        return "recipe_lookup";
    }

    @Override
    public String description() {
        return "Look up recipes by recipe id, output item, or input item";
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
        limit.addProperty("maximum", 100);
        properties.add("limit", limit);
        schema.add("properties", properties);
        JsonArray required = new JsonArray();
        required.add("query");
        schema.add("required", required);
        return schema;
    }

    @Override
    public ToolRisk risk() {
        return ToolRisk.READ_ONLY;
    }

    @Override
    public CompletableFuture<ToolResult> execute(ToolInvocation invocation, ToolContext context) {
        String query = ToolArguments.string(invocation.arguments(), "query", "");
        int limit = ToolArguments.integer(invocation.arguments(), "limit", 25);
        return ClientThread.supply(() -> RecipeIndex.lookup(query, limit))
                .thenApply(text -> new ToolResult(invocation.callId(), text, false, List.of()))
                .exceptionally(failure -> new ToolResult(
                        invocation.callId(),
                        failure.getMessage() == null ? "Could not look up recipes." : failure.getMessage(),
                        true,
                        List.of()));
    }
}
