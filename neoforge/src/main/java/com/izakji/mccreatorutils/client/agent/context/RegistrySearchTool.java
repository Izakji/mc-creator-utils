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
 * Read-only, lazily cached registry search.
 *
 * @author Izakji
 */
public final class RegistrySearchTool implements AgentTool {

    @Override
    public String name() {
        return "registry_search";
    }

    @Override
    public String description() {
        return "Search item, block, entity, biome, structure, dimension, effect, or enchantment ids";
    }

    @Override
    public JsonObject inputSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject properties = new JsonObject();
        JsonObject kind = new JsonObject();
        kind.addProperty("type", "string");
        JsonArray kinds = new JsonArray();
        for (RegistryKind value : RegistryKind.values()) {
            kinds.add(value.id);
        }
        kind.add("enum", kinds);
        properties.add("kind", kind);
        JsonObject query = new JsonObject();
        query.addProperty("type", "string");
        properties.add("query", query);
        JsonObject namespace = new JsonObject();
        namespace.addProperty("type", "string");
        properties.add("namespace", namespace);
        JsonObject limit = new JsonObject();
        limit.addProperty("type", "integer");
        limit.addProperty("minimum", 1);
        limit.addProperty("maximum", 100);
        properties.add("limit", limit);
        schema.add("properties", properties);
        JsonArray required = new JsonArray();
        required.add("kind");
        schema.add("required", required);
        return schema;
    }

    @Override
    public ToolRisk risk() {
        return ToolRisk.READ_ONLY;
    }

    @Override
    public CompletableFuture<ToolResult> execute(ToolInvocation invocation, ToolContext context) {
        String kindId = ToolArguments.string(invocation.arguments(), "kind", "");
        String query = ToolArguments.string(invocation.arguments(), "query", "");
        String namespace = ToolArguments.string(invocation.arguments(), "namespace", "");
        int limit = ToolArguments.integer(invocation.arguments(), "limit", 25);
        return ClientThread.supply(() -> search(kindId, query, namespace, limit))
                .thenApply(text -> new ToolResult(invocation.callId(), text, false, List.of()))
                .exceptionally(failure -> new ToolResult(invocation.callId(), message(failure), true, List.of()));
    }

    private static String search(String kindId, String query, String namespace, int limit) {
        RegistryKind kind = RegistryKind.parse(kindId);
        if (kind == null) {
            throw new IllegalArgumentException("Unknown registry kind: " + kindId);
        }
        return RegistrySearchReader.search(kind, query, namespace, limit);
    }

    private static String message(Throwable failure) {
        Throwable cause = failure.getCause() == null ? failure : failure.getCause();
        return cause.getMessage() == null ? "Registry search failed." : cause.getMessage();
    }
}
