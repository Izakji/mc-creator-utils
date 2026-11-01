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
 * Read-only client log search tool.
 *
 * @author Izakji
 */
public final class SearchLogsTool implements AgentTool {
    private final ClientLogReader reader;

    public SearchLogsTool(ClientLogReader reader) {
        this.reader = reader;
    }

    @Override
    public String name() {
        return "search_logs";
    }

    @Override
    public String description() {
        return "Search the recent client latest.log by text, level, and context lines";
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
        limit.addProperty("maximum", 200);
        properties.add("limit", limit);
        JsonObject level = new JsonObject();
        level.addProperty("type", "string");
        com.google.gson.JsonArray levels = new com.google.gson.JsonArray();
        for (String value : List.of("ERROR", "WARN", "INFO", "DEBUG", "TRACE", "FATAL")) {
            levels.add(value);
        }
        level.add("enum", levels);
        properties.add("level", level);
        JsonObject context = new JsonObject();
        context.addProperty("type", "integer");
        context.addProperty("minimum", 0);
        context.addProperty("maximum", 5);
        properties.add("context", context);
        schema.add("properties", properties);
        com.google.gson.JsonArray required = new com.google.gson.JsonArray();
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
        int limit = ToolArguments.integer(invocation.arguments(), "limit", 100);
        String level = ToolArguments.string(invocation.arguments(), "level", "");
        int surrounding = ToolArguments.integer(invocation.arguments(), "context", 0);
        return reader.search(query, limit, level, surrounding)
                .thenApply(text -> new ToolResult(invocation.callId(), text, false, List.of()));
    }
}
