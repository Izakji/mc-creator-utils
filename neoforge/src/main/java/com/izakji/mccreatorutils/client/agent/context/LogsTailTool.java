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
 * Read-only client log tail tool.
 *
 * @author Izakji
 */
public final class LogsTailTool implements AgentTool {
    private final ClientLogReader reader;

    public LogsTailTool(ClientLogReader reader) {
        this.reader = reader;
    }

    @Override
    public String name() {
        return "logs_tail";
    }

    @Override
    public String description() {
        return "Read the newest lines from the client latest.log";
    }

    @Override
    public JsonObject inputSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject properties = new JsonObject();
        JsonObject lines = new JsonObject();
        lines.addProperty("type", "integer");
        lines.addProperty("minimum", 1);
        lines.addProperty("maximum", 500);
        properties.add("lines", lines);
        schema.add("properties", properties);
        return schema;
    }

    @Override
    public ToolRisk risk() {
        return ToolRisk.READ_ONLY;
    }

    @Override
    public CompletableFuture<ToolResult> execute(ToolInvocation invocation, ToolContext context) {
        int lines = ToolArguments.integer(invocation.arguments(), "lines", 100);
        return reader.tail(lines).thenApply(text -> new ToolResult(invocation.callId(), text, false, List.of()));
    }
}
