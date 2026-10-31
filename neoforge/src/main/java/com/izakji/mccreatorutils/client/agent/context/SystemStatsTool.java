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
 * Read-only CPU, heap, and garbage-collection tool.
 *
 * @author Izakji
 */
public final class SystemStatsTool implements AgentTool {

    @Override
    public String name() {
        return "system_stats";
    }

    @Override
    public String description() {
        return "Read process and system CPU load, heap usage, and garbage-collection pauses";
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
        return CompletableFuture.completedFuture(new ToolResult(
                invocation.callId(),
                SystemStatsReport.format(SystemDiagnostics.instance().snapshot()),
                false,
                List.of()));
    }
}
