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
 * Read-only grouped summary of recent client log warnings and errors.
 *
 * @author Izakji
 */
public final class LogSummaryTool implements AgentTool {
    private final ClientLogReader reader;

    public LogSummaryTool(ClientLogReader reader) {
        this.reader = reader;
    }

    @Override
    public String name() {
        return "log_summary";
    }

    @Override
    public String description() {
        return "Summarize recent ERROR and WARN log lines, group repeats, and show the newest stack trace";
    }

    @Override
    public JsonObject inputSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject properties = new JsonObject();
        JsonObject groups = new JsonObject();
        groups.addProperty("type", "integer");
        groups.addProperty("minimum", 1);
        groups.addProperty("maximum", 50);
        properties.add("groups", groups);
        schema.add("properties", properties);
        return schema;
    }

    @Override
    public ToolRisk risk() {
        return ToolRisk.READ_ONLY;
    }

    @Override
    public CompletableFuture<ToolResult> execute(ToolInvocation invocation, ToolContext context) {
        int groups = invocation.arguments() != null && invocation.arguments().has("groups")
                ? invocation.arguments().get("groups").getAsInt()
                : 15;
        return reader.summary(groups).thenApply(text -> new ToolResult(invocation.callId(), text, false, List.of()));
    }
}
